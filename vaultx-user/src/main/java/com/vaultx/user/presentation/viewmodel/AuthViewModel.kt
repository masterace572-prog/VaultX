package com.vaultx.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultx.user.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// AuthViewModel — Login and Register screen state
// ─────────────────────────────────────────────────────────────────────────────

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    // ── Form fields (shared between login and register) ───────────────────────
    private val _email        = MutableStateFlow("")
    private val _password     = MutableStateFlow("")
    private val _displayName  = MutableStateFlow("")
    private val _confirmPass  = MutableStateFlow("")

    val email:        StateFlow<String> = _email.asStateFlow()
    val password:     StateFlow<String> = _password.asStateFlow()
    val displayName:  StateFlow<String> = _displayName.asStateFlow()
    val confirmPass:  StateFlow<String> = _confirmPass.asStateFlow()

    // ── UI state ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Password visibility ───────────────────────────────────────────────────
    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    // ── Validation errors ─────────────────────────────────────────────────────
    val emailError: StateFlow<String?> = email.map { validateEmail(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val passwordError: StateFlow<String?> = password.map { validatePassword(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val confirmPassError: StateFlow<String?> = combine(password, confirmPass) { p, c ->
        if (c.isNotEmpty() && p != c) "Passwords do not match" else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // ── Field update handlers ─────────────────────────────────────────────────
    fun onEmailChanged(v: String)       { _email.value = v }
    fun onPasswordChanged(v: String)    { _password.value = v }
    fun onDisplayNameChanged(v: String) { _displayName.value = v }
    fun onConfirmPassChanged(v: String) { _confirmPass.value = v }
    fun togglePasswordVisibility()      { _passwordVisible.value = !_passwordVisible.value }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun login() {
        if (!isLoginFormValid()) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(_email.value.trim(), _password.value)
            _uiState.value = if (result.isSuccess) AuthUiState.Success
                             else AuthUiState.Error(friendlyError(result.exceptionOrNull()))
        }
    }

    fun register() {
        if (!isRegisterFormValid()) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(
                email       = _email.value.trim(),
                password    = _password.value,
                displayName = _displayName.value.trim()
            )
            _uiState.value = if (result.isSuccess) AuthUiState.Success
                             else AuthUiState.Error(friendlyError(result.exceptionOrNull()))
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }

    fun forgotPassword() {
        val emailValue = _email.value.trim()
        if (emailValue.isEmpty() || validateEmail(emailValue) != null) {
            _uiState.value = AuthUiState.Error("Please enter a valid email to reset your password")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                firebaseAuth.sendPasswordResetEmail(emailValue).await()
                _uiState.value = AuthUiState.Error("Password reset email sent! Check your inbox.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(friendlyError(e))
            }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun isLoginFormValid(): Boolean {
        val eErr = validateEmail(_email.value)
        val pErr = validatePassword(_password.value)
        if (eErr != null || pErr != null) {
            _uiState.value = AuthUiState.Error("Please fix the errors above")
            return false
        }
        return true
    }

    private fun isRegisterFormValid(): Boolean {
        val eErr = validateEmail(_email.value)
        val pErr = validatePassword(_password.value)
        val cErr = if (_password.value != _confirmPass.value) "Passwords do not match" else null
        val nErr = if (_displayName.value.isBlank()) "Name cannot be empty" else null
        if (listOfNotNull(eErr, pErr, cErr, nErr).isNotEmpty()) {
            _uiState.value = AuthUiState.Error("Please fix the errors above")
            return false
        }
        return true
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email cannot be empty"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email"
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) return "Minimum 8 characters"
        if (!password.any { it.isUpperCase() }) return "Include at least one uppercase letter"
        if (!password.any { it.isDigit() }) return "Include at least one number"
        return null
    }

    private fun friendlyError(e: Throwable?): String = when {
        e == null -> "Unknown error"
        e.message?.contains("INVALID_PASSWORD") == true ||
        e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Invalid email or password"
        e.message?.contains("EMAIL_EXISTS") == true -> "This email is already registered"
        e.message?.contains("network") == true -> "No internet connection"
        else -> e.message ?: "Something went wrong"
    }
}
