package com.vaultx.admin.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.vaultx.admin.presentation.ui.components.VaultTextField
import com.vaultx.admin.presentation.ui.components.VaultButton
import com.vaultx.admin.presentation.ui.components.VaultButtonVariant
import com.vaultx.admin.presentation.ui.components.VaultIconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.data.model.AppConfig
import com.vaultx.admin.presentation.AdminViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateManagerScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val appConfig by viewModel.appConfig.collectAsState()
    val context = LocalContext.current

    var versionCode by remember(appConfig) { mutableStateOf(appConfig?.latestVersionCode?.toString() ?: "1") }
    var versionName by remember(appConfig) { mutableStateOf(appConfig?.latestVersionName ?: "1.0") }
    var downloadUrl by remember(appConfig) { mutableStateOf(appConfig?.apkDownloadUrl ?: "") }
    var isForcedUpdate by remember(appConfig) { mutableStateOf(appConfig?.isForcedUpdate ?: false) }
    var changelog by remember(appConfig) { mutableStateOf(appConfig?.changelog?.joinToString("\n") ?: "") }
    var importMessage by remember { mutableStateOf<String?>(null) }

    val jsonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()

                val json = JSONObject(jsonString)
                versionCode = json.optInt("versionCode", 1).toString()
                versionName = json.optString("versionName", "1.0")
                downloadUrl = json.optString("downloadUrl", "")
                isForcedUpdate = json.optBoolean("forcedUpdate", false)

                val changelogArray = json.optJSONArray("changelog")
                if (changelogArray != null) {
                    val items = mutableListOf<String>()
                    for (i in 0 until changelogArray.length()) {
                        items.add(changelogArray.getString(i))
                    }
                    changelog = items.joinToString("\n")
                }
                importMessage = "✅ JSON imported successfully"
            } catch (e: Exception) {
                importMessage = "❌ Failed to parse JSON: ${e.message}"
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val config = AppConfig(
                        latestVersionCode = versionCode.toIntOrNull() ?: 1,
                        latestVersionName = versionName,
                        apkDownloadUrl = downloadUrl,
                        changelog = changelog.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        isForcedUpdate = isForcedUpdate,
                        announcement = appConfig?.announcement ?: "",
                        isAnnouncementActive = appConfig?.isAnnouncementActive ?: false,
                        upiId = appConfig?.upiId ?: "",
                        payeeName = appConfig?.payeeName ?: "",
                        isMaintenanceMode = appConfig?.isMaintenanceMode ?: false,
                        maintenanceMessage = appConfig?.maintenanceMessage ?: "",
                        isAutofillEnabled = appConfig?.isAutofillEnabled ?: true,
                        isSignupEnabled = appConfig?.isSignupEnabled ?: true,
                        maxFreeAccounts = appConfig?.maxFreeAccounts ?: 5
                    )
                    viewModel.updateAppConfig(config)
                    onBack()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Save, contentDescription = "Publish Update")
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
                    "App Update Manager",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            // ── Import JSON Button ────────────────────────────────────────────
            VaultButton(
                text = "Import from JSON",
                onClick = { jsonPicker.launch("application/json") },
                variant = VaultButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.FileOpen
            )

            if (importMessage != null) {
                Text(
                    text = importMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (importMessage!!.startsWith("✅")) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
            }

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
                        Text("Release New Version", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        VaultTextField(
                            value = versionCode,
                            onValueChange = { versionCode = it },
                            label = "Version Code",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        VaultTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = "Version Name",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    VaultTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = "APK Download URL",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Force Update Required?", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isForcedUpdate,
                            onCheckedChange = { isForcedUpdate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    VaultTextField(
                        value = changelog,
                        onValueChange = { changelog = it },
                        label = "Changelog (One item per line)",
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        singleLine = false
                    )
                }
            }
            Spacer(Modifier.height(80.dp)) // FAB padding
        }
    }
}
