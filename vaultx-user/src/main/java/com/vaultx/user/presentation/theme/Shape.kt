package com.vaultx.user.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// VaultX Shape System
// Rounded corners in multiples of 4dp — clean, modern, iOS-adjacent
// ─────────────────────────────────────────────────────────────────────────────
val VaultXShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Convenience aliases used in components
val ShapeCard     = RoundedCornerShape(20.dp) // Apple-style large card radius
val ShapeInput    = RoundedCornerShape(16.dp)
val ShapeButton   = RoundedCornerShape(16.dp)
val ShapeChip     = RoundedCornerShape(12.dp)
val ShapeBottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ShapeBadge    = RoundedCornerShape(8.dp)
val ShapeFull     = RoundedCornerShape(50)  // Pill / FAB
