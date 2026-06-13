package com.vaultx.user.presentation.theme

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

// High Contrast dark scheme
private val HighContrastDarkScheme = darkColorScheme(
    primary              = Color.White,
    onPrimary            = Color.Black,
    primaryContainer     = Color.White,
    onPrimaryContainer   = Color.Black,
    secondary            = Color.White,
    onSecondary          = Color.Black,
    secondaryContainer   = Color(0xFF1C1C1C),
    onSecondaryContainer = Color.White,
    background           = HighContrastBgDark,
    onBackground         = HighContrastTextDark,
    surface              = HighContrastSurfaceDark,
    onSurface            = HighContrastTextDark,
    surfaceVariant       = Color(0xFF121212),
    onSurfaceVariant     = HighContrastTextDark,
    outline              = HighContrastBorderDark,
    outlineVariant       = HighContrastBorderDark,
    error                = Color.Red,
    onError              = Color.White,
)

// High Contrast light scheme
private val HighContrastLightScheme = lightColorScheme(
    primary              = Color.Black,
    onPrimary            = Color.White,
    primaryContainer     = Color.Black,
    onPrimaryContainer   = Color.White,
    secondary            = Color.Black,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFE5E5E5),
    onSecondaryContainer = Color.Black,
    background           = HighContrastBgLight,
    onBackground         = HighContrastTextLight,
    surface              = HighContrastSurfaceLight,
    onSurface            = HighContrastTextLight,
    surfaceVariant       = Color(0xFFF0F0F0),
    onSurfaceVariant     = HighContrastTextLight,
    outline              = HighContrastBorderLight,
    outlineVariant       = HighContrastBorderLight,
    error                = Color.Red,
    onError              = Color.White,
)

// Helper extension to scale typography
private fun Typography.scale(factor: Float): Typography {
    if (factor == 1.0f) return this
    return Typography(
        displayLarge = displayLarge.copy(fontSize = displayLarge.fontSize * factor, lineHeight = displayLarge.lineHeight * factor),
        displayMedium = displayMedium.copy(fontSize = displayMedium.fontSize * factor, lineHeight = displayMedium.lineHeight * factor),
        displaySmall = displaySmall.copy(fontSize = displaySmall.fontSize * factor, lineHeight = displaySmall.lineHeight * factor),
        headlineLarge = headlineLarge.copy(fontSize = headlineLarge.fontSize * factor, lineHeight = headlineLarge.lineHeight * factor),
        headlineMedium = headlineMedium.copy(fontSize = headlineMedium.fontSize * factor, lineHeight = headlineMedium.lineHeight * factor),
        headlineSmall = headlineSmall.copy(fontSize = headlineSmall.fontSize * factor, lineHeight = headlineSmall.lineHeight * factor),
        titleLarge = titleLarge.copy(fontSize = titleLarge.fontSize * factor, lineHeight = titleLarge.lineHeight * factor),
        titleMedium = titleMedium.copy(fontSize = titleMedium.fontSize * factor, lineHeight = titleMedium.lineHeight * factor),
        titleSmall = titleSmall.copy(fontSize = titleSmall.fontSize * factor, lineHeight = titleSmall.lineHeight * factor),
        bodyLarge = bodyLarge.copy(fontSize = bodyLarge.fontSize * factor, lineHeight = bodyLarge.lineHeight * factor),
        bodyMedium = bodyMedium.copy(fontSize = bodyMedium.fontSize * factor, lineHeight = bodyMedium.lineHeight * factor),
        bodySmall = bodySmall.copy(fontSize = bodySmall.fontSize * factor, lineHeight = bodySmall.lineHeight * factor),
        labelLarge = labelLarge.copy(fontSize = labelLarge.fontSize * factor, lineHeight = labelLarge.lineHeight * factor),
        labelMedium = labelMedium.copy(fontSize = labelMedium.fontSize * factor, lineHeight = labelMedium.lineHeight * factor),
        labelSmall = labelSmall.copy(fontSize = labelSmall.fontSize * factor, lineHeight = labelSmall.lineHeight * factor)
    )
}

