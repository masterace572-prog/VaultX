package com.vaultx.user.presentation.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vaultx.user.di.VaultSessionManager
import com.vaultx.user.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthState {
    Loading,
    Unauthenticated,
    NeedsVaultUnlock,
    Authenticated
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val firebaseAuth:   FirebaseAuth,
    private val vaultSession:   VaultSessionManager,
    private val dataStore:      DataStore<Preferences>,
    private val cryptoManager:  com.vaultx.user.security.CryptoManager,
    private val encryptedPrefs: android.content.SharedPreferences
) : ViewModel() {

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_COLOR_KEY = stringPreferencesKey("app_color")
        val CORNER_RADIUS_KEY = stringPreferencesKey("corner_radius")
        val UI_DENSITY_KEY = stringPreferencesKey("ui_density")
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        val FONT_FAMILY_KEY = stringPreferencesKey("font_family")
        val CARD_STYLE_KEY = stringPreferencesKey("card_style")
        val NAV_STYLE_KEY = stringPreferencesKey("nav_style")
        val ANIMATIONS_KEY = booleanPreferencesKey("animations_enabled")
        val BLUR_EFFECTS_KEY = booleanPreferencesKey("blur_effects")
        val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")
        val ICON_SHAPE_KEY = stringPreferencesKey("icon_shape")
        val DASHBOARD_LAYOUT_KEY = stringPreferencesKey("dashboard_layout")
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val uiEngine: StateFlow<VaultUIEngine> = dataStore.data.map { prefs ->
        VaultUIEngine(
            themeMode = safeValueOf(prefs[THEME_MODE_KEY], ThemeMode.SYSTEM),
            appColor = safeValueOf(prefs[APP_COLOR_KEY], AppColor.DYNAMIC),
            cornerRadius = safeValueOf(prefs[CORNER_RADIUS_KEY], CornerRadiusOption.MEDIUM),
            uiDensity = safeValueOf(prefs[UI_DENSITY_KEY], UIDensity.COMFORTABLE),
            fontSize = safeValueOf(prefs[FONT_SIZE_KEY], FontSizeOption.MEDIUM),
            fontFamily = safeValueOf(prefs[FONT_FAMILY_KEY], FontFamilyOption.SYSTEM),
            cardStyle = safeValueOf(prefs[CARD_STYLE_KEY], CardStyle.FILLED),
            navStyle = safeValueOf(prefs[NAV_STYLE_KEY], NavStyle.BOTTOM_NAV),
            animationsEnabled = prefs[ANIMATIONS_KEY] ?: true,
            blurEffectsEnabled = prefs[BLUR_EFFECTS_KEY] ?: false,
            amoledMode = prefs[AMOLED_MODE_KEY] ?: false,
            iconShape = safeValueOf(prefs[ICON_SHAPE_KEY], IconShapeOption.ROUNDED),
            dashboardLayout = safeValueOf(prefs[DASHBOARD_LAYOUT_KEY], DashboardLayoutOption.LIST)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VaultUIEngine())

    private inline fun <reified T : Enum<T>> safeValueOf(value: String?, default: T): T {
        if (value == null) return default
        return try {
            enumValueOf<T>(value)
        } catch (e: Exception) {
            default
        }
    }

    fun updateEngine(updater: (VaultUIEngine) -> VaultUIEngine) {
        val current = uiEngine.value
        val next = updater(current)
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[THEME_MODE_KEY] = next.themeMode.name
                prefs[APP_COLOR_KEY] = next.appColor.name
                prefs[CORNER_RADIUS_KEY] = next.cornerRadius.name
                prefs[UI_DENSITY_KEY] = next.uiDensity.name
                prefs[FONT_SIZE_KEY] = next.fontSize.name
                prefs[FONT_FAMILY_KEY] = next.fontFamily.name
                prefs[CARD_STYLE_KEY] = next.cardStyle.name
                prefs[NAV_STYLE_KEY] = next.navStyle.name
                prefs[ANIMATIONS_KEY] = next.animationsEnabled
                prefs[BLUR_EFFECTS_KEY] = next.blurEffectsEnabled
                prefs[AMOLED_MODE_KEY] = next.amoledMode
                prefs[ICON_SHAPE_KEY] = next.iconShape.name
                prefs[DASHBOARD_LAYOUT_KEY] = next.dashboardLayout.name
            }
        }
    }

    init {
        resolveInitialAuthState()
    }

    fun checkAuth() {
        resolveInitialAuthState()
    }

    private fun resolveInitialAuthState() {
        viewModelScope.launch {
            _isLoading.value = true
            val user = firebaseAuth.currentUser
            if (user == null) {
                _authState.value = AuthState.Unauthenticated
            } else {
                val hasPin = encryptedPrefs.contains("pref_app_pin_salt")
                val hasBiometrics = encryptedPrefs.getBoolean("pref_biometric_enabled", false)
                
                if (!hasPin && !hasBiometrics) {
                    _authState.value = AuthState.Authenticated
                } else if (vaultSession.isUnlocked()) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.NeedsVaultUnlock
                }
            }
            _isLoading.value = false
        }
    }
}
