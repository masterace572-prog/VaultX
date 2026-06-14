package com.vaultx.user.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// --- Enums for Advanced MD3 Customization ---
enum class ThemeMode { LIGHT, DARK, SYSTEM, AMOLED }
enum class AppColor { BLUE, INDIGO, PURPLE, GREEN, ORANGE, RED, TEAL, GRAY, DYNAMIC }
enum class CornerRadiusOption { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
enum class UIDensity { COMPACT, COMFORTABLE, SPACIOUS }
enum class FontSizeOption { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
enum class FontFamilyOption { SYSTEM, SERIF, MONOSPACE }
enum class CardStyle { FILLED, ELEVATED, OUTLINED }
enum class NavStyle { BOTTOM_NAV, NAV_RAIL }
enum class IconShapeOption { ROUNDED, SQUIRCLE, CIRCLE }
enum class DashboardLayoutOption { LIST, GRID, COMPACT_GRID }

// --- VaultUIEngine ---
data class VaultUIEngine(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appColor: AppColor = AppColor.DYNAMIC,
    val cornerRadius: CornerRadiusOption = CornerRadiusOption.MEDIUM,
    val uiDensity: UIDensity = UIDensity.COMFORTABLE,
    val fontSize: FontSizeOption = FontSizeOption.MEDIUM,
    val fontFamily: FontFamilyOption = FontFamilyOption.SYSTEM,
    val cardStyle: CardStyle = CardStyle.FILLED,
    val navStyle: NavStyle = NavStyle.BOTTOM_NAV,
    val animationsEnabled: Boolean = true,
    val blurEffectsEnabled: Boolean = false,
    val amoledMode: Boolean = false,
    val iconShape: IconShapeOption = IconShapeOption.ROUNDED,
    val dashboardLayout: DashboardLayoutOption = DashboardLayoutOption.LIST
)

val LocalVaultUIEngine = compositionLocalOf { VaultUIEngine() }

// --- Color Palettes ---
private fun getColorScheme(darkTheme: Boolean, amoled: Boolean, appColor: AppColor): ColorScheme {
    // A simplified map of core seed colors for the presets
    val seedColor = when (appColor) {
        AppColor.BLUE -> Color(0xFF1976D2)
        AppColor.INDIGO -> Color(0xFF3F51B5)
        AppColor.PURPLE -> Color(0xFF6750A4)
        AppColor.GREEN -> Color(0xFF386A20)
        AppColor.ORANGE -> Color(0xFFE65100)
        AppColor.RED -> Color(0xFFB3261E)
        AppColor.TEAL -> Color(0xFF006B5E)
        AppColor.GRAY -> Color(0xFF606060)
        AppColor.DYNAMIC -> Color(0xFF6750A4) // Fallback
    }
    
    // MD3 tonal palettes based on the seed
    val primary = if (darkTheme) seedColor.copy(alpha = 0.8f) else seedColor
    val onPrimary = if (darkTheme) Color.Black else Color.White
    val background = if (amoled && darkTheme) Color.Black else if (darkTheme) Color(0xFF121212) else Color(0xFFFBFDF8)
    val surface = if (amoled && darkTheme) Color.Black else if (darkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val surfaceVariant = if (darkTheme) Color(0xFF49454F) else Color(0xFFE7E0EC)
    val onSurfaceVariant = if (darkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)
    
    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = seedColor.copy(alpha = 0.3f),
            onPrimaryContainer = seedColor.copy(alpha = 0.9f),
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = seedColor.copy(alpha = 0.2f),
            onPrimaryContainer = seedColor,
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant
        )
    }
}

// --- Typography ---
private fun getTypography(fontSize: FontSizeOption, fontFamilyOption: FontFamilyOption): Typography {
    val scale = when (fontSize) {
        FontSizeOption.SMALL -> 0.85f
        FontSizeOption.MEDIUM -> 1.0f
        FontSizeOption.LARGE -> 1.15f
        FontSizeOption.EXTRA_LARGE -> 1.3f
    }
    
    val family = when (fontFamilyOption) {
        FontFamilyOption.SERIF -> FontFamily.Serif
        FontFamilyOption.MONOSPACE -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    
    val default = Typography()
    fun androidx.compose.ui.text.TextStyle.scale() = this.copy(
        fontSize = this.fontSize * scale,
        lineHeight = this.lineHeight * scale,
        fontFamily = family
    )
    
    return Typography(
        displayLarge = default.displayLarge.scale(),
        displayMedium = default.displayMedium.scale(),
        displaySmall = default.displaySmall.scale(),
        headlineLarge = default.headlineLarge.scale(),
        headlineMedium = default.headlineMedium.scale(),
        headlineSmall = default.headlineSmall.scale(),
        titleLarge = default.titleLarge.scale(),
        titleMedium = default.titleMedium.scale(),
        titleSmall = default.titleSmall.scale(),
        bodyLarge = default.bodyLarge.scale(),
        bodyMedium = default.bodyMedium.scale(),
        bodySmall = default.bodySmall.scale(),
        labelLarge = default.labelLarge.scale(),
        labelMedium = default.labelMedium.scale(),
        labelSmall = default.labelSmall.scale()
    )
}

// --- Shapes ---
private fun getShapes(cornerRadius: CornerRadiusOption): Shapes {
    val radius = when (cornerRadius) {
        CornerRadiusOption.SMALL -> 8.dp
        CornerRadiusOption.MEDIUM -> 16.dp
        CornerRadiusOption.LARGE -> 24.dp
        CornerRadiusOption.EXTRA_LARGE -> 32.dp
    }
    return Shapes(
        small = RoundedCornerShape(radius / 2),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape(radius * 1.5f)
    )
}

// --- Theme Composable ---
object VaultXTheme {
    val colorScheme: ColorScheme
        @Composable get() = MaterialTheme.colorScheme
    val typography: Typography
        @Composable get() = MaterialTheme.typography
    val shapes: Shapes
        @Composable get() = MaterialTheme.shapes
}

val LocalDarkMode = compositionLocalOf { false }

@Composable
fun VaultXTheme(
    engine: VaultUIEngine,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (engine.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val isAmoled = engine.themeMode == ThemeMode.AMOLED || (engine.themeMode == ThemeMode.SYSTEM && systemDark && engine.amoledMode)
    
    val context = LocalContext.current
    val colorScheme = if (engine.appColor == AppColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        getColorScheme(isDark, isAmoled, engine.appColor)
    }

    val typography = getTypography(engine.fontSize, engine.fontFamily)
    val shapes = getShapes(engine.cornerRadius)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalVaultUIEngine provides engine,
        LocalDarkMode provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
