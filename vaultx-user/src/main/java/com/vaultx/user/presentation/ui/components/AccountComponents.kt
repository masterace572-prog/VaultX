package com.vaultx.user.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import com.vaultx.user.data.model.PlatformType
import com.vaultx.user.presentation.theme.*

import com.vaultx.user.R

// ─────────────────────────────────────────────────────────────────────────────
// Platform icon + color mapping
// ─────────────────────────────────────────────────────────────────────────────

data class PlatformVisual(
    val icon: ImageVector,
    val color: Color
)

fun platformVisual(type: PlatformType): PlatformVisual = when (type) {
    PlatformType.INSTAGRAM -> PlatformVisual(Icons.Outlined.PhotoCamera,  PlatformInstagram)
    PlatformType.TWITTER   -> PlatformVisual(Icons.Outlined.Tag,           PlatformTwitterX)
    PlatformType.FACEBOOK  -> PlatformVisual(Icons.Outlined.People,        PlatformFacebook)
    PlatformType.GOOGLE    -> PlatformVisual(Icons.Outlined.AlternateEmail, PlatformGoogle)
    PlatformType.DISCORD   -> PlatformVisual(Icons.Outlined.ChatBubbleOutline, Color(0xFF5865F2))
    PlatformType.GAME      -> PlatformVisual(Icons.Outlined.SportsEsports, PlatformGame)
    PlatformType.CUSTOM    -> PlatformVisual(Icons.Outlined.Key,            PlatformCustom)
}

fun platformIconRes(type: PlatformType): Int? = when (type) {
    PlatformType.INSTAGRAM -> R.drawable.ic_platform_instagram
    PlatformType.TWITTER   -> R.drawable.ic_platform_twitter
    PlatformType.FACEBOOK  -> R.drawable.ic_platform_facebook
    PlatformType.GOOGLE    -> R.drawable.ic_platform_google
    PlatformType.DISCORD   -> R.drawable.ic_platform_discord
    else -> null
}

// ── Platform icon avatar ──────────────────────────────────────────────────────
@Composable
fun PlatformIcon(
    type: PlatformType,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val visual = platformVisual(type)
    val customRes = platformIconRes(type)

    Surface(
        modifier = modifier.size(size),
        shape    = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), // Premium Apple-like squircle
        color    = visual.color.copy(alpha = 0.15f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (customRes != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = customRes),
                    contentDescription = type.displayName,
                    tint = Color.Unspecified, // Keep the real vector colors
                    modifier = Modifier.size(size * 0.55f)
                )
            } else {
                Icon(
                    imageVector        = visual.icon,
                    contentDescription = type.displayName,
                    tint               = visual.color,
                    modifier           = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

// ── Account list card ─────────────────────────────────────────────────────────
@Composable
fun AccountCard(
    platformType:  PlatformType,
    platformLabel: String,
    subtitle:      String,   // username or email (masked)
    isFavorite:    Boolean = false,
    isGameAccount: Boolean = false,
    onClick:       () -> Unit,
    modifier:      Modifier = Modifier
) {
    val visual = platformVisual(platformType)
    val hue = (kotlin.math.abs(platformLabel.hashCode().toLong()) % 360).toFloat()
    val glowColor = Color.hsv(hue, 0.6f, 0.5f)

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(24.dp)
                .background(glowColor.copy(alpha = 0.3f), ShapeCard)
        )
        Surface(
            onClick       = onClick,
            modifier      = Modifier.fillMaxWidth(),
            shape         = ShapeCard,
            color         = MaterialTheme.colorScheme.surface,
            border        = androidx.compose.foundation.BorderStroke(1.5.dp, visual.color.copy(alpha = 0.4f)),
            tonalElevation = 0.dp,
        ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlatformIcon(type = platformType)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text  = platformLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (isGameAccount) {
                        Surface(
                            shape = ShapeBadge,
                            color = PlatformGame.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text     = "GAME",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = PlatformGame,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = "Favourite",
                        tint = DarkBadgePremium,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ── Divider between cards ─────────────────────────────────────────────────────
@Composable
fun VaultDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier  = modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    )
}

// ── Tier badge (Free / Premium) ───────────────────────────────────────────────
@Composable
fun TierBadge(isPremium: Boolean, planName: String? = null, daysLeft: Int? = null, modifier: Modifier = Modifier) {
    val defaultName = planName?.takeIf { it.isNotBlank() } ?: "PRO"
    val label = when {
        !isPremium       -> "FREE"
        daysLeft != null -> "$defaultName · ${daysLeft}d left"
        else             -> defaultName
    }
    val color = if (isPremium) DarkBadgePremium else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Surface(
        modifier = modifier,
        shape    = ShapeBadge,
        color    = color.copy(alpha = 0.15f)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
fun EmptyVaultState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(24.dp)
                    .size(40.dp)
            )
        }
        Text(
            text  = "Your vault is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text  = "Tap the + button to save your first account",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
