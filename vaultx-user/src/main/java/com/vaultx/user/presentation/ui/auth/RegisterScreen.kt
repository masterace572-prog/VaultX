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
// RegisterScreen — Multi-field registration with live validation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val email         by viewModel.email.collectAsState()
    val password      by viewModel.password.collectAsState()
    val displayName   by viewModel.displayName.collectAsState()
    val confirmPass   by viewModel.confirmPass.collectAsState()
    val uiState       by viewModel.uiState.collectAsState()
    val passVisible   by viewModel.passwordVisible.collectAsState()
    val emailError    by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val confirmError  by viewModel.confirmPassError.collectAsState()

    val focusManager = LocalFocusManager.current
    val isLoading    = uiState is AuthUiState.Loading
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Back arrow
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
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
                Spacer(Modifier.height(16.dp))
                VaultBrandMark()
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text  = "Create your vault",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Your data is encrypted locally — we never see it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isImeVisible) {
                Spacer(Modifier.height(32.dp))
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // ── Zero-Knowledge notice banner ──────────────────────────────────
            ZeroKnowledgeBanner()

            Spacer(Modifier.height(24.dp))

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

            // ── Display name ──────────────────────────────────────────────────
            VaultTextField(
                value         = displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label         = "Your Name",
                placeholder   = "Alex Morgan",
                leadingIcon   = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError      = displayName.isNotEmpty() && displayName.isBlank(),
                errorMessage = if (displayName.isNotEmpty() && displayName.isBlank()) "Name cannot be blank" else null
            )

            Spacer(Modifier.height(16.dp))

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
                isError      = emailError != null && email.isNotEmpty(),
                errorMessage = emailError
            )

            Spacer(Modifier.height(16.dp))

            // ── Password ──────────────────────────────────────────────────────
            VaultTextField(
                value         = password,
                onValueChange = viewModel::onPasswordChanged,
                label         = "Master Password",
                placeholder   = "Min 8 chars, 1 uppercase, 1 number",
                leadingIcon   = Icons.Outlined.Lock,
                visualTransformation = if (passVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError      = passwordError != null && password.isNotEmpty(),
                errorMessage = passwordError,
                trailingIcon = {
                    VaultIconButton(
                        icon = if (passVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        onClick = viewModel::togglePasswordVisibility
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            // ── Confirm password ──────────────────────────────────────────────
            VaultTextField(
                value         = confirmPass,
                onValueChange = viewModel::onConfirmPassChanged,
                label         = "Confirm Password",
                leadingIcon   = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus(); viewModel.register() }
                ),
                isError      = confirmError != null,
                errorMessage = confirmError
            )

            Spacer(Modifier.height(32.dp))

            // ── Password strength indicator ───────────────────────────────────
            AnimatedVisibility(visible = password.isNotEmpty()) {
                Column {
                    PasswordStrengthBar(password = password)
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Register button ───────────────────────────────────────────────
            VaultButton(
                text      = "Create Vault",
                onClick   = { focusManager.clearFocus(); viewModel.register() },
                isLoading = isLoading,
                enabled   = email.isNotEmpty() && password.isNotEmpty() &&
                            confirmPass.isNotEmpty() && displayName.isNotEmpty()
            )

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text  = "Sign in",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Zero-Knowledge info banner ────────────────────────────────────────────────
@Composable
private fun ZeroKnowledgeBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = ShapeCard,
        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = "Zero-Knowledge Encryption",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text  = "Your master password encrypts everything locally. Not even VaultX can access your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Password strength bar ─────────────────────────────────────────────────────
@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = calculateStrength(password)
    val (label, color, fraction) = when (strength) {
        0    -> Triple("Weak",   MaterialTheme.colorScheme.error,     0.25f)
        1    -> Triple("Fair",   DarkWarning,                          0.50f)
        2    -> Triple("Good",   MaterialTheme.colorScheme.secondary,  0.75f)
        else -> Triple("Strong", MaterialTheme.colorScheme.secondary,  1.00f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Password strength", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color    = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
    }
}

private val DarkWarning = androidx.compose.ui.graphics.Color(0xFFFFA726)

private fun calculateStrength(password: String): Int {
    var score = 0
    if (password.length >= 8)               score++
    if (password.any { it.isUpperCase() })  score++
    if (password.any { it.isDigit() })      score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score - 1
}
