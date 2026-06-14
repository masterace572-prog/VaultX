package com.vaultx.user.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultx.user.presentation.theme.LocalVaultUIEngine
import com.vaultx.user.presentation.theme.NavStyle

enum class VaultTab(val route: String, val title: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    VAULT("vault", "Vault", Icons.Outlined.Lock),
    SECURITY("security", "Security", Icons.Outlined.Shield),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultXShell(
    currentRoute: String,
    onTabSelected: (VaultTab) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp in 600..839
    val isDesktop = screenWidthDp >= 840
    val engine = LocalVaultUIEngine.current

    val activeTab = remember(currentRoute) {
        VaultTab.values().firstOrNull { currentRoute.startsWith(it.route) } ?: VaultTab.DASHBOARD
    }

    if (isDesktop) {
        // Desktop Layout: Permanent Navigation Drawer
        Row(modifier = Modifier.fillMaxSize()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(240.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VAULT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    letterSpacing = 1.sp,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "X",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    letterSpacing = 1.sp,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        VaultTab.values().forEach { tab ->
                            NavigationDrawerItem(
                                label = { Text(tab.title, style = MaterialTheme.typography.labelLarge) },
                                selected = activeTab == tab,
                                onClick = { onTabSelected(tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content(PaddingValues(0.dp))
                }
            }
        }
    } else if (isTablet || engine.navStyle == NavStyle.NAV_RAIL) {
        // Navigation Rail Layout
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Spacer(Modifier.height(32.dp))
                VaultTab.values().forEach { tab ->
                    NavigationRailItem(
                        selected = activeTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                content(PaddingValues(0.dp))
            }
        }
    } else {
        // Phone Layout: Bottom Navigation (Standard MD3)
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp // Standard MD3 elevation for Nav bars
                ) {
                    VaultTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { onTabSelected(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                content(PaddingValues(0.dp))
            }
        }
    }
}
