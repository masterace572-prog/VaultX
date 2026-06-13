package com.vaultx.user.presentation.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vaultx.user.di.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// AuthState — determines which navigation start destination to use
// ─────────────────────────────────────────────────────────────────────────────

enum class AuthState {
    Loading,
    Unauthenticated,
    NeedsVaultUnlock,  // Firebase logged in but vault not opened (biometric needed)
    Authenticated      // Firebase logged in AND vault open
}

// ─────────────────────────────────────────────────────────────────────────────
// AppViewModel — top-level shared state (theme, auth state)
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AppViewModel @Inject constructor(
    private val firebaseAuth:   FirebaseAuth,
    private val vaultSession:   VaultSessionManager,
    private val dataStore:      DataStore<Preferences>,
    private val cryptoManager:  com.vaultx.user.security.CryptoManager,
    private val encryptedPrefs: android.content.SharedPreferences
) : ViewModel() {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
        val HIGH_CONTRAST_KEY = booleanPreferencesKey("high_contrast")
        val FONT_SIZE_SCALE_KEY = floatPreferencesKey("font_size_scale")
    }

    // ── Loading state ─────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Auth state ────────────────────────────────────────────────────────────
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ── Theme settings (flows) ────────────────────────────────────────────────
    val isDarkMode: StateFlow<Boolean?> = dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<String> = dataStore.data
        .map { prefs -> prefs[THEME_MODE_KEY] ?: "SYSTEM" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")

    val accentColor: StateFlow<String> = dataStore.data
        .map { prefs -> prefs[ACCENT_COLOR_KEY] ?: "PURPLE" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "PURPLE")

    val highContrast: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[HIGH_CONTRAST_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val fontSizeScale: StateFlow<Float> = dataStore.data
        .map { prefs -> prefs[FONT_SIZE_SCALE_KEY] ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    // ── Theme configuration setters ───────────────────────────────────────────
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode }
            // Keep the legacy DARK_MODE_KEY in sync for backwards-compatibility parts
            val legacyDark = when (mode) {
                "DARK", "AMOLED" -> true
                "LIGHT" -> false
                else -> null
            }
            dataStore.edit { prefs ->
                if (legacyDark != null) prefs[DARK_MODE_KEY] = legacyDark
                else prefs.remove(DARK_MODE_KEY)
            }
        }
    }

    fun setAccentColor(colorName: String) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[ACCENT_COLOR_KEY] = colorName }
        }
    }

    fun toggleHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[HIGH_CONTRAST_KEY] = enabled }
        }
    }

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[FONT_SIZE_SCALE_KEY] = scale }
        }
    }

    init {
        resolveInitialAuthState()
    }

    private fun resolveInitialAuthState() {
        viewModelScope.launch {
            try {
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    _authState.value = AuthState.Unauthenticated
                } else if (vaultSession.isUnlocked) {
                    _authState.value = AuthState.Authenticated
                } else {
                    val isAppLockEnabled = encryptedPrefs.getBoolean("pref_app_lock_enabled", false)
                    val isBiometricEnabled = encryptedPrefs.getBoolean("pref_biometric_enabled", false)
                    if (isAppLockEnabled || isBiometricEnabled) {
                        _authState.value = AuthState.NeedsVaultUnlock
                    } else {
                        try {
                            val keystoreKey = cryptoManager.getOrCreateKeystoreKey()
                            val ciphertext = encryptedPrefs.getString("wrapped_db_key", null)
                            val iv = encryptedPrefs.getString("wrapped_db_iv", null)
                            if (ciphertext != null && iv != null) {
                                val encryptedData = com.vaultx.user.security.EncryptedData(ciphertext, iv)
                                val dbKey = cryptoManager.unwrapKey(encryptedData, keystoreKey)
                                vaultSession.openVault(cryptoManager.keyToBytes(dbKey))
                                _authState.value = AuthState.Authenticated
                            } else {
                                _authState.value = AuthState.NeedsVaultUnlock
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _authState.value = AuthState.NeedsVaultUnlock
                        }
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onVaultUnlocked() {
        _authState.value = AuthState.Authenticated
    }

    fun onLogout() {
        _authState.value = AuthState.Unauthenticated
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
        }
    }
}
