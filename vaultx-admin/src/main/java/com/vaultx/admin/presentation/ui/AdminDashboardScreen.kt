package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.presentation.AdminViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToPromoCodes: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pendingPayments = payments.filter { it.status == "pending" }
    val premiumUsers = users.filter { it.tier == "premium" }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "VaultX Admin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

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

                item { Spacer(Modifier.height(16.dp)) }

                item {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    AdminActionCard(
                        title = "Manage Users",
                        subtitle = "View, ban, or grant premium to users manually",
                        icon = Icons.Outlined.People,
                        onClick = onNavigateToUsers
                    )
                }

                item {
                    AdminActionCard(
                        title = "Payment Approvals",
                        subtitle = "${pendingPayments.size} pending premium upgrade requests",
                        icon = Icons.Outlined.Payments,
                        onClick = onNavigateToPayments,
                        badgeCount = pendingPayments.size
                    )
                }

                item {
                    AdminActionCard(
                        title = "App Configuration",
                        subtitle = "Manage versions and global announcements",
                        icon = Icons.Outlined.SettingsSuggest,
                        onClick = onNavigateToConfig
                    )
                }

                item {
                    AdminActionCard(
                        title = "App Updates",
                        subtitle = "Publish new APKs and force updates",
                        icon = Icons.Outlined.CloudUpload,
                        onClick = onNavigateToUpdates
                    )
                }

                item {
                    AdminActionCard(
                        title = "Manage Plans",
                        subtitle = "Create and edit dynamic premium subscription plans",
                        icon = Icons.Outlined.List,
                        onClick = onNavigateToPlans
                    )
                }

                item {
                    AdminActionCard(
                        title = "Promo Codes",
                        subtitle = "Create and manage promotional discounts and free plans",
                        icon = Icons.Outlined.LocalOffer,
                        onClick = onNavigateToPromoCodes
                    )
                }
            }
        }
    }
}

@Composable
fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp).size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (badgeCount > 0) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


