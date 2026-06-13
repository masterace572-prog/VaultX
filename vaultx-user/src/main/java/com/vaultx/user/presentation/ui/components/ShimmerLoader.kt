package com.vaultx.user.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultx.user.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer / Skeleton Loading
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shimmer effect modifier — apply to any Box/Surface placeholder.
 * Uses an infinite animated gradient sweep to mimic content loading.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val isDark = LocalDarkMode.current
    val baseColor      = if (isDark) ShimmerDarkBase      else ShimmerLightBase
    val highlightColor = if (isDark) ShimmerDarkHighlight else ShimmerLightHighlight

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start  = Offset(translateAnim - 300f, 0f),
            end    = Offset(translateAnim, 0f)
        )
    )
}

// ── Account Card Skeleton ──────────────────────────────────────────────────────
@Composable
fun AccountCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .shimmerEffect()
                .then(Modifier.background(Color.Transparent, ShapeCard))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
                    .shimmerEffect()
            )
            // Subtitle line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(12.dp)
                    .shimmerEffect()
            )
        }

        // Trailing chevron placeholder
        Box(
            modifier = Modifier
                .size(20.dp)
                .shimmerEffect()
        )
    }
}

// ── Full list of skeleton cards ───────────────────────────────────────────────
@Composable
fun AccountListSkeleton(count: Int = 6) {
    Column {
        repeat(count) {
            AccountCardSkeleton()
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ── Text skeleton (for announcement banner etc.) ──────────────────────────────
@Composable
fun TextSkeleton(
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.7f,
    height: androidx.compose.ui.unit.Dp = 14.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .shimmerEffect()
    )
}
