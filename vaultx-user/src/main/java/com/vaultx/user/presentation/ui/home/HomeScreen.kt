package com.vaultx.user.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.AccountEntry
import com.vaultx.user.data.model.AppConfig
import com.vaultx.user.data.model.EntryType
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountViewModel
import com.vaultx.user.presentation.viewmodel.HomeViewModel
import com.vaultx.user.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAdd:      () -> Unit,
    onNavigateToDetail:   (String) -> Unit,
    onNavigateToSearch:   () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium:  () -> Unit,
    onNavigateToVault:    () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    accountViewModel: AccountViewModel = hiltViewModel(),
    homeViewModel:    HomeViewModel    = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val accountsState = accountViewModel.accounts.collectAsState()
    val accounts = accountsState.value
    val userProfileState = homeViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value
    val appConfigState = homeViewModel.appConfig.collectAsState()
    val appConfig = appConfigState.value
    val isLoadingState = homeViewModel.isLoading.collectAsState()
    val isLoading = isLoadingState.value
    val isRefreshingState = homeViewModel.isLoading.collectAsState()
    val isRefreshing = isRefreshingState.value

    // Sync state from settings view model
    val cloudSyncEnabledState = settingsViewModel.cloudSyncEnabled.collectAsState()
    val cloudSyncEnabled = cloudSyncEnabledState.value
    val lastBackupTimeState = settingsViewModel.lastBackupTime.collectAsState()
    val lastBackupTime = lastBackupTimeState.value
    val isSyncingState = settingsViewModel.isSyncing.collectAsState()
    val isSyncing = isSyncingState.value

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    var showCommandPalette by remember { mutableStateOf(false) }
    var showPremiumDetailsDialog by remember { mutableStateOf(false) }

    // Computations for Bento Grid counts
    val loginCount = remember(accounts) { accounts.count { it.entryType == EntryType.LOGIN } }
    val gameCount = remember(accounts) { accounts.count { it.entryType == EntryType.GAME } }
    val noteCount = remember(accounts) { accounts.count { it.entryType == EntryType.NOTE } }
    val cardCount = remember(accounts) { accounts.count { it.entryType == EntryType.CARD } }

    // Live local security score evaluation
    val securityScore = remember(accounts) {
        val logins = accounts.filter { it.password.isNotEmpty() }
        if (logins.isEmpty()) 100 else {
            var weak = 0
            var reused = 0
            val passCounts = logins.groupBy { it.password }.mapValues { it.value.size }
            logins.forEach { entry ->
                val isWeak = entry.password.length < 8 || entry.password.all { it.isLowerCase() } || entry.password.all { it.isDigit() }
                if (isWeak) weak++
                if ((passCounts[entry.password] ?: 1) > 1) reused++
            }
            val weakRatio = weak.toFloat() / logins.size
            val reusedRatio = reused.toFloat() / logins.size
            (100 - (weakRatio * 40) - (reusedRatio * 40)).toInt().coerceIn(15, 100)
        }
    }

    val recentEntries = remember(accounts) {
        accounts.sortedByDescending { it.updatedAt }.take(3)
    }

    // TopBar announcement settles
    var showAnnouncement by remember { mutableStateOf(false) }
    val hasAnnouncement = appConfig?.announcementActive == true && appConfig.announcementText.isNotBlank()

    LaunchedEffect(hasAnnouncement) {
        if (hasAnnouncement) {
            delay(800)
            showAnnouncement = true
            delay(5000)
            showAnnouncement = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            homeViewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val engine = LocalVaultUIEngine.current
                val densityPadding = when (engine.uiDensity) {
                    UIDensity.COMPACT -> 8.dp
                    UIDensity.COMFORTABLE -> 16.dp
                    UIDensity.SPACIOUS -> 24.dp
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = densityPadding, vertical = densityPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = showAnnouncement,
                            transitionSpec = {
                                (slideInVertically { it } + fadeIn(tween(500)))
                                    .togetherWith(slideOutVertically { -it } + fadeOut(tween(400)))
                            },
                            modifier = Modifier.weight(1f),
                            label = "header_content"
                        ) { isAnnouncement ->
                            if (isAnnouncement && appConfig != null) {
                                WelcomeHeader(
                                    title = "Announcement",
                                    titleColor = MaterialTheme.colorScheme.primary,
                                    subtitle = appConfig.announcementText,
                                    userProfile = userProfile,
                                    onPremiumClick = { showPremiumDetailsDialog = true }
                                )
                            } else {
                                WelcomeHeader(
                                    title = "Welcome back,",
                                    titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    subtitle = userProfile?.displayName ?: "there",
                                    userProfile = userProfile,
                                    onPremiumClick = { showPremiumDetailsDialog = true }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { showCommandPalette = true }) {
                                Icon(Icons.Outlined.Search, "Command Palette", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Outlined.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAdd,
                    expanded = listState.isScrollingUp(),
                    icon = { Icon(Icons.Outlined.Add, contentDescription = "Add account") },
                    text = { Text("Add Item") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = ShapeFull,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                )
            }
        ) { padding ->
            val engine = LocalVaultUIEngine.current
            val densityPadding = when (engine.uiDensity) {
                UIDensity.COMPACT -> 8.dp
                UIDensity.COMFORTABLE -> 16.dp
                UIDensity.SPACIOUS -> 24.dp
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = densityPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Premium Banner ──
                if (userProfile?.isPremium == false) {
                    item {
                        PremiumUpgradeBanner(onClick = onNavigateToPremium)
                    }
                }

                // ── Dashboard Grid/List Section ──
                item {
                    if (engine.dashboardLayout == DashboardLayoutOption.LIST) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Password Vault
                            BentoCard(onClick = onNavigateToVault, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Password Vault", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("$loginCount accounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            // Security Score
                            BentoCard(onClick = onNavigateToSecurity, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Security Score", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Score: $securityScore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            // Games
                            BentoCard(onClick = onNavigateToVault, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Outlined.SportsEsports, null, tint = PlatformGame, modifier = Modifier.size(28.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Games", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("$gameCount accounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    } else {
                        // Grid Layout (Default / Compact Grid)
                        val isCompactGrid = engine.dashboardLayout == DashboardLayoutOption.COMPACT_GRID
                        val gridSpacing = if (isCompactGrid) 8.dp else 12.dp
                        
                        Column(verticalArrangement = Arrangement.spacedBy(gridSpacing)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                            ) {
                            // Password Vault Card (Large - 2/3)
                            BentoCard(
                                modifier = Modifier.weight(2f),
                                onClick = onNavigateToVault
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "$loginCount",
                                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Password Vault",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Security Score Card (1/3)
                            BentoCard(
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToSecurity
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val scoreColor = when {
                                        securityScore >= 80 -> AccentGreenLight
                                        securityScore >= 50 -> AccentOrangeLight
                                        else -> AccentRedLight
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(48.dp)) {
                                            drawCircle(
                                                color = scoreColor.copy(alpha = 0.12f),
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                            drawArc(
                                                color = scoreColor,
                                                startAngle = -90f,
                                                sweepAngle = (securityScore * 3.6f),
                                                useCenter = false,
                                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                        Text(
                                            text = "$securityScore",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Security",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Middle row: Game accounts + Backup Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BentoCard(
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToVault
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.SportsEsports,
                                        null,
                                        tint = PlatformGame,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "$gameCount",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Games",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Sync Card (Small / Interactive)
                            BentoCard(
                                modifier = Modifier.weight(1f),
                                onClick = { settingsViewModel.syncNow() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                Icons.Outlined.Sync,
                                                null,
                                                tint = if (cloudSyncEnabled) AccentGreenLight else AccentOrangeLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (isSyncing) "Syncing" else if (cloudSyncEnabled) "Cloud Active" else "Local Only",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lastBackupTime ?: "Sync Now",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            }
                        }
                    }
                }

                // ── Recent Activity Section ──
                if (recentEntries.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(title = "RECENT ACTIVITY")
                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    recentEntries.forEachIndexed { index, entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onNavigateToDetail(entry.id) }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            PlatformIcon(type = entry.platformType, size = 38.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.platformLabel,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val desc = when (entry.entryType) {
                                                    EntryType.LOGIN -> entry.username ?: entry.email ?: "Login Details"
                                                    EntryType.GAME -> entry.gameAccount?.gameName ?: "Game Details"
                                                    EntryType.NOTE -> "Secure Note"
                                                    EntryType.CARD -> entry.paymentCard?.cardNumber?.takeLast(4)?.let { "•••• $it" } ?: "Card"
                                                }
                                                Text(
                                                    text = desc,
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
                                        if (index < recentEntries.lastIndex) {
                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Spacing at the bottom
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Command Palette Overlay ──
    CommandPalette(
        isOpen = showCommandPalette,
        onDismiss = { showCommandPalette = false },
        accounts = accounts,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToAdd = onNavigateToAdd,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPremium = onNavigateToPremium,
        onLock = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            settingsViewModel.logout {
                onNavigateToSettings()
            }
        },
        onBackup = {
            settingsViewModel.syncNow()
        }
    )

    // ── Subscription Details Dialog ──
    if (showPremiumDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDetailsDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showPremiumDetailsDialog = false
                    onNavigateToPremium()
                }) {
                    Text("Manage Subscription")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDetailsDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text(if (userProfile?.isPremium == true) "Premium Plan" else "Free Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (userProfile?.isPremium == true) {
                        Text("Active Plan: ${userProfile?.planName ?: "PRO Tier"}")
                        userProfile?.daysLeft?.let {
                            Text("Time remaining: $it days")
                        }
                        Text("You have unlimited entries, cloud backup sync, and full local vulnerability protection.")
                    } else {
                        Text("Plan tier: FREE")
                        Text("Upgrade to PREMIUM to get unlimited accounts storage (free is capped at ${appConfig?.maxFreeAccounts ?: 5} items), cloud backups sync, and full security score scanning.")
                    }
                }
            }
        )
    }
}

// ── Welcome header ──
@Composable
private fun WelcomeHeader(
    title: String,
    titleColor: Color,
    subtitle: String,
    userProfile: UserProfile?,
    onPremiumClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = titleColor
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.02).sp),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (title != "Announcement" && userProfile != null) {
                TierBadge(
                    isPremium = userProfile.isPremium,
                    planName = userProfile.planName,
                    daysLeft = userProfile.daysLeft,
                    modifier = Modifier.clickableNoRipple(onPremiumClick)
                )
            }
        }
    }
}

// ── Custom Bento Card Wrapper using VaultCard ──
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    VaultCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ── Premium Upgrade Banner ──
@Composable
private fun PremiumUpgradeBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ShapeCard,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Unlock unlimited accounts storage, instant sync, and full threats protection.",
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

// ── User Profile representation for UI layer ──
data class UserProfile(
    val displayName: String,
    val email: String,
    val isPremium: Boolean,
    val planName: String?,
    val daysLeft: Int?
)

// ── Extension for scroll aware collapsing FAB ──
@Composable
fun androidx.compose.foundation.lazy.LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

// ── Modifier helper to disable ripple on clicks ──
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication           = null,
        interactionSource    = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        onClick              = onClick
    )
}

