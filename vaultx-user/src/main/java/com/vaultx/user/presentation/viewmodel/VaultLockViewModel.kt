package com.vaultx.user.presentation.viewmodel

import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultx.user.di.VaultSessionManager
import com.vaultx.user.domain.repository.AuthRepository
import com.vaultx.user.security.BiometricHelper
import com.vaultx.user.security.BiometricResult
import com.vaultx.user.security.CryptoManager
import com.vaultx.user.security.EncryptedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VaultLockUiState {
    object Idle : VaultLockUiState()
    object Authenticating : VaultLockUiState()
    object Success : VaultLockUiState()
    data class Error(val message: String) : VaultLockUiState()
}

@HiltViewModel
class VaultLockViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cryptoManager: CryptoManager,
    private val prefs: SharedPreferences,
    private val vaultSession: VaultSessionManager
) : ViewModel() {

    private val biometricHelper = BiometricHelper()

    val isBiometricEnabled: Boolean
        get() = prefs.getBoolean("pref_biometric_enabled", false)

    val isAppLockEnabled: Boolean
        get() = prefs.getBoolean("pref_app_lock_enabled", false)

    private val _uiState = MutableStateFlow<VaultLockUiState>(VaultLockUiState.Idle)
    val uiState: StateFlow<VaultLockUiState> = _uiState.asStateFlow()

    fun unlockWithMasterPassword(password: String) {
        viewModelScope.launch {
            _uiState.value = VaultLockUiState.Authenticating
            val result = authRepository.unlockWithMasterPassword(password)
            if (result.isSuccess) {
                _uiState.value = VaultLockUiState.Success
            } else {
                val exception = result.exceptionOrNull()
                val message = if (exception?.message?.contains("decryption", ignoreCase = true) == true ||
                    exception?.message?.contains("crypto", ignoreCase = true) == true ||
                    exception is javax.crypto.AEADBadTagException) {
                    "Incorrect master password"
                } else {
                    exception?.localizedMessage ?: "Incorrect master password"
                }
                _uiState.value = VaultLockUiState.Error(message)
            }
        }
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            _uiState.value = VaultLockUiState.Authenticating
            val result = authRepository.unlockWithPin(pin)
            if (result.isSuccess) {
                _uiState.value = VaultLockUiState.Success
            } else {
                val exception = result.exceptionOrNull()
                val message = if (exception?.message?.contains("decryption", ignoreCase = true) == true ||
                    exception?.message?.contains("crypto", ignoreCase = true) == true ||
                    exception is javax.crypto.AEADBadTagException) {
                    "Incorrect PIN"
                } else {
                    exception?.localizedMessage ?: "Incorrect PIN"
                }
                _uiState.value = VaultLockUiState.Error(message)
            }
        }
    }

    fun attemptBiometricUnlock(activity: FragmentActivity?) {
        if (activity == null) return
        _uiState.value = VaultLockUiState.Authenticating

        val keystoreKey = runCatching { cryptoManager.getOrCreateKeystoreKey() }.getOrNull()
        if (keystoreKey == null) {
            _uiState.value = VaultLockUiState.Error("Keystore unavailable")
            return
        }

        biometricHelper.authenticate(
            activity = activity,
            title = "Unlock VaultX",
            subtitle = "Verify identity to open vault",
            onResult = { result ->
                when (result) {
                    is BiometricResult.Success -> {
                        // Decrypt the DB key using keystoreKey
                        viewModelScope.launch {
                            try {
                                val ciphertext = prefs.getString("wrapped_db_key", null)
                                val iv = prefs.getString("wrapped_db_iv", null)
                                if (ciphertext != null && iv != null) {
                                    val encryptedData = EncryptedData(ciphertext, iv)
                                    val dbKey = cryptoManager.unwrapKey(encryptedData, keystoreKey)
                                    vaultSession.openVault(cryptoManager.keyToBytes(dbKey))
                                    _uiState.value = VaultLockUiState.Success
                                } else {
                                    _uiState.value = VaultLockUiState.Error("No vault keys found. Use Master Password.")
                                }
                            } catch (e: Exception) {
                                _uiState.value = VaultLockUiState.Error("Decryption failed. Use Master Password.")
                            }
                        }
                    }
                    is BiometricResult.Error -> {
                        _uiState.value = VaultLockUiState.Error(result.message)
                    }
                    BiometricResult.Failed -> {
                        _uiState.value = VaultLockUiState.Error("Biometric not recognized")
                    }
                    BiometricResult.NotAvailable -> {
                        _uiState.value = VaultLockUiState.Error("Biometric not available")
                    }
                }
            }
        )
    }

    fun resetState() {
        _uiState.value = VaultLockUiState.Idle
    }
}
