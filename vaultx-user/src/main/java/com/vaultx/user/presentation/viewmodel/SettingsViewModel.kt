package com.vaultx.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.di.VaultSessionManager
import com.vaultx.user.domain.repository.AccountRepository
import com.vaultx.user.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SettingsViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository:    AuthRepository,
    private val accountRepository: AccountRepository,
    private val vaultSession:      VaultSessionManager,
    private val firebaseAuth:      FirebaseAuth,
    private val firestore:         FirebaseFirestore,
    private val prefs:             android.content.SharedPreferences,
) : ViewModel() {

    private val _accountCount       = MutableStateFlow(0)
    private val _cloudSyncEnabled   = MutableStateFlow(false)
    private val _biometricLockEnabled = MutableStateFlow(false)
    private val _appLockEnabled     = MutableStateFlow(false)
    private val _userEmail          = MutableStateFlow("")
    private val _userName           = MutableStateFlow("")
    private val _isPremium          = MutableStateFlow(false)
    private val _isSyncing          = MutableStateFlow(false)
    private val _lastBackupTime     = MutableStateFlow<String?>(null)
    private val _screenshotProtectionEnabled = MutableStateFlow(true)

    val accountCount:       StateFlow<Int>     = _accountCount.asStateFlow()
    val cloudSyncEnabled:   StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()
    val appLockEnabled:     StateFlow<Boolean> = _appLockEnabled.asStateFlow()
    val userEmail:          StateFlow<String>  = _userEmail.asStateFlow()
    val userName:           StateFlow<String>  = _userName.asStateFlow()
    val isPremium:          StateFlow<Boolean> = _isPremium.asStateFlow()
    val isSyncing:          StateFlow<Boolean> = _isSyncing.asStateFlow()
    val lastBackupTime:     StateFlow<String?> = _lastBackupTime.asStateFlow()
    val screenshotProtectionEnabled: StateFlow<Boolean> = _screenshotProtectionEnabled.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _accountCount.value = accountRepository.countAccounts()
            _userEmail.value    = firebaseAuth.currentUser?.email ?: ""
            _biometricLockEnabled.value = prefs.getBoolean("pref_biometric_enabled", false)
            _appLockEnabled.value = prefs.getBoolean("pref_app_lock_enabled", false)
            _screenshotProtectionEnabled.value = prefs.getBoolean("pref_screenshot_protection", true)
            _userName.value = firebaseAuth.currentUser?.displayName
                ?: firebaseAuth.currentUser?.email?.substringBefore("@")
                ?: "User"

            runCatching {
                val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
                val doc = firestore.collection("users").document(uid).get().await()
                val tier   = doc.getString("tier") ?: "free"
                val expiry = doc.getLong("premium_expiry")
                val daysLeft = expiry?.let {
                    val diff = it - System.currentTimeMillis()
                    if (diff > 0) (diff / 86_400_000L).toInt() else 0
                } ?: 0
                _isPremium.value = tier == "premium" && daysLeft > 0
                
                val name = doc.getString("name")
                if (!name.isNullOrBlank()) {
                    _userName.value = name
                }
            }
            
            _cloudSyncEnabled.value = prefs.getBoolean("pref_cloud_sync_enabled", false)
            val backupTime = prefs.getLong("last_backup_timestamp", 0L)
            if (backupTime > 0) {
                val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(backupTime))
                _lastBackupTime.value = date
            }
        }
    }

    fun setBiometricLock(enabled: Boolean) { 
        _biometricLockEnabled.value = enabled 
        prefs.edit().putBoolean("pref_biometric_enabled", enabled).apply()
    }

    fun setAppLock(enabled: Boolean) {
        _appLockEnabled.value = enabled
        prefs.edit().putBoolean("pref_app_lock_enabled", enabled).apply()
        if (!enabled) {
            // Also clear the PIN data if they turn it off
            prefs.edit().remove("pref_app_pin_salt").remove("pref_wrapped_db_key_by_pin").apply()
        }
    }

    fun setupAppPin(pin: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.setupAppPin(pin)
            if (result.isSuccess) {
                setAppLock(true)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun setCloudSync(enabled: Boolean) { 
        _cloudSyncEnabled.value = enabled 
        prefs.edit().putBoolean("pref_cloud_sync_enabled", enabled).apply()
        if (enabled) {
            viewModelScope.launch {
                _isSyncing.value = true
                accountRepository.syncFromCloud()
                accountRepository.syncToCloud()
                prefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
                val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis()))
                _lastBackupTime.value = date
                _isSyncing.value = false
            }
        }
    }

    fun setScreenshotProtection(enabled: Boolean) {
        _screenshotProtectionEnabled.value = enabled
        prefs.edit().putBoolean("pref_screenshot_protection", enabled).apply()
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            accountRepository.syncFromCloud()
            accountRepository.syncToCloud()
            prefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
            val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis()))
            _lastBackupTime.value = date
            _isSyncing.value = false
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            vaultSession.lockVault()
            onLogout()
        }
    }
}
