package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.presentation.AdminViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.vaultx.admin.data.model.User
import com.vaultx.admin.presentation.ui.components.VaultTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToPayments: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToPromoCodes: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val users by viewModel.users.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUsers = users.filter { 
        it.email.contains(searchQuery, ignoreCase = true) || 
        it.uid.contains(searchQuery, ignoreCase = true) ||
        (it.name?.contains(searchQuery, ignoreCase = true) == true)
    }
    val pendingPayments = payments.filter { it.status == "pending" }
    val premiumUsers = users.filter { it.tier == "premium" }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Quick Actions at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = Icons.Outlined.Payments,
                    label = "Payments",
                    badgeCount = pendingPayments.size,
                    onClick = onNavigateToPayments
                )
                QuickActionButton(
                    icon = Icons.Outlined.SettingsSuggest,
                    label = "Config",
                    onClick = onNavigateToConfig
                )
                QuickActionButton(
                    icon = Icons.Outlined.CloudUpload,
                    label = "Updates",
                    onClick = onNavigateToUpdates
                )
                QuickActionButton(
                    icon = Icons.Outlined.List,
                    label = "Plans",
                    onClick = onNavigateToPlans
                )
                QuickActionButton(
                    icon = Icons.Outlined.LocalOffer,
                    label = "Promos",
                    onClick = onNavigateToPromoCodes
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    "Admin Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            VaultTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search Users",
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
                    item {
                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard("Total Users", users.size.toString(), Modifier.weight(1f))
                            StatCard("Premium Users", premiumUsers.size.toString(), Modifier.weight(1f))
                        }
                    }

                    items(filteredUsers, key = { it.uid }) { user ->
                        UserAdminCard(
                            user = user,
                            plans = plans,
                            onGrantPremium = { days -> 
                                viewModel.grantManualPremium(user.uid, days) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRevokePremium = { 
                                viewModel.revokePremium(user.uid) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            }
            if (badgeCount > 0) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                ) {
                    Text(
                        badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun UserAdminCard(
    user: User,
    plans: List<com.vaultx.admin.data.model.Plan>,
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
                        // Dynamically generate grant premium options from plans
                        plans.filter { it.isActive }.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text("Grant ${plan.name} (${plan.durationDays} Days)") },
                                onClick = { onGrantPremium(plan.durationDays); showActionMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.WorkspacePremium, null) }
                            )
                        }
                        
                        if (plans.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Grant 30 Days Premium") },
                                onClick = { onGrantPremium(30); showActionMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.WorkspacePremium, null) }
                            )
                        }

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
