package com.vaultx.user.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.AppConfig
import com.vaultx.user.data.model.PlatformType
import com.vaultx.user.data.model.UserTier
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.composed
import com.vaultx.user.presentation.viewmodel.AccountViewModel
import com.vaultx.user.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — dynamic header, announcement animation, account list
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAdd:      () -> Unit,
    onNavigateToDetail:   (String) -> Unit,
    onNavigateToSearch:   () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium:  () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel(),
    homeViewModel:    HomeViewModel    = hiltViewModel(),
) {
    val accounts     by accountViewModel.accounts.collectAsState()
    val userProfile  by homeViewModel.userProfile.collectAsState()
    val appConfig    by homeViewModel.appConfig.collectAsState()
    val isLoading    by homeViewModel.isLoading.collectAsState()
    val isRefreshing by homeViewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                userProfile        = userProfile,
                appConfig          = appConfig,
                onSettingsClick    = onNavigateToSettings,
                onPremiumClick     = onNavigateToPremium,
                onSearchClick      = onNavigateToSearch,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onNavigateToAdd,
                containerColor    = MaterialTheme.colorScheme.primary,
                contentColor      = MaterialTheme.colorScheme.onPrimary,
                shape             = ShapeFull,
                elevation         = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add account")
            }
        }
    ) { padding ->
        if (isLoading && accounts.isEmpty()) {
            // Shimmer skeleton while loading
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(6) { AccountCardSkeleton() }
            }
        } else if (accounts.isEmpty()) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { homeViewModel.refresh() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                Box(
                    modifier           = Modifier.fillMaxSize(),
                    contentAlignment   = Alignment.Center
                ) {
                    EmptyVaultState()
                }
            }
        } else {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { homeViewModel.refresh() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ── Premium Upgrade Banner for Free Users ──
                    if (userProfile?.isPremium == false) {
                        item {
                            PremiumUpgradeBanner(onClick = onNavigateToPremium)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Group by platform type
                    val grouped = accounts.groupBy { it.platformType }
                    grouped.forEach { (type, entries) ->
                        item(key = "header_${type.key}") {
                            SectionHeader(title = type.displayName.uppercase())
                        }
                        items(entries, key = { it.id }) { entry ->
                            AccountCard(
                                platformType  = entry.platformType,
                                platformLabel = entry.platformLabel,
                                subtitle      = entry.username
                                    ?: entry.email?.maskEmail()
                                    ?: "No identifier",
                                isFavorite    = entry.isFavorite,
                                isGameAccount = entry.gameAccount != null,
                                onClick       = { onNavigateToDetail(entry.id) }
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        item(key = "spacer_${type.key}") { Spacer(Modifier.height(8.dp)) }
                    }
                    // Bottom padding for FAB
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

// ── Top bar with announcement crossfade animation ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    userProfile:     UserProfile?,
    appConfig:       AppConfig?,
    onSettingsClick: () -> Unit,
    onPremiumClick:  () -> Unit,
    onSearchClick:   () -> Unit,
) {
    // Announcement animation state
    var showAnnouncement by remember { mutableStateOf(false) }
    val hasAnnouncement = appConfig?.announcementActive == true &&
                          appConfig.announcementText.isNotBlank()

    LaunchedEffect(hasAnnouncement) {
        if (hasAnnouncement) {
            delay(800)              // Let screen settle
            showAnnouncement = true
            delay(5_000)           // Hold announcement for 5s (as specified)
            showAnnouncement = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // ── Welcome / Announcement crossfade ──────────────────────────────
            AnimatedContent(
                targetState  = showAnnouncement,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn(tween(500)))
                        .togetherWith(slideOutVertically { -it } + fadeOut(tween(400)))
                },
                modifier = Modifier.weight(1f),
                label    = "header_content"
            ) { isAnnouncement ->
                if (isAnnouncement && appConfig != null) {
                    WelcomeHeader(
                        title = "Announcement",
                        titleColor = MaterialTheme.colorScheme.primary,
                        subtitle = appConfig.announcementText,
                        isPremium = userProfile?.isPremium ?: false,
                        daysLeft = userProfile?.daysLeft,
                        onPremiumClick = onPremiumClick
                    )
                } else {
                    WelcomeHeader(
                        title = "Welcome back,",
                        titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        subtitle = userProfile?.displayName ?: "there",
                        isPremium = userProfile?.isPremium ?: false,
                        daysLeft = userProfile?.daysLeft,
                        onPremiumClick = onPremiumClick
                    )
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Outlined.Search,
                        "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Outlined.Settings,
                        "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun WelcomeHeader(
    title:          String,
    titleColor:     androidx.compose.ui.graphics.Color,
    subtitle:       String,
    isPremium:      Boolean,
    daysLeft:       Int?,
    onPremiumClick: () -> Unit
) {
    var showPlanDialog by remember { mutableStateOf(false) }

    if (showPlanDialog) {
        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("Plan Details") },
            text = { Text("Active plan: ${if (isPremium) "Premium" else "Free"}\nDays left: ${daysLeft ?: 0}") },
            confirmButton = {
                TextButton(onClick = { showPlanDialog = false }) { Text("OK") }
            },
            dismissButton = {
                if (!isPremium) {
                    TextButton(onClick = { showPlanDialog = false; onPremiumClick() }) { Text("Upgrade") }
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.bodySmall,
            color    = titleColor
        )
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text     = subtitle,
                style    = MaterialTheme.typography.titleLarge,
                color    = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (title != "Announcement") {
                TierBadge(
                    isPremium  = isPremium,
                    daysLeft   = daysLeft,
                    modifier   = Modifier.clickableNoRipple { showPlanDialog = true }
                )
            }
        }
    }
}

// ── Premium Upgrade Banner ────────────────────────────────────────────────────
@Composable
private fun PremiumUpgradeBanner(onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        shape    = ShapeCard,
        color    = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text  = "Unlock unlimited accounts, cloud sync, and more exclusive features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── Helper: Modifier.clickableNoRipple ────────────────────────────────────────
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication           = null,
        interactionSource    = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        onClick              = onClick
    )
}

// ── User profile data class (for UI layer only) ───────────────────────────────
data class UserProfile(
    val displayName: String,
    val email: String,
    val isPremium: Boolean,
    val daysLeft: Int?
)

// ── Extension ─────────────────────────────────────────────────────────────────
private fun String.maskEmail(): String {
    val parts = split("@")
    if (parts.size != 2) return this
    val user   = parts[0]
    val domain = parts[1]
    val masked = user.take(2) + "•".repeat((user.length - 2).coerceAtLeast(1))
    return "$masked@$domain"
}
