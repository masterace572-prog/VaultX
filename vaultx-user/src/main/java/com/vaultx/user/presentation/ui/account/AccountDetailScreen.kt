package com.vaultx.user.presentation.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
            // ── Platform header (Un-carded Apple Style) ──────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformIcon(
                    type = e.platformType,
                    size = 72.dp,
                    modifier = Modifier.shadow(8.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = e.platformLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).em
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = e.platformType.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                if (e.isFavorite || e.gameAccount != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (e.isFavorite) {
                            Surface(
                                shape = ShapeBadge,
                                color = DarkBadgePremium.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "★ Favourite",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkBadgePremium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (e.gameAccount != null) {
                            Surface(
                                shape = ShapeBadge,
                                color = PlatformGame.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "GAME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PlatformGame,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Unified Credentials Card ──────────────────────────────────────
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val hasUsername = !e.username.isNullOrBlank()
                    
                    if (hasUsername) {
                        CredentialRowItem(
                            label = "Username / Email",
                            value = e.username,
                            icon = Icons.Outlined.Person,
                            onCopy = { copyToClipboard("Username / Email", e.username!!) },
                            isCopied = copiedField == "Username / Email"
                        )
                    }

                    if (hasUsername) {
                        VaultDivider()
                    }

                    PasswordRowItem(
                        password = e.password,
                        passwordVisible = passwordVisible,
                        onVisibilityToggle = { passwordVisible = !passwordVisible },
                        onCopy = { copyToClipboard("Password", e.password) },
                        isCopied = copiedField == "Password"
                    )

                    if (e.websiteUrl.isNotBlank()) {
                        VaultDivider()
                        CredentialRowItem(
                            label = "Website URL",
                            value = e.websiteUrl,
                            icon = Icons.Outlined.Language,
                            onCopy = { copyToClipboard("Website URL", e.websiteUrl) },
                            isCopied = copiedField == "Website URL"
                        )
                    }

                    if (e.appPackageName.isNotBlank()) {
                        VaultDivider()
                        CredentialRowItem(
                            label = "App Package Name",
                            value = e.appPackageName,
                            icon = Icons.Outlined.Android,
                            onCopy = { copyToClipboard("App Package Name", e.appPackageName) },
                            isCopied = copiedField == "App Package Name"
                        )
                    }
                }
            }

            // ── Game details ──────────────────────────────────────────────────
            e.gameAccount?.let { game ->
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "GAME DETAILS")
                Surface(
                    shape = ShapeCard,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        GameDetailRowItem("Game Name", game.gameName, Icons.Outlined.SportsEsports)
                        game.inGameId?.takeIf { it.isNotBlank() }?.let { id ->
                            VaultDivider()
                            GameDetailRowItem("In-Game ID", id, Icons.Outlined.Badge)
                        }
                        game.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            VaultDivider()
                            GameDetailRowItem("Description", desc, Icons.Outlined.Description)
                        }
                    }
                }
            }

            // ── Metadata Footer ───────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Created: ${formatTimestamp(e.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Last Updated: ${formatTimestamp(e.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(24.dp))
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

// ── Credential row item component ─────────────────────────────────────────────
@Composable
private fun CredentialRowItem(
    label:    String,
    value:    String?,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    onCopy:   () -> Unit,
    isCopied: Boolean
) {
    if (value == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        AnimatedCopyButton(isCopied = isCopied, onClick = onCopy)
    }
}

@Composable
private fun PasswordRowItem(
    password:           String,
    passwordVisible:    Boolean,
    onVisibilityToggle: () -> Unit,
    onCopy:             () -> Unit,
    isCopied:           Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.Lock, null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Password",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            AnimatedContent(
                targetState = passwordVisible,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "password_reveal"
            ) { visible ->
                if (visible) {
                    Text(
                        text  = password,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text  = "•".repeat(password.length.coerceAtMost(16)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedCopyButton(isCopied = isCopied, onClick = onCopy)
        }
    }
}

@Composable
private fun GameDetailRowItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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

private fun formatTimestamp(ms: Long): String {
    val date = java.util.Date(ms)
    return java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(date)
}
