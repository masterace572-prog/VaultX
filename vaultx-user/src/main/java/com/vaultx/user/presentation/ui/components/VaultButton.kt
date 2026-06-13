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
import com.vaultx.user.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// VaultButton — 3 variants: Primary, Secondary (outlined), Ghost (text-only)
// All have a built-in loading state with circular indicator
// ─────────────────────────────────────────────────────────────────────────────

enum class VaultButtonVariant { Primary, Secondary, Ghost }

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
    val buttonModifier = modifier.then(
        if (fullWidth) Modifier.fillMaxWidth() else Modifier
    ).height(44.dp)

    when (variant) {
        VaultButtonVariant.Primary -> {
            Button(
                onClick = { if (!isLoading) onClick() },
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = ShapeButton,
                colors = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    contentColor           = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    disabledContentColor   = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp,
                )
            ) {
                ButtonContent(text, leadingIcon, trailingIcon, isLoading)
            }
        }

        VaultButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = { if (!isLoading) onClick() },
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = ShapeButton,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor           = MaterialTheme.colorScheme.primary,
                    disabledContentColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
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
                shape = ShapeButton,
                colors = ButtonDefaults.textButtonColors(
                    contentColor         = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
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
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
