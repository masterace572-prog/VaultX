package com.vaultx.user.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaultx.user.data.model.AccountEntry
import com.vaultx.user.data.model.EntryType
import com.vaultx.user.presentation.theme.ShapeCard

data class CommandPaletteAction(
    val command: String,
    val description: String,
    val icon: ImageVector,
    val execute: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPalette(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    accounts: List<AccountEntry>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onLock: () -> Unit,
    onBackup: () -> Unit
) {
    if (!isOpen) return

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val commands = remember {
        listOf(
            CommandPaletteAction("/add", "Add new vault entry", Icons.Outlined.Add) {
                onDismiss()
                onNavigateToAdd()
            },
            CommandPaletteAction("/lock", "Lock vault immediately", Icons.Outlined.Lock) {
                onDismiss()
                onLock()
            },
            CommandPaletteAction("/backup", "Sync vault database to cloud", Icons.Outlined.CloudUpload) {
                onDismiss()
                onBackup()
            },
            CommandPaletteAction("/settings", "Open security settings", Icons.Outlined.Settings) {
                onDismiss()
                onNavigateToSettings()
            },
            CommandPaletteAction("/premium", "Upgrade to Premium tier", Icons.Outlined.WorkspacePremium) {
                onDismiss()
                onNavigateToPremium()
            }
        )
    }

    val filteredCommands = remember(query) {
        if (query.startsWith("/")) {
            commands.filter { it.command.startsWith(query, ignoreCase = true) }
        } else {
            commands
        }
    }

    val filteredAccounts = remember(query, accounts) {
        if (query.isBlank() || query.startsWith("/")) {
            emptyList()
        } else {
            accounts.filter {
                it.platformLabel.contains(query, ignoreCase = true) ||
                (it.username?.contains(query, ignoreCase = true) ?: false) ||
                (it.email?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding()
                    .clickable(enabled = false) {}, // prevent closing on inner click
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Search Input
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search accounts or type / for commands...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Outlined.Terminal, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Commands section
                        if (query.isEmpty() || query.startsWith("/")) {
                            item {
                                Text(
                                    text = "QUICK COMMANDS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                            items(filteredCommands) { cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { cmd.execute() }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = cmd.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cmd.command,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = cmd.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.SubdirectoryArrowLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Accounts section
                        if (filteredAccounts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "SEARCH RESULTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                            items(filteredAccounts) { account ->
                                val icon = when (account.entryType) {
                                    EntryType.LOGIN -> Icons.Outlined.Lock
                                    EntryType.GAME -> Icons.Outlined.SportsEsports
                                    EntryType.NOTE -> Icons.Outlined.Description
                                    EntryType.CARD -> Icons.Outlined.CreditCard
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onNavigateToDetail(account.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.platformLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val subtitle = when (account.entryType) {
                                            EntryType.LOGIN -> account.username ?: account.email ?: "No identifier"
                                            EntryType.CARD -> account.paymentCard?.cardNumber?.takeLast(4)?.let { "•••• $it" } ?: "Payment Card"
                                            EntryType.NOTE -> "Secure Note"
                                            EntryType.GAME -> account.gameAccount?.gameName ?: "Game Account"
                                        }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else if (query.isNotEmpty() && !query.startsWith("/") && filteredAccounts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching accounts found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
