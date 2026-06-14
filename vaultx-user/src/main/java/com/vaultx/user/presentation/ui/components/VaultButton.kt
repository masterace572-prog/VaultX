package com.vaultx.user.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.vaultx.user.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// VaultButton — 3 variants: Primary, Secondary (outlined), Ghost (text-only)
// All have a built-in loading state with circular indicator
// ─────────────────────────────────────────────────────────────────────────────

enum class VaultButtonVariant { Primary, Secondary, Tonal, Ghost }

@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: VaultButtonVariant = VaultButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    val engine = LocalVaultUIEngine.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val targetScale = if (isPressed && engine.animationsEnabled) 0.95f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (!engine.animationsEnabled) snap() 
                        else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    val buttonModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .height(48.dp) // MD3 standard is often 40-48dp, 48 for larger touch targets
        .scale(scale)

    when (variant) {
        VaultButtonVariant.Primary -> {
            Button(
                onClick = { if (!isLoading) onClick() },
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    contentColor           = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            ) {
                ButtonContent(text, leadingIcon, trailingIcon, isLoading)
            }
        }
        
        VaultButtonVariant.Tonal -> {
            FilledTonalButton(
                onClick = { if (!isLoading) onClick() },
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                ButtonContent(text, leadingIcon, trailingIcon, isLoading)
            }
        }

        VaultButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = { if (!isLoading) onClick() },
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.small,
                border = ButtonDefaults.outlinedButtonBorder,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor           = MaterialTheme.colorScheme.primary,
                    disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            ) {
                ButtonContent(text, leadingIcon, trailingIcon, isLoading)
            }
        }

        VaultButtonVariant.Ghost -> {
            TextButton(
                onClick = { if (!isLoading) onClick() },
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.textButtonColors(
                    contentColor         = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            ) {
                ButtonContent(text, leadingIcon, trailingIcon, isLoading)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    isLoading: Boolean
) {
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        },
        label = "button_content"
    ) { loading ->
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = LocalContentColor.current
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
                trailingIcon?.let {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Small icon button (used for copy, delete, etc.) ───────────────────────────
@Composable
fun VaultIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val engine = LocalVaultUIEngine.current
    
    val targetScale = if (isPressed) 0.85f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (engine.animationSpeed == "Instant") snap() 
                        else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "icon_button_scale"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.size(40.dp).scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
