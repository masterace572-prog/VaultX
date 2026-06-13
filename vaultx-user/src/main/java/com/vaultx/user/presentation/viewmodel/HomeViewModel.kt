package com.vaultx.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.data.model.AppConfig
import com.vaultx.user.presentation.ui.home.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel — user profile + app config (announcements, update check)
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore,
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _appConfig = MutableStateFlow<AppConfig?>(null)
    val appConfig: StateFlow<AppConfig?> = _appConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        _isLoading.value = true
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _error.value = null
            runCatching {
                // Load user profile
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) {
                    val doc = firestore.collection("users").document(uid).get().await()
                    val tier      = doc.getString("tier") ?: "free"
                    val expiryMs  = doc.getLong("premium_expiry")
                    val daysLeft  = expiryMs?.let {
                        val diff = it - System.currentTimeMillis()
                        if (diff > 0) (diff / 86_400_000L).toInt() else 0
                    }
                    val planName  = doc.getString("plan_name")
                    _userProfile.value = UserProfile(
                        displayName = doc.getString("display_name") ?: "there",
                        email       = doc.getString("email") ?: "",
                        isPremium   = tier == "premium" && (daysLeft ?: 0) > 0,
                        planName    = planName,
                        daysLeft    = daysLeft
                    )
                }

                // Load app config (announcements, update info)
                val configDoc = firestore.collection("app_config").document("main").get().await()
                _appConfig.value = AppConfig(
                    latestVersionCode  = configDoc.getLong("latest_version_code")?.toInt() ?: 0,
                    latestVersionName  = configDoc.getString("latest_version_name") ?: "",
                    apkDownloadUrl     = configDoc.getString("apk_download_url") ?: "",
                    changelog          = (configDoc.get("changelog") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList(),
                    isForcedUpdate     = configDoc.getBoolean("is_forced_update") ?: false,
                    announcementText   = configDoc.getString("announcement") ?: "",
                    announcementActive = configDoc.getBoolean("is_announcement_active") ?: false,
                    enabledTemplates   = (configDoc.get("enabled_templates") as? List<*>)
                                            ?.filterIsInstance<String>()
                                            ?: listOf("instagram","twitter","facebook","google","custom"),
                    upiId              = configDoc.getString("upi_id") ?: "",
                    payeeName          = configDoc.getString("payee_name") ?: "",
                    isMaintenanceMode  = configDoc.getBoolean("is_maintenance_mode") ?: false,
                    maintenanceMessage = configDoc.getString("maintenance_message") ?: "We are currently under maintenance. Please check back later.",
                    isAutofillEnabled  = configDoc.getBoolean("is_autofill_enabled") ?: true,
                    isSignupEnabled    = configDoc.getBoolean("is_signup_enabled") ?: true,
                    maxFreeAccounts    = configDoc.getLong("max_free_accounts")?.toInt() ?: 5,
                    isScreenshotAllowed = configDoc.getBoolean("is_screenshot_allowed") ?: false,
                    supportEmail       = configDoc.getString("support_email") ?: "",
                    discordLink        = configDoc.getString("discord_link") ?: "",
                    updateDialogMessage= configDoc.getString("update_dialog_message") ?: "A new update is available. Please update to the latest version."
                )
            }.onFailure { e ->
                android.util.Log.e("HomeViewModel", "Error loading profile/config", e)
                _error.value = "Failed to load dashboard: ${e.localizedMessage ?: "Network error"}"
            }
            _isLoading.value = false
        }
    }
}
