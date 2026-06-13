package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.vaultx.admin.presentation.ui.components.VaultTextField
import com.vaultx.admin.presentation.ui.components.VaultIconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.data.model.AppConfig
import com.vaultx.admin.presentation.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val appConfig by viewModel.appConfig.collectAsState()

    var announcement by remember(appConfig) { mutableStateOf(appConfig?.announcement ?: "") }
    var isAnnouncementActive by remember(appConfig) { mutableStateOf(appConfig?.isAnnouncementActive ?: false) }
    var upiId by remember(appConfig) { mutableStateOf(appConfig?.upiId ?: "") }
    var payeeName by remember(appConfig) { mutableStateOf(appConfig?.payeeName ?: "") }
    var isMaintenanceMode by remember(appConfig) { mutableStateOf(appConfig?.isMaintenanceMode ?: false) }
    var maintenanceMessage by remember(appConfig) { mutableStateOf(appConfig?.maintenanceMessage ?: "We are currently under maintenance. Please check back later.") }
    var isAutofillEnabled by remember(appConfig) { mutableStateOf(appConfig?.isAutofillEnabled ?: true) }
    var isSignupEnabled by remember(appConfig) { mutableStateOf(appConfig?.isSignupEnabled ?: true) }
    var maxFreeAccountsStr by remember(appConfig) { mutableStateOf(appConfig?.maxFreeAccounts?.toString() ?: "5") }
    var isScreenshotAllowed by remember(appConfig) { mutableStateOf(appConfig?.isScreenshotAllowed ?: false) }
    var supportEmail by remember(appConfig) { mutableStateOf(appConfig?.supportEmail ?: "") }
    var discordLink by remember(appConfig) { mutableStateOf(appConfig?.discordLink ?: "") }
    var updateDialogMessage by remember(appConfig) { mutableStateOf(appConfig?.updateDialogMessage ?: "A new update is available. Please update to the latest version.") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val config = AppConfig(
                        latestVersionCode = appConfig?.latestVersionCode ?: 1,
                        latestVersionName = appConfig?.latestVersionName ?: "1.0",
                        apkDownloadUrl = appConfig?.apkDownloadUrl ?: "",
                        changelog = appConfig?.changelog ?: emptyList(),
                        isForcedUpdate = appConfig?.isForcedUpdate ?: false,
                        announcement = announcement,
                        isAnnouncementActive = isAnnouncementActive,
                        upiId = upiId,
                        payeeName = payeeName,
                        isMaintenanceMode = isMaintenanceMode,
                        maintenanceMessage = maintenanceMessage,
                        isAutofillEnabled = isAutofillEnabled,
                        isSignupEnabled = isSignupEnabled,
                        maxFreeAccounts = maxFreeAccountsStr.toIntOrNull() ?: 5,
                        isScreenshotAllowed = isScreenshotAllowed,
                        supportEmail = supportEmail,
                        discordLink = discordLink,
                        updateDialogMessage = updateDialogMessage
                    )
                    viewModel.updateAppConfig(config)
                    onBack()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Save, contentDescription = "Save")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Inline Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                VaultIconButton(
                    icon = Icons.Outlined.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "App Configuration",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            // ── Maintenance Mode Card ────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Maintenance Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Enable Global Maintenance?", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isMaintenanceMode, 
                            onCheckedChange = { isMaintenanceMode = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    VaultTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = "Maintenance Message",
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        enabled = isMaintenanceMode,
                        singleLine = false
                    )
                }
            }

            // ── Global Announcement Card ─────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Global Announcement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Is Announcement Active?", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isAnnouncementActive, 
                            onCheckedChange = { isAnnouncementActive = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    VaultTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = "Announcement Message",
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        enabled = isAnnouncementActive,
                        singleLine = false
                    )
                }
            }

            // ── Feature Toggles Card ─────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Feature Toggles", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Enable Autofill Service", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isAutofillEnabled, 
                            onCheckedChange = { isAutofillEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Enable New Signups", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isSignupEnabled, 
                            onCheckedChange = { isSignupEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    VaultTextField(
                        value = maxFreeAccountsStr,
                        onValueChange = { maxFreeAccountsStr = it },
                        label = "Max Free Accounts Limit",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // ── Payment Settings Card ────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Payment Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    VaultTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = "UPI ID",
                        modifier = Modifier.fillMaxWidth()
                    )

                    VaultTextField(
                        value = payeeName,
                        onValueChange = { payeeName = it },
                        label = "Payee Name",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // ── Other Settings Card ────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Other Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Allow Screenshots Globally", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isScreenshotAllowed,
                            onCheckedChange = { isScreenshotAllowed = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    VaultTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = "Support Email",
                        modifier = Modifier.fillMaxWidth()
                    )

                    VaultTextField(
                        value = discordLink,
                        onValueChange = { discordLink = it },
                        label = "Discord Link",
                        modifier = Modifier.fillMaxWidth()
                    )

                    VaultTextField(
                        value = updateDialogMessage,
                        onValueChange = { updateDialogMessage = it },
                        label = "Update Dialog Message",
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        singleLine = false
                    )
                }
            }
            
            Spacer(Modifier.height(80.dp)) // FAB padding
        }
    }
}
