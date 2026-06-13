package com.vaultx.user.presentation.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AppViewModel
import com.vaultx.user.presentation.viewmodel.SettingsViewModel
import com.vaultx.user.security.BiometricAvailability
import com.vaultx.user.security.BiometricHelper

// ─────────────────────────────────────────────────────────────────────────────
// SettingsScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack:              () -> Unit,
    onLogout:            () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAppLockSetup: () -> Unit,
    onNavigateToPremium: () -> Unit,
    appViewModel:    AppViewModel    = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val isDarkMode      by appViewModel.isDarkMode.collectAsState()
    val accountCount    by settingsViewModel.accountCount.collectAsState()
    val cloudSyncEnabled by settingsViewModel.cloudSyncEnabled.collectAsState()
    val biometricLockEnabled by settingsViewModel.biometricLockEnabled.collectAsState()
    val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
    val userEmail       by settingsViewModel.userEmail.collectAsState()
    val userName        by settingsViewModel.userName.collectAsState()
    val isPremium       by settingsViewModel.isPremium.collectAsState()
    val isSyncing       by settingsViewModel.isSyncing.collectAsState()
    val lastBackupTime  by settingsViewModel.lastBackupTime.collectAsState()

    val context  = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricHelper = remember { BiometricHelper() }
    val biometricAvailability = remember {
        if (activity != null) biometricHelper.checkAvailability(context)
        else BiometricAvailability.NO_HARDWARE
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Account card ──────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            AccountProfileCard(
                name        = userName,
                email       = userEmail,
                isPremium   = isPremium,
                accountCount = accountCount,
                onTap       = onNavigateToProfile
            )

            Spacer(Modifier.height(8.dp))

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSectionLabel("APPEARANCE")
            SettingsCard {
                SettingsToggleRow(
                    icon    = if (isDarkMode == true) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    title   = "Dark Mode",
                    subtitle = "Switch between light and dark theme",
                    checked = isDarkMode ?: false,
                    onToggle = { appViewModel.toggleDarkMode(it) }
                )
            }

            // ── Security ──────────────────────────────────────────────────────
            SettingsSectionLabel("SECURITY")
            SettingsCard {
                SettingsToggleRow(
                    icon     = Icons.Outlined.Fingerprint,
                    title    = "Biometric Lock",
                    subtitle = when (biometricAvailability) {
                        BiometricAvailability.AVAILABLE   -> "Require fingerprint/face to open app"
                        BiometricAvailability.NOT_ENROLLED -> "No biometrics enrolled on this device"
                        else                               -> "Biometric hardware not available"
                    },
                    checked  = biometricLockEnabled,
                    enabled  = biometricAvailability == BiometricAvailability.AVAILABLE,
                    onToggle = { settingsViewModel.setBiometricLock(it) }
                )
                VaultDivider()
                SettingsToggleRow(
                    icon     = Icons.Outlined.Pin,
                    title    = "App Lock PIN",
                    subtitle = "Require a custom PIN to open vault",
                    checked  = appLockEnabled,
                    onToggle = { enable ->
                        if (enable) {
                            onNavigateToAppLockSetup()
                        } else {
                            settingsViewModel.setAppLock(false)
                        }
                    }
                )
                VaultDivider()
                SettingsToggleRow(
                    icon     = Icons.Outlined.ScreenLockPortrait,
                    title    = "Screenshot Protection",
                    subtitle = "Prevent screenshots and screen recording",
                    checked  = settingsViewModel.screenshotProtectionEnabled.collectAsState().value,
                    onToggle = { settingsViewModel.setScreenshotProtection(it) }
                )
            }

            // ── Device Integrations ───────────────────────────────────────────
            SettingsSectionLabel("DEVICE INTEGRATIONS")
            SettingsCard {
                SettingsActionRow(
                    icon     = Icons.Outlined.AutoFixHigh,
                    title    = "Android Autofill Service",
                    subtitle = "Enable VaultX to autofill passwords in apps and websites",
                    onClick  = {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                            intent.data = android.net.Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback for older devices or if action not found
                            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // ── Cloud & Storage ───────────────────────────────────────────────
            SettingsSectionLabel("CLOUD & STORAGE")
            SettingsCard {
                SettingsToggleRow(
                    icon     = Icons.Outlined.CloudSync,
                    title    = "Cloud Backup",
                    subtitle = if (isPremium) "Encrypted sync to Firestore"
                               else "Requires Premium",
                    checked  = cloudSyncEnabled,
                    enabled  = isPremium,
                    onToggle = { settingsViewModel.setCloudSync(it) }
                )
                if (cloudSyncEnabled && isPremium) {
                    VaultDivider()
                    if (lastBackupTime != null) {
                        SettingsInfoRow(
                            icon     = Icons.Outlined.Update,
                            title    = "Last Backup",
                            subtitle = lastBackupTime!!,
                            valueText = "",
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VaultDivider()
                    }
                    SettingsActionRow(
                        icon     = Icons.Outlined.Sync,
                        title    = "Sync Now",
                        subtitle = "Push pending changes to cloud",
                        isLoading = isSyncing,
                        onClick  = { settingsViewModel.syncNow() }
                    )
                }
                VaultDivider()
                SettingsInfoRow(
                    icon      = Icons.Outlined.Storage,
                    title     = "Local Vault",
                    subtitle  = "Encrypted with AES-256-GCM",
                    valueText = "$accountCount accounts",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Premium ───────────────────────────────────────────────────────
            if (!isPremium) {
                SettingsSectionLabel("UPGRADE")
                Surface(
                    onClick  = onNavigateToPremium,
                    shape    = ShapeCard,
                    color    = DarkBadgePremium.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.WorkspacePremium, null,
                            tint = DarkBadgePremium, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Go Premium", style = MaterialTheme.typography.titleSmall,
                                color = DarkBadgePremium)
                            Text("Unlimited accounts · Cloud Backup · Biometric Lock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null,
                            tint = DarkBadgePremium, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            SettingsSectionLabel("ABOUT")
            SettingsCard {
                SettingsInfoRow(
                    icon      = Icons.Outlined.Info,
                    title     = "Version",
                    subtitle  = "VaultX for Android",
                    valueText = "v${com.vaultx.user.BuildConfig.APP_VERSION_NAME}",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VaultDivider()
                SettingsInfoRow(
                    icon      = Icons.Outlined.Security,
                    title     = "Encryption",
                    subtitle  = "Zero-Knowledge AES-256-GCM",
                    valueText = "Active",
                    valueColor = MaterialTheme.colorScheme.secondary
                )
            }

            // ── Danger zone ───────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                SettingsActionRow(
                    icon     = Icons.Outlined.Logout,
                    title    = "Sign Out",
                    subtitle = "Lock vault and sign out",
                    tint     = MaterialTheme.colorScheme.error,
                    onClick  = { showLogoutDialog = true }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon  = { Icon(Icons.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Sign Out?") },
            text  = { Text("Your local vault will be locked. You'll need your master password to unlock it again.") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; settingsViewModel.logout(onLogout) },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
            shape          = ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun AccountProfileCard(
    name: String,
    email: String,
    isPremium: Boolean,
    accountCount: Int,
    onTap: () -> Unit
) {
    Surface(
        onClick  = onTap,
        shape    = ShapeCard,
        color    = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Surface(shape = ShapeFull,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                Icon(Icons.Outlined.Person, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp).size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name.ifBlank { "User" }, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(email.ifBlank { "No Email" }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TierBadge(isPremium = isPremium)
                    Text("$accountCount saved", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = ShapeCard, color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggleRow(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
    enabled:  Boolean = true,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            enabled         = enabled,
            colors          = SwitchDefaults.colors(
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon:       ImageVector,
    title:      String,
    subtitle:   String,
    valueText:  String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(valueText, style = MaterialTheme.typography.labelMedium, color = valueColor)
    }
}

@Composable
private fun SettingsActionRow(
    icon:      ImageVector,
    title:     String,
    subtitle:  String,
    onClick:   () -> Unit,
    tint:      androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    isLoading: Boolean = false
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp, color = tint)
            } else {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
