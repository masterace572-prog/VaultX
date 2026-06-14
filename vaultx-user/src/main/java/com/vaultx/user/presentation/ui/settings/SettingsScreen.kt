package com.vaultx.user.presentation.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack:              () -> Unit,
    onLogout:            () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAppLockSetup: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    showBackButton:      Boolean = true,
    appViewModel:    AppViewModel    = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiEngine by appViewModel.uiEngine.collectAsState()
    
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
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, "Back")
                        }
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
                EnumPickerRow(
                    icon = Icons.Outlined.SettingsBrightness,
                    title = "Theme Mode",
                    currentValue = uiEngine.themeMode,
                    enumValues = ThemeMode.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(themeMode = v) } }
                )
                VaultDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Contrast,
                    title = "AMOLED Dark Mode",
                    subtitle = "Use pure black for dark theme backgrounds",
                    checked = uiEngine.amoledMode,
                    onToggle = { v -> appViewModel.updateEngine { it.copy(amoledMode = v) } }
                )
                VaultDivider()
                // Color Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ColorLens, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("App Color", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Select a global accent color", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colors = listOf(
                                AppColor.BLUE to Color(0xFF1976D2),
                                AppColor.INDIGO to Color(0xFF3F51B5),
                                AppColor.PURPLE to Color(0xFF6750A4),
                                AppColor.GREEN to Color(0xFF386A20),
                                AppColor.ORANGE to Color(0xFFE65100),
                                AppColor.RED to Color(0xFFB3261E),
                                AppColor.TEAL to Color(0xFF006B5E),
                                AppColor.GRAY to Color(0xFF606060),
                                AppColor.DYNAMIC to Color.Transparent
                            )
                            // We can use a scrollable row if needed, but 9 items is tight.
                            // Let's wrap in a scrollable row
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── UI Architecture ───────────────────────────────────────
            SettingsSectionLabel("UI ARCHITECTURE")
            SettingsCard {
                EnumPickerRow(
                    icon = Icons.Outlined.RoundedCorner,
                    title = "Corner Radius",
                    currentValue = uiEngine.cornerRadius,
                    enumValues = CornerRadiusOption.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(cornerRadius = v) } }
                )
                VaultDivider()
                EnumPickerRow(
                    icon = Icons.Outlined.Dashboard,
                    title = "Dashboard Layout",
                    currentValue = uiEngine.dashboardLayout,
                    enumValues = DashboardLayoutOption.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(dashboardLayout = v) } }
                )
                VaultDivider()
                EnumPickerRow(
                    icon = Icons.Outlined.Layers,
                    title = "Card Style",
                    currentValue = uiEngine.cardStyle,
                    enumValues = CardStyle.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(cardStyle = v) } }
                )
                VaultDivider()
                EnumPickerRow(
                    icon = Icons.Outlined.FormatLineSpacing,
                    title = "UI Density",
                    currentValue = uiEngine.uiDensity,
                    enumValues = UIDensity.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(uiDensity = v) } }
                )
                VaultDivider()
                EnumPickerRow(
                    icon = Icons.Outlined.Menu,
                    title = "Navigation Style",
                    currentValue = uiEngine.navStyle,
                    enumValues = NavStyle.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(navStyle = v) } }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Typography ───────────────────────────────────────
            SettingsSectionLabel("TYPOGRAPHY")
            SettingsCard {
                EnumPickerRow(
                    icon = Icons.Outlined.TextFields,
                    title = "Font Size",
                    currentValue = uiEngine.fontSize,
                    enumValues = FontSizeOption.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(fontSize = v) } }
                )
                VaultDivider()
                EnumPickerRow(
                    icon = Icons.Outlined.FontDownload,
                    title = "Font Family",
                    currentValue = uiEngine.fontFamily,
                    enumValues = FontFamilyOption.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(fontFamily = v) } }
                )
            }

            Spacer(Modifier.height(8.dp))
            
            // ── Motion & Graphics ───────────────────────────────────────
            SettingsSectionLabel("MOTION & GRAPHICS")
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Outlined.Animation,
                    title = "Animations",
                    subtitle = "Enable premium screen transitions and effects",
                    checked = uiEngine.animationsEnabled,
                    onToggle = { v -> appViewModel.updateEngine { it.copy(animationsEnabled = v) } }
                )
                VaultDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.BlurOn,
                    title = "Blur Effects",
                    subtitle = "Enable real-time background blurring",
                    checked = uiEngine.blurEffectsEnabled,
                    onToggle = { v -> appViewModel.updateEngine { it.copy(blurEffectsEnabled = v) } }
                )
                VaultDivider()
                EnumPickerRow(
                    icon = Icons.Outlined.CropSquare,
                    title = "Icon Shape",
                    currentValue = uiEngine.iconShape,
                    enumValues = IconShapeOption.values(),
                    onValueSelected = { v -> appViewModel.updateEngine { it.copy(iconShape = v) } }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Security ──────────────────────────────────────────────────────
            SettingsSectionLabel("SECURITY")
            SettingsCard {
                SettingsActionRow(
                    icon     = Icons.Outlined.Lock,
                    title    = "App PIN Lock",
                    subtitle = if (appLockEnabled) "Enabled" else "Disabled",
                    onClick  = onNavigateToAppLockSetup
                )
                if (biometricAvailability == BiometricAvailability.READY) {
                    VaultDivider()
                    SettingsToggleRow(
                        icon     = Icons.Outlined.Fingerprint,
                        title    = "Biometric Unlock",
                        subtitle = "Use fingerprint or face to unlock VaultX",
                        checked  = biometricLockEnabled,
                        onToggle = { enabled ->
                            if (enabled) {
                                biometricHelper.authenticate(
                                    activity = activity ?: return@SettingsToggleRow,
                                    title = "Enable Biometrics",
                                    subtitle = "Verify it's you",
                                    onSuccess = { settingsViewModel.setBiometricLock(true) },
                                    onError = {}
                                )
                            } else {
                                settingsViewModel.setBiometricLock(false)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Backup & Sync ─────────────────────────────────────────────────
            SettingsSectionLabel("BACKUP & SYNC")
            SettingsCard {
                SettingsToggleRow(
                    icon     = Icons.Outlined.CloudSync,
                    title    = "Cloud Sync",
                    subtitle = if (isPremium) "Securely back up to Google Drive" else "Requires Premium",
                    checked  = cloudSyncEnabled,
                    enabled  = isPremium,
                    onToggle = { settingsViewModel.setCloudSync(it) }
                )
                if (cloudSyncEnabled && isPremium) {
                    VaultDivider()
                    SettingsActionRow(
                        icon     = Icons.Outlined.Sync,
                        title    = "Sync Now",
                        subtitle = lastBackupTime?.let { "Last synced: $it" } ?: "Never synced",
                        isLoading = isSyncing,
                        onClick  = { settingsViewModel.syncNow() }
                    )
                }
                VaultDivider()
                SettingsActionRow(
                    icon     = Icons.Outlined.Download,
                    title    = "Export Vault",
                    subtitle = "Save an encrypted backup to your device",
                    onClick  = { showExportDialog = true }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Account Actions ───────────────────────────────────────────────
            SettingsSectionLabel("ACCOUNT")
            SettingsCard {
                if (!isPremium) {
                    SettingsActionRow(
                        icon     = Icons.Outlined.WorkspacePremium,
                        title    = "Upgrade to Premium",
                        subtitle = "Unlock cloud sync and advanced features",
                        onClick  = onNavigateToPremium
                    )
                    VaultDivider()
                }
                SettingsActionRow(
                    icon     = Icons.Outlined.HelpOutline,
                    title    = "Help & Support",
                    subtitle = "Contact us or view FAQs",
                    onClick  = onNavigateToHelpSupport
                )
                VaultDivider()
                SettingsActionRow(
                    icon     = Icons.Outlined.Logout,
                    title    = "Sign Out",
                    subtitle = "Lock vault and sign out",
                    onClick  = { showLogoutDialog = true },
                    isDestructive = true
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        // Export Dialog
        if (showExportDialog) {
            var isExporting by remember { mutableStateOf(false) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) {
                    isExporting = true
                    // Export logic would be handled here in reality
                    isExporting = false
                    showExportDialog = false
                    exportPassword = ""
                } else {
                    showExportDialog = false
                    exportPassword = ""
                }
            }

            AlertDialog(
                onDismissRequest = { if (!isExporting) showExportDialog = false },
                title = { Text("Export Vault") },
                text = {
                    Column {
                        Text("Enter a strong password to encrypt your backup file.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        VaultTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = "Encryption Password",
                            enabled = !isExporting
                        )
                    }
                },
                confirmButton = {
                    VaultButton(
                        text = "Export",
                        onClick = { launcher.launch("VaultX_Backup_${System.currentTimeMillis()}.json") },
                        enabled = exportPassword.length >= 6 && !isExporting,
                        isLoading = isExporting,
                        fullWidth = false
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }, enabled = !isExporting) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Logout Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to lock the vault and sign out?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        settingsViewModel.logout(onLogout)
                    }) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun <T : Enum<T>> EnumPickerRow(
    icon: ImageVector,
    title: String,
    currentValue: T,
    enumValues: Array<T>,
    onValueSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            val friendlyName = currentValue.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
            Text("Selected: $friendlyName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Box {
            Icon(Icons.Outlined.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                enumValues.forEach { value ->
                    val name = value.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = name, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (value == currentValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    VaultCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun VaultDivider() {
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(icon, null, tint = if (isDestructive) contentColor else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = contentColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null, 
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.titleSmall, 
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodySmall, 
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled
        )
    }
}

@Composable
private fun AccountProfileCard(
    name: String,
    email: String,
    isPremium: Boolean,
    accountCount: Int,
    onTap: () -> Unit
) {
    VaultCard(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPremium) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) {
                            Text("PREMIUM", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    Text("$accountCount Vaults", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
