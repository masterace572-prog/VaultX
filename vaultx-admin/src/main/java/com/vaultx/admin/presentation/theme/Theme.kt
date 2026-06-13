package com.vaultx.admin.presentation.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
// VaultX Theme
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryVariant,
    onPrimaryContainer   = DarkOnPrimary,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = Color(0xFF1E3A2F),
    onSecondaryContainer = DarkSecondary,
    tertiary             = DarkBadgePremium,
    onTertiary           = Color(0xFF12100A),
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    surfaceContainer     = DarkSurfaceContainer,
    outline              = DarkOutline,
    outlineVariant       = DarkOutlineFocused,
    error                = DarkError,
    onError              = DarkOnError,
)

private val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = Color(0xFFDEDEFF),
    onPrimaryContainer   = LightPrimary,
    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = Color(0xFFD0F0E0),
    onSecondaryContainer = LightSecondary,
    tertiary             = LightBadgePremium,
    onTertiary           = Color(0xFFFFFFFF),
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    surfaceContainer     = LightSurfaceContainer,
    outline              = LightOutline,
    outlineVariant       = LightOutlineFocused,
    error                = LightError,
    onError              = LightOnError,
)

// ── CompositionLocal for theme override ───────────────────────────────────────
// This allows any screen to call VaultXTheme.darkMode to read the current mode
object VaultXTheme {
    val colorScheme: ColorScheme
        @Composable get() = MaterialTheme.colorScheme
    val typography: Typography
        @Composable get() = MaterialTheme.typography
    val shapes: Shapes
        @Composable get() = MaterialTheme.shapes
}

// Composition local to allow runtime dark mode toggle
val LocalDarkMode = compositionLocalOf { false }

@Composable
fun VaultXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // If overridden by user setting, use that; otherwise use system
    userDarkModeOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val effectiveDarkMode = userDarkModeOverride ?: darkTheme

    // Smooth crossfade animation on theme toggle
    val animatedPrimary by animateColorAsState(
        targetValue = if (effectiveDarkMode) DarkPrimary else LightPrimary,
        animationSpec = tween(durationMillis = 400),
        label = "primary_color"
    )
    val animatedBackground by animateColorAsState(
        targetValue = if (effectiveDarkMode) DarkBackground else LightBackground,
        animationSpec = tween(durationMillis = 400),
        label = "background_color"
    )
    val animatedSurface by animateColorAsState(
        targetValue = if (effectiveDarkMode) DarkSurface else LightSurface,
        animationSpec = tween(durationMillis = 400),
        label = "surface_color"
    )

    val colorScheme = if (effectiveDarkMode) {
        DarkColorScheme.copy(
            primary = animatedPrimary,
            background = animatedBackground,
            surface = animatedSurface
        )
    } else {
        LightColorScheme.copy(
            primary = animatedPrimary,
            background = animatedBackground,
            surface = animatedSurface
        )
    }

    // Edge-to-edge: transparent status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !effectiveDarkMode
                isAppearanceLightNavigationBars = !effectiveDarkMode
            }
        }
    }

    CompositionLocalProvider(LocalDarkMode provides effectiveDarkMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = VaultXTypography,
            shapes      = VaultXShapes,
            content     = content
        )
    }
}

