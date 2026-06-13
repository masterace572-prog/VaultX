package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.data.model.User
import com.vaultx.admin.presentation.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.vaultx.admin.presentation.ui.components.VaultTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUsers = users.filter { 
        it.email.contains(searchQuery, ignoreCase = true) || 
        it.uid.contains(searchQuery, ignoreCase = true) ||
        (it.name?.contains(searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Users", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VaultTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                placeholder = "Search by name, email or UID",
                leadingIcon = Icons.Outlined.Search,
                singleLine = true
            )

            if (isLoading && users.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredUsers, key = { it.uid }) { user ->
                        UserAdminCard(
                            user = user,
                            onGrantPremium = { days -> viewModel.grantManualPremium(user.uid, days) },
                            onRevokePremium = { viewModel.revokePremium(user.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserAdminCard(
    user: User,
    onGrantPremium: (Int) -> Unit,
    onRevokePremium: () -> Unit
) {
    var showActionMenu by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Initial
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.name?.take(1) ?: user.email.take(1)).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name?.ifBlank { null } ?: "No Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email.ifBlank { "No Email" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "UID: ${user.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Box {
                    IconButton(onClick = { showActionMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Grant 30 Days Premium") },
                            onClick = { onGrantPremium(30); showActionMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.WorkspacePremium, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Grant 365 Days Premium") },
                            onClick = { onGrantPremium(365); showActionMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.WorkspacePremium, null) }
                        )
                        if (user.tier == "premium") {
                            DropdownMenuItem(
                                text = { Text("Revoke Premium", color = MaterialTheme.colorScheme.error) },
                                onClick = { onRevokePremium(); showActionMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Cancel, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (user.tier == "premium") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = user.tier.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.tier == "premium") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (user.tier == "premium" && user.premiumExpiry != null) {
                    val expiryDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(user.premiumExpiry))
                    Text(
                        text = "Expires: $expiryDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
