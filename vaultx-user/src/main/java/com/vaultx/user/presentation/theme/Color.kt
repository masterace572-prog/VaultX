package com.vaultx.user.presentation.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// VaultX Color Palette
// Principle: NO gradients. Solid, muted, premium. High contrast.
// ─────────────────────────────────────────────────────────────────────────────

// ── Dark Mode Palette ─────────────────────────────────────────────────────────
val DarkBackground        = Color(0xFF0B0B14)  // Deep midnight blue/black
val DarkSurface           = Color(0xFF161625)  // Elevated deep blue-tinted card
val DarkSurfaceVariant    = Color(0xFF222236)  // Lighter surface
val DarkSurfaceContainer  = Color(0xFF19192B)  // Container color
val DarkOutline           = Color(0xFF383854)  // Subtle dividers
val DarkOutlineFocused    = Color(0xFF9D63FF)  // Active input border (Vibrant Purple)

val DarkPrimary           = Color(0xFFFF528E)  // Vibrant Pink/Rose — brand accent
val DarkPrimaryVariant    = Color(0xFFD6316C)  // Pressed / hover state
val DarkOnPrimary         = Color(0xFFFFFFFF)

val DarkSecondary         = Color(0xFF00E5FF)  // Bright Cyan — success / premium
val DarkOnSecondary       = Color(0xFF000000)

val DarkError             = Color(0xFFFF5252)  // Vibrant red
val DarkOnError           = Color(0xFFFFFFFF)

val DarkOnBackground      = Color(0xFFF4F4FA)  // Near-white text on dark bg
val DarkOnSurface         = Color(0xFFEBEBF5)  // Text on cards
val DarkOnSurfaceVariant  = Color(0xFFA5A5BA)  // Muted secondary text

// Free / Premium badge colors
val DarkBadgeFree         = Color(0xFF6A6A80)
val DarkBadgePremium      = Color(0xFFFFD700)  // Gold

// ── Light Mode Palette ────────────────────────────────────────────────────────
val LightBackground       = Color(0xFFF8F8FC)  // Very light lavender/grey
val LightSurface          = Color(0xFFFFFFFF)  // Pure white cards
val LightSurfaceVariant   = Color(0xFFEEEEF6)  // Very light grey with blue tint
val LightSurfaceContainer = Color(0xFFF2F2F9)
val LightOutline          = Color(0xFFC7C7D6)
val LightOutlineFocused   = Color(0xFF7C3AED)  // Deep Purple

val LightPrimary          = Color(0xFFE11D48)  // Deep Rose Pink
val LightPrimaryVariant   = Color(0xFFBE123C)
val LightOnPrimary        = Color(0xFFFFFFFF)

val LightSecondary        = Color(0xFF0891B2)  // Deep Cyan
val LightOnSecondary      = Color(0xFFFFFFFF)

val LightError            = Color(0xFFDC2626)
val LightOnError          = Color(0xFFFFFFFF)

val LightOnBackground     = Color(0xFF13131A)
val LightOnSurface        = Color(0xFF1B1B26)
val LightOnSurfaceVariant = Color(0xFF5A5A72)

val LightBadgeFree        = Color(0xFF8A8A9A)
val LightBadgePremium     = Color(0xFFD97706)  // Dark golden

// ── Shimmer Colors ────────────────────────────────────────────────────────────
val ShimmerDarkBase       = Color(0xFF1E1E22)
val ShimmerDarkHighlight  = Color(0xFF2E2E34)
val ShimmerLightBase      = Color(0xFFE8E8EE)
val ShimmerLightHighlight = Color(0xFFF5F5F9)

// ── Platform Brand Colors (for category badges) ───────────────────────────────
val PlatformInstagram     = Color(0xFFE1306C)
val PlatformTwitterX      = Color(0xFF1DA1F2)  // Classic vibrant Twitter Blue
val PlatformFacebook      = Color(0xFF1877F2)
val PlatformGoogle        = Color(0xFF4285F4)
val PlatformGame          = Color(0xFF9C27B0)
val PlatformCustom        = Color(0xFF607D8B)
