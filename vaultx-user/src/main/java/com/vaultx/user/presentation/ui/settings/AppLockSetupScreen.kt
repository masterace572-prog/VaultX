package com.vaultx.user.presentation.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val pinLength = 4

    fun onNumberPadClick(number: Int) {
        errorMessage = null
        if (!isConfirming) {
            if (pin.length < pinLength) {
                pin += number.toString()
                if (pin.length == pinLength) {
                    isConfirming = true
                }
            }
        } else {
            if (confirmPin.length < pinLength) {
                confirmPin += number.toString()
                if (confirmPin.length == pinLength) {
                    // Check if match
                    if (pin == confirmPin) {
                        isSaving = true
                        viewModel.setupAppPin(pin) { success ->
                            isSaving = false
                            if (success) {
                                onNavigateBack()
                            } else {
                                errorMessage = "Failed to set up PIN. Try again."
                                pin = ""
                                confirmPin = ""
                                isConfirming = false
                            }
                        }
                    } else {
                        errorMessage = "PINs do not match. Try again."
                        pin = ""
                        confirmPin = ""
                        isConfirming = false
                    }
                }
            }
        }
    }

    fun onBackspaceClick() {
        errorMessage = null
        if (!isConfirming) {
            if (pin.isNotEmpty()) {
                pin = pin.dropLast(1)
            }
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                isConfirming = false
                pin = pin.dropLast(1)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("App Lock Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = if (!isConfirming) "Create your PIN" else "Confirm your PIN",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "This PIN will be used to quickly unlock your vault instead of your master password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // PIN Dots Indicator
            val currentEntry = if (!isConfirming) pin else confirmPin
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until pinLength) {
                    val isFilled = i < currentEntry.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = isSaving) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(Modifier.weight(1f))

            // Number Pad
            NumberPad(
                onNumberClick = ::onNumberPadClick,
                onBackspaceClick = ::onBackspaceClick,
                enabled = !isSaving
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    enabled: Boolean
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-1, 0, -2) // -1 is empty, -2 is backspace
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (num in row) {
                    if (num == -1) {
                        Spacer(Modifier.size(72.dp))
                    } else if (num == -2) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable(enabled = enabled, onClick = onBackspaceClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Backspace,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable(enabled = enabled) { onNumberClick(num) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = num.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
