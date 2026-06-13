package com.vaultx.admin.presentation.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// VaultX Color Palette
// Principle: NO gradients. Solid, muted, premium. High contrast.
// ─────────────────────────────────────────────────────────────────────────────

// ── Dark Mode Palette ─────────────────────────────────────────────────────────
val DarkBackground        = Color(0xFF0A0A0A)  // Near-black charcoal
val DarkSurface           = Color(0xFF171717)  // Elevated card surface
val DarkSurfaceVariant    = Color(0xFF252529)  // Slightly lighter surface
val DarkSurfaceContainer  = Color(0xFF1E1E22)  // Container color
val DarkOutline           = Color(0xFF27272A)  // Subtle dividers / borders
val DarkOutlineFocused    = Color(0xFF7A7AFF)  // Active input border

val DarkPrimary           = Color(0xFF8B5CF6)  // Soft indigo — brand accent
val DarkPrimaryVariant    = Color(0xFF5C5CCC)  // Pressed / hover state
val DarkOnPrimary         = Color(0xFFFFFFFF)

val DarkSecondary         = Color(0xFF4CAF82)  // Muted emerald — success / premium
val DarkOnSecondary       = Color(0xFFFFFFFF)

val DarkError             = Color(0xFFE57373)  // Muted red — errors
val DarkOnError           = Color(0xFFFFFFFF)

val DarkOnBackground      = Color(0xFFF1F1F3)  // Near-white text on dark bg
val DarkOnSurface         = Color(0xFFE8E8EC)  // Text on cards
val DarkOnSurfaceVariant  = Color(0xFF9898A6)  // Muted secondary text

// Free / Premium badge colors
val DarkBadgeFree         = Color(0xFF5A5A6A)  // Muted grey
val DarkBadgePremium      = Color(0xFFD4AF37)  // Warm gold

// ── Light Mode Palette ────────────────────────────────────────────────────────
val LightBackground       = Color(0xFFFAFAFA)  // Warm off-white
val LightSurface          = Color(0xFFFFFFFF)  // Pure white cards
val LightSurfaceVariant   = Color(0xFFEFEFF3)  // Very light grey
val LightSurfaceContainer = Color(0xFFF0F0F5)
val LightOutline          = Color(0xFFE5E7EB)
val LightOutlineFocused   = Color(0xFF5C5CCC)

val LightPrimary          = Color(0xFF7C3AED)  // Deeper indigo for light mode
val LightPrimaryVariant   = Color(0xFF3737A8)
val LightOnPrimary        = Color(0xFFFFFFFF)

val LightSecondary        = Color(0xFF2E7D5A)  // Deep emerald
val LightOnSecondary      = Color(0xFFFFFFFF)

val LightError            = Color(0xFFB00020)
val LightOnError          = Color(0xFFFFFFFF)

val LightOnBackground     = Color(0xFF12121A)  // Deep charcoal text on light
val LightOnSurface        = Color(0xFF1A1A24)
val LightOnSurfaceVariant = Color(0xFF5A5A72)

val LightBadgeFree        = Color(0xFF8A8A9A)
val LightBadgePremium     = Color(0xFFB8860B)  // Dark golden

// ── Shimmer Colors ────────────────────────────────────────────────────────────
val ShimmerDarkBase       = Color(0xFF1E1E22)
val ShimmerDarkHighlight  = Color(0xFF2E2E34)
val ShimmerLightBase      = Color(0xFFE8E8EE)
val ShimmerLightHighlight = Color(0xFFF5F5F9)

// ── Platform Brand Colors (for category badges) ───────────────────────────────
val PlatformInstagram     = Color(0xFFE1306C)
val PlatformTwitterX      = Color(0xFF14171A)
val PlatformFacebook      = Color(0xFF1877F2)
val PlatformGoogle        = Color(0xFF4285F4)
val PlatformGame          = Color(0xFF9C27B0)
val PlatformCustom        = Color(0xFF607D8B)

