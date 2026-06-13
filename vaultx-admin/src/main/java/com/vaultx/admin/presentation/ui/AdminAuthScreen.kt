package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.presentation.AdminViewModel
import com.vaultx.admin.presentation.ui.components.VaultButton
import com.vaultx.admin.presentation.ui.components.VaultTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Admin Login",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        VaultTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = "Email",
                            leadingIcon = Icons.Outlined.Mail,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        VaultTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = "Password",
                            leadingIcon = Icons.Outlined.Lock,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    VaultButton(
                        text = "Sign In",
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password) { err ->
                                    errorMessage = err
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = email.isNotBlank() && password.isNotBlank(),
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}
