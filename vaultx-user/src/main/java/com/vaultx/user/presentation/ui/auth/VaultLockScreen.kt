package com.vaultx.user.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.VaultButton
import com.vaultx.user.presentation.ui.components.VaultButtonVariant
import com.vaultx.user.presentation.ui.components.VaultIconButton
import com.vaultx.user.presentation.ui.components.VaultTextField
import com.vaultx.user.presentation.viewmodel.VaultLockUiState
import com.vaultx.user.presentation.viewmodel.VaultLockViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VaultLockScreen(
    onUnlockSuccess: () -> Unit,
    onFallbackToLogin: () -> Unit,
    viewModel: VaultLockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showMasterPassword by remember { mutableStateOf(!viewModel.isAppLockEnabled) }
    var masterPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var pin by remember { mutableStateOf("") }
    val pinLength = 4

    var autoPromptFired by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is VaultLockUiState.Success) {
            onUnlockSuccess()
        }
    }

    LaunchedEffect(Unit) {
        if (!autoPromptFired && viewModel.isBiometricEnabled) {
            autoPromptFired = true
            delay(400)
            viewModel.attemptBiometricUnlock(activity)
        }
    }

    val focusManager = LocalFocusManager.current

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.LockPerson,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = showMasterPassword,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "auth_content"
            ) { isMasterPass ->
                if (isMasterPass) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Vault Locked",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Enter your master password to decrypt your vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(32.dp))

                        VaultTextField(
                            value = masterPassword,
                            onValueChange = { masterPassword = it },
                            label = "Master Password",
                            leadingIcon = Icons.Outlined.Lock,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.unlockWithMasterPassword(masterPassword)
                                }
                            ),
                            trailingIcon = {
                                VaultIconButton(
                                    icon = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    onClick = { passwordVisible = !passwordVisible }
                                )
                            }
                        )

                        if (uiState is VaultLockUiState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = (uiState as VaultLockUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        VaultButton(
                            text = "Unlock",
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.unlockWithMasterPassword(masterPassword)
                            },
                            isLoading = uiState is VaultLockUiState.Authenticating,
                            enabled = masterPassword.isNotEmpty()
                        )

                        if (viewModel.isAppLockEnabled) {
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { showMasterPassword = false }) {
                                Text("Use App PIN", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        // Switch account fallback
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = onFallbackToLogin) {
                            Text("Switch Account", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Enter PIN",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.height(32.dp))

                        // PIN Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until pinLength) {
                                val isFilled = i < pin.length
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

                        if (uiState is VaultLockUiState.Error) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = (uiState as VaultLockUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Spacer(Modifier.height(36.dp))
                        }

                        if (uiState is VaultLockUiState.Authenticating) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        } else {
                            // Number Pad
                            PinNumberPad(
                                onNumberClick = { num ->
                                    viewModel.resetState()
                                    if (pin.length < pinLength) {
                                        pin += num.toString()
                                        if (pin.length == pinLength) {
                                            viewModel.unlockWithPin(pin)
                                            // Reset pin after short delay
                                            scope.launch {
                                                delay(500)
                                                if (uiState is VaultLockUiState.Error) {
                                                    pin = ""
                                                }
                                            }
                                        }
                                    }
                                },
                                onBackspaceClick = {
                                    viewModel.resetState()
                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                },
                                onBiometricClick = if (viewModel.isBiometricEnabled) {
                                    { viewModel.attemptBiometricUnlock(activity) }
                                } else null,
                                enabled = true
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        TextButton(onClick = { showMasterPassword = true }) {
                            Text("Use Master Password", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinNumberPad(
    onNumberClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)?,
    enabled: Boolean
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-3, 0, -2) // -3 is biometric or empty, -2 is backspace
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
                    if (num == -3) {
                        if (onBiometricClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = enabled, onClick = onBiometricClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Fingerprint,
                                    contentDescription = "Biometric",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            Spacer(Modifier.size(72.dp))
                        }
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
