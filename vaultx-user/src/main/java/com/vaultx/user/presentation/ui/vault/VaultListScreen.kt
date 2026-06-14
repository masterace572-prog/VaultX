package com.vaultx.user.presentation.ui.vault

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import com.vaultx.user.presentation.theme.liquidGlass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.AccountEntry
import com.vaultx.user.data.model.EntryType
import com.vaultx.user.data.model.PlatformType
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountViewModel

enum class VaultFilter(val displayName: String, val type: EntryType?) {
    ALL("All", null),
    LOGINS("Logins", EntryType.LOGIN),
    GAMES("Games", EntryType.GAME)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(VaultFilter.ALL) }
    var sortByAlphabetical by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    // Filtered entries in memory
    val filteredEntries = remember(accounts, selectedFilter, searchQuery) {
        accounts.filter { entry ->
            val matchesFilter = selectedFilter.type == null || entry.entryType == selectedFilter.type
            matchesFilter
        }.sortedWith { a, b ->
            if (sortByAlphabetical) a.platformLabel.compareTo(b.platformLabel, ignoreCase = true)
            else b.updatedAt.compareTo(a.updatedAt)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "My Vault",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            sortByAlphabetical = !sortByAlphabetical
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }) {
                            Icon(
                                imageVector = if (sortByAlphabetical) Icons.Outlined.SortByAlpha else Icons.Outlined.Schedule,
                                contentDescription = "Sort Mode",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search vault entries...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultFilter.values().forEach { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            onClick = {
                                selectedFilter = filter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = filter.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No entries found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Try changing your search or filter options",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val engine = LocalVaultUIEngine.current
            val densityPadding = when (engine.uiDensity) {
                UIDensity.COMPACT -> 8.dp
                UIDensity.COMFORTABLE -> 16.dp
                UIDensity.SPACIOUS -> 24.dp
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = densityPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    val index = filteredEntries.indexOf(entry)
                    val animatedOffset = remember { androidx.compose.animation.core.Animatable(50f) }
                    val animatedAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
                    
                    LaunchedEffect(entry.id) {
                        kotlinx.coroutines.delay((index * 40L).coerceAtMost(300L))
                        launch { 
                            animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) 
                        }
                        launch { 
                            animatedAlpha.animateTo(1f, tween(250)) 
                        }
                    }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { false } // Spring bounce back
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 
                                    MaterialTheme.colorScheme.errorContainer 
                                else 
                                    Color.Transparent,
                                label = "bg_color"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, ShapeCard)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = if (color == Color.Transparent) Color.Transparent else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        },
                        modifier = Modifier
                            .animateItemPlacement()
                            .graphicsLayer {
                                translationY = animatedOffset.value
                                alpha = animatedAlpha.value
                            }
                    ) {
                        VaultEntryCard(
                            entry = entry,
                            onClick = { onNavigateToDetail(entry.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun VaultEntryCard(
    entry: AccountEntry,
    onClick: () -> Unit
) {
    val engine = LocalVaultUIEngine.current
    val visual = platformVisual(entry.platformType)
    val hue = (kotlin.math.abs(entry.platformLabel.hashCode().toLong()) % 360).toFloat()
    val glowColor = Color.hsv(hue, 0.6f, 0.5f)

    // Highlight visual icon based on entry type
    val icon = when (entry.entryType) {
        EntryType.LOGIN -> Icons.Outlined.Lock
        EntryType.GAME -> Icons.Outlined.SportsEsports
        EntryType.NOTE -> Icons.Outlined.Description
        EntryType.CARD -> Icons.Outlined.CreditCard
    }

    val typeLabel = when (entry.entryType) {
        EntryType.LOGIN -> "Login"
        EntryType.GAME -> "Game Account"
        EntryType.NOTE -> "Secure Note"
        EntryType.CARD -> "Payment Card"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (engine.blurEffectsEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(20.dp)
                    .background(glowColor.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
            )
        }
        VaultCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Customized avatar circle with Type Icon
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = visual.color.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = typeLabel,
                            tint = visual.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.platformLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (entry.isFavorite) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favorite",
                                tint = DarkBadgePremium,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    val subtitleText = when (entry.entryType) {
                        EntryType.LOGIN -> entry.username ?: entry.email ?: "No username"
                        EntryType.GAME -> entry.gameAccount?.gameName ?: "No game name"
                        EntryType.NOTE -> "Secure Note Content"
                        EntryType.CARD -> entry.paymentCard?.cardNumber?.takeLast(4)?.let { "•••• •••• •••• $it" } ?: "No card number"
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = ShapeBadge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = typeLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        }
    }
}