@Composable
fun VaultXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Legacy support
    userDarkModeOverride: Boolean? = null,
    // 3-way mode override: "LIGHT" | "DARK" | "AMOLED" | "SYSTEM"
    themeMode: String = "SYSTEM",
    isDynamicColor: Boolean = false,
    // Accent override: "PURPLE" | "BLUE" | "GREEN" | "RED" | "ORANGE"
    accentColor: String = "PURPLE",
    // High contrast override
    highContrast: Boolean = false,
    fontSizeScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val effectiveDarkMode = when (themeMode) {
        "DARK", "AMOLED" -> true
        "LIGHT" -> false
        else -> userDarkModeOverride ?: darkTheme
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Accent mappings
    val selectedPrimaryLight = when (accentColor) {
        "BLUE" -> AccentBlueLight
        "GREEN" -> AccentGreenLight
        "RED" -> AccentRedLight
        "ORANGE" -> AccentOrangeLight
        else -> AccentPurpleLight
    }
    val selectedPrimaryDark = when (accentColor) {
        "BLUE" -> AccentBlueDark
        "GREEN" -> AccentGreenDark
        "RED" -> AccentRedDark
        "ORANGE" -> AccentOrangeDark
        else -> AccentPurpleDark
    }

    // Dynamic target values for crossfade animations
    val targetPrimary = when {
        highContrast && effectiveDarkMode -> Color.White
        highContrast && !effectiveDarkMode -> Color.Black
        effectiveDarkMode -> selectedPrimaryDark
        else -> selectedPrimaryLight
    }
    val targetBackground = when {
        highContrast && effectiveDarkMode -> HighContrastBgDark
        highContrast && !effectiveDarkMode -> HighContrastBgLight
        themeMode == "AMOLED" -> AmoledBackground
        effectiveDarkMode -> DarkBackground
        else -> LightBackground
    }
    val targetSurface = when {
        highContrast && effectiveDarkMode -> HighContrastSurfaceDark
        highContrast && !effectiveDarkMode -> HighContrastSurfaceLight
        themeMode == "AMOLED" -> AmoledSurface
        effectiveDarkMode -> DarkSurface
        else -> LightSurface
    }

    // Smooth color crossfade animation
    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 350),
        label = "primary_color"
    )
    val animatedBackground by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 350),
        label = "background_color"
    )
    val animatedSurface by animateColorAsState(
        targetValue = targetSurface,
        animationSpec = tween(durationMillis = 350),
        label = "surface_color"
    )

    // Build custom AMOLED scheme
    val AmoledColorScheme = darkColorScheme(
        primary              = selectedPrimaryDark,
        onPrimary            = DarkOnPrimary,
        primaryContainer     = DarkPrimaryVariant,
        onPrimaryContainer   = DarkOnPrimary,
        secondary            = DarkSecondary,
        onSecondary          = DarkOnSecondary,
        secondaryContainer   = Color(0xFF122C24),
        onSecondaryContainer = DarkSecondary,
        tertiary             = DarkBadgePremium,
        onTertiary           = Color(0xFF12100A),
        background           = AmoledBackground,
        onBackground         = DarkOnBackground,
        surface              = AmoledSurface,
        onSurface            = DarkOnSurface,
        surfaceVariant       = AmoledSurfaceVariant,
        onSurfaceVariant     = DarkOnSurfaceVariant,
        surfaceContainer     = AmoledSurfaceContainer,
        outline              = DarkOutline,
        outlineVariant       = DarkOutlineFocused,
        error                = DarkError,
        onError              = DarkOnError,
    )

    // Resolve active base scheme
    val baseScheme = when {
        highContrast && effectiveDarkMode -> HighContrastDarkScheme
        highContrast && !effectiveDarkMode -> HighContrastLightScheme
        isDynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (effectiveDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == "AMOLED" -> AmoledColorScheme
        effectiveDarkMode -> DarkColorScheme.copy(primary = selectedPrimaryDark)
        else -> LightColorScheme.copy(primary = selectedPrimaryLight)
    }

    // Blend animated components
    val colorScheme = baseScheme.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface
    )

    // Edge-to-edge status bars
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

    val scaledTypography = remember(fontSizeScale) {
        VaultXTypography.scale(fontSizeScale)
    }

    CompositionLocalProvider(LocalDarkMode provides effectiveDarkMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = scaledTypography,
            shapes      = VaultXShapes,
            content     = content
        )
    }
}
