package com.vaultx.user.presentation.ui.update

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AppUpdateViewModel
import com.vaultx.user.presentation.viewmodel.UpdateCheckState
import com.vaultx.user.presentation.viewmodel.UpdateInfo
import com.vaultx.user.update.DownloadState

// ─────────────────────────────────────────────────────────────────────────────
// UpdateBottomSheet — elegant update prompt shown from MainActivity
// Forced updates: cannot be dismissed until installed
// Optional updates: can be skipped
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    viewModel: AppUpdateViewModel = hiltViewModel()
) {
    val context       = LocalContext.current
    val updateState   by viewModel.updateState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val showSheet = updateState is UpdateCheckState.UpdateAvailable

    if (showSheet) {
        val info = (updateState as UpdateCheckState.UpdateAvailable).info

        ModalBottomSheet(
            onDismissRequest = {
                if (!info.isForced) viewModel.dismissUpdate()
            },
            shape          = ShapeBottomSheet,
            containerColor = MaterialTheme.colorScheme.surface,
            // Forced update: prevent swipe-to-dismiss
            sheetState     = rememberModalBottomSheetState(
                skipPartiallyExpanded     = true,
                confirmValueChange        = { if (info.isForced) false else true }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape    = ShapeFull,
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text  = if (info.isForced) "Update Required" else "Update Available",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = "Version ${info.versionName} is ready to install",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (info.isForced) {
                        Surface(
                            shape = ShapeChip,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Warning, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                Text("This update is required to continue",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // ── Changelog ─────────────────────────────────────────────────
                if (info.changelog.isNotEmpty()) {
                    Surface(
                        shape    = ShapeCard,
                        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("What's new", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface)
                            info.changelog.forEach { item ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment     = Alignment.Top
                                ) {
                                    Text("•", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(item, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // ── Download progress ─────────────────────────────────────────
                AnimatedContent(
                    targetState  = downloadState,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label        = "download_state"
                ) { state ->
                    when (state) {
                        DownloadState.Idle, DownloadState.Queued -> {
                            DownloadIdleContent(
                                info      = info,
                                onDownload = { viewModel.startDownload(info) },
                                onSkip     = if (!info.isForced) ({ viewModel.dismissUpdate() }) else null
                            )
                        }
                        is DownloadState.Downloading -> {
                            DownloadProgressContent(percent = state.percent)
                        }
                        is DownloadState.Completed -> {
                            DownloadCompleteContent(
                                onClick = { viewModel.installApk(context, state.file) }
                            )
                        }
                        is DownloadState.Failed -> {
                            DownloadFailedContent(
                                reason  = state.reason,
                                onRetry = { viewModel.startDownload(info) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Download states ────────────────────────────────────────────────────────────

@Composable
private fun DownloadIdleContent(
    info:      UpdateInfo,
    onDownload: () -> Unit,
    onSkip:    (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultButton(text = "Download & Install", onClick = onDownload)
        if (onSkip != null) {
            VaultButton(text = "Not Now", onClick = onSkip, variant = VaultButtonVariant.Ghost)
        }
    }
}

@Composable
private fun DownloadProgressContent(percent: Int) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Downloading… $percent%", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        LinearProgressIndicator(
            progress       = { percent / 100f },
            modifier       = Modifier.fillMaxWidth().height(6.dp),
            color          = MaterialTheme.colorScheme.primary,
            trackColor     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            strokeCap      = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text("Do not close the app",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DownloadCompleteContent(onClick: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = ShapeFull,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)) {
            Icon(Icons.Outlined.CheckCircle, null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(12.dp).size(28.dp))
        }
        Text("Download complete!", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface)
        VaultButton(text = "Install Now", onClick = onClick)
    }
}

@Composable
private fun DownloadFailedContent(reason: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Download failed", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error)
        Text(reason, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        VaultButton(text = "Retry", onClick = onRetry, variant = VaultButtonVariant.Secondary)
    }
}


