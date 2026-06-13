package com.vaultx.user.presentation.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.AccountEntry
import com.vaultx.user.data.model.PlatformType
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountUiState
import com.vaultx.user.presentation.viewmodel.AccountViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// AccountDetailScreen — view, reveal, copy, delete
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    entryId:   String,
    onEdit:    () -> Unit,
    onBack:    () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val entry   by viewModel.selectedAccount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var passwordVisible     by remember { mutableStateOf(false) }
    var showDeleteDialog    by remember { mutableStateOf(false) }
    var copiedField         by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(entryId) { viewModel.loadAccountForEdit(entryId) }

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.Success) onBack()
    }

    // Copy-to-clipboard helper with toast feedback
    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        scope.launch {
            copiedField = label
            delay(2000)
            copiedField = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        entry?.platformLabel ?: "Account Detail",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, "Edit",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.DeleteOutline, "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (entry == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val e = entry!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Platform header card ──────────────────────────────────────────
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    PlatformIcon(type = e.platformType, size = 56.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(e.platformLabel, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(e.platformType.displayName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (e.isFavorite) {
                                Surface(shape = ShapeBadge,
                                    color = DarkBadgePremium.copy(alpha = 0.15f)) {
                                    Text("★ Favourite", style = MaterialTheme.typography.labelSmall,
                                        color = DarkBadgePremium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (e.gameAccount != null) {
                                Surface(shape = ShapeBadge,
                                    color = PlatformGame.copy(alpha = 0.15f)) {
                                    Text("GAME", style = MaterialTheme.typography.labelSmall,
                                        color = PlatformGame,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Credential fields ─────────────────────────────────────────────
            SectionHeader(title = "CREDENTIALS")

            CredentialRow(
                label    = "Username / Email",
                value    = e.username,
                icon     = Icons.Outlined.Person,
                onCopy   = { e.username?.let { copyToClipboard("Username / Email", it) } },
                isCopied = copiedField == "Username / Email"
            )

            // ── Password row (tap to reveal) ──────────────────────────────────
            Surface(
                shape    = ShapeCard,
                color    = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Password", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))

                        // Reveal toggle
                        VaultIconButton(
                            icon = if (passwordVisible) Icons.Outlined.VisibilityOff
                                   else Icons.Outlined.Visibility,
                            onClick = { passwordVisible = !passwordVisible },
                            tint = MaterialTheme.colorScheme.primary
                        )
                        // Copy
                        AnimatedCopyButton(
                            isCopied = copiedField == "Password",
                            onClick  = { copyToClipboard("Password", e.password) }
                        )
                    }

                    AnimatedContent(
                        targetState = passwordVisible,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "password_reveal"
                    ) { visible ->
                        if (visible) {
                            Text(
                                text  = e.password,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text  = "•".repeat(e.password.length.coerceAtMost(16)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Autofill Details ─────────────────────────────────────────────
            if (e.websiteUrl.isNotBlank() || e.appPackageName.isNotBlank()) {
                SectionHeader(title = "AUTOFILL DETAILS")
                if (e.websiteUrl.isNotBlank()) {
                    CredentialRow(
                        label    = "Website URL",
                        value    = e.websiteUrl,
                        icon     = Icons.Outlined.Language,
                        onCopy   = { copyToClipboard("Website URL", e.websiteUrl) },
                        isCopied = copiedField == "Website URL"
                    )
                }
                if (e.appPackageName.isNotBlank()) {
                    CredentialRow(
                        label    = "App Package Name",
                        value    = e.appPackageName,
                        icon     = Icons.Outlined.Android,
                        onCopy   = { copyToClipboard("App Package Name", e.appPackageName) },
                        isCopied = copiedField == "App Package Name"
                    )
                }
            }

            // ── Game details ──────────────────────────────────────────────────
            e.gameAccount?.let { game ->
                SectionHeader(title = "GAME DETAILS")
                Surface(shape = ShapeCard, color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GameDetailRow("Game Name", game.gameName)
                        game.inGameId?.let { GameDetailRow("In-Game ID", it) }
                        game.description?.let { GameDetailRow("Description", it) }
                    }
                }
            }

            // ── Timestamps ────────────────────────────────────────────────────
            SectionHeader(title = "INFO")
            Surface(shape = ShapeCard, color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow("Created",  formatTimestamp(e.createdAt))
                    InfoRow("Updated",  formatTimestamp(e.updatedAt))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(Icons.Outlined.DeleteOutline, null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            },
            title = { Text("Delete Account?") },
            text  = {
                Text("This will permanently remove \"${entry?.platformLabel}\" from your vault. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount(entryId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            shape = ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ── Credential row component ──────────────────────────────────────────────────
@Composable
private fun CredentialRow(
    label:    String,
    value:    String?,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    onCopy:   () -> Unit,
    isCopied: Boolean
) {
    if (value == null) return
    Surface(shape = ShapeCard, color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            AnimatedCopyButton(isCopied = isCopied, onClick = onCopy)
        }
    }
}

@Composable
private fun AnimatedCopyButton(isCopied: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        AnimatedContent(
            targetState = isCopied,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "copy_icon"
        ) { copied ->
            Icon(
                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy",
                tint = if (copied) MaterialTheme.colorScheme.secondary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GameDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatTimestamp(ms: Long): String {
    val date = java.util.Date(ms)
    return java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(date)
}
