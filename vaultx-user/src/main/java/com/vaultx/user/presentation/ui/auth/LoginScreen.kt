package com.vaultx.user.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AuthUiState
import com.vaultx.user.presentation.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────────────────
// LoginScreen — Clean, premium login UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val email          by viewModel.email.collectAsState()
    val password       by viewModel.password.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()
    val passVisible    by viewModel.passwordVisible.collectAsState()
    val emailError     by viewModel.emailError.collectAsState()
    val passwordError  by viewModel.passwordError.collectAsState()

    val focusManager = LocalFocusManager.current
    val isLoading = uiState is AuthUiState.Loading
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isImeVisible) {
                Spacer(Modifier.height(40.dp))
                // ── Brand mark ────────────────────────────────────────────────────
                VaultBrandMark()
                Spacer(Modifier.height(48.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sign in to access your vault",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isImeVisible) {
                Spacer(Modifier.height(40.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }

            // ── Error banner ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState is AuthUiState.Error,
                enter = slideInVertically(tween(300)) + fadeIn(),
                exit  = slideOutVertically(tween(300)) + fadeOut()
            ) {
                val msg = (uiState as? AuthUiState.Error)?.message ?: ""
                ErrorBanner(message = msg)
                Spacer(Modifier.height(16.dp))
            }

            // ── Email ─────────────────────────────────────────────────────────
            VaultTextField(
                value         = email,
                onValueChange = viewModel::onEmailChanged,
                label         = "Email",
                placeholder   = "you@example.com",
                leadingIcon   = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError       = emailError != null && email.isNotEmpty(),
                errorMessage  = emailError,
            )

            Spacer(Modifier.height(16.dp))

            // ── Password ──────────────────────────────────────────────────────
            VaultTextField(
                value         = password,
                onValueChange = viewModel::onPasswordChanged,
                label         = "Password",
                placeholder   = "••••••••",
                leadingIcon   = Icons.Outlined.Lock,
                visualTransformation = if (passVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }
                ),
                isError      = passwordError != null && password.isNotEmpty(),
                errorMessage = passwordError,
                trailingIcon = {
                    VaultIconButton(
                        icon = if (passVisible) Icons.Outlined.VisibilityOff
                               else Icons.Outlined.Visibility,
                        onClick = viewModel::togglePasswordVisibility,
                        contentDescription = if (passVisible) "Hide password" else "Show password"
                    )
                }
            )

            Spacer(Modifier.height(8.dp))

            // ── Forgot Password ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = viewModel::forgotPassword,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign In button ────────────────────────────────────────────────
            VaultButton(
                text      = "Sign In",
                onClick   = { focusManager.clearFocus(); viewModel.login() },
                isLoading = isLoading,
                enabled   = email.isNotEmpty() && password.isNotEmpty()
            )

            Spacer(Modifier.height(24.dp))

            // ── Register link ─────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text  = "Create one",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Brand mark composable ─────────────────────────────────────────────────────
@Composable
fun VaultBrandMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape    = ShapeCard,
            color    = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "VaultX",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text  = "VaultX",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ── Error banner ──────────────────────────────────────────────────────────────
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier  = modifier.fillMaxWidth(),
        shape     = ShapeChip,
        color     = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
