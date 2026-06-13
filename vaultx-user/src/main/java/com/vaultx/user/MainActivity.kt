package com.vaultx.user

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.navigation.VaultXNavGraph
import com.vaultx.user.presentation.theme.VaultXTheme
import com.vaultx.user.presentation.viewmodel.AppViewModel
import com.vaultx.user.presentation.ui.update.UpdateBottomSheet
import dagger.hilt.android.AndroidEntryPoint

// ─────────────────────────────────────────────────────────────────────────────
// MainActivity
// • Installs splash screen
// • Sets FLAG_SECURE (no screenshots/screen recording)
// • Edge-to-edge layout
// • Hosts the Compose NavGraph
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("vaultx_prefs", android.content.Context.MODE_PRIVATE) }

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "pref_screenshot_protection") {
            updateScreenshotProtection(sharedPreferences.getBoolean("pref_screenshot_protection", true))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        updateScreenshotProtection(prefs.getBoolean("pref_screenshot_protection", true))
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val homeViewModel: com.vaultx.user.presentation.viewmodel.HomeViewModel = hiltViewModel()
            val isDarkMode by appViewModel.isDarkMode.collectAsState()
            val isLoading by appViewModel.isLoading.collectAsState()
            val appConfig by homeViewModel.appConfig.collectAsState()

            splashScreen.setKeepOnScreenCondition { isLoading || appConfig == null }

            VaultXTheme(userDarkModeOverride = isDarkMode) {
                if (appConfig?.isMaintenanceMode == true) {
                    com.vaultx.user.presentation.ui.maintenance.MaintenanceScreen(
                        message = appConfig?.maintenanceMessage ?: "We are currently under maintenance. Please check back later.",
                        onRetry = { homeViewModel.refresh() }
                    )
                } else {
                    VaultXNavGraph(appViewModel = appViewModel)
                    UpdateBottomSheet()
                }
            }
        }
    }

    private fun updateScreenshotProtection(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
