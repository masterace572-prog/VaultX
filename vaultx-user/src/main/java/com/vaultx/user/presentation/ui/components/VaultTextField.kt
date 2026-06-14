package com.vaultx.user.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import com.vaultx.user.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// VaultTextField
// Custom OutlinedTextField — 12dp corners, animated focus border, clean labels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
) {
    val engine = LocalVaultUIEngine.current
    var isFocused by remember { mutableStateOf(false) }

    // Animate border color on focus
    val borderColor by animateColorAsState(
        targetValue = when {
            isError  -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else     -> MaterialTheme.colorScheme.outline
        },
        animationSpec = if (engine.animationsEnabled) tween(durationMillis = 250) else snap(),
        label = "border_color"
    )

    // Animate label color
    val labelColor by animateColorAsState(
        targetValue = when {
            isError   -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else      -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = if (engine.animationsEnabled) tween(durationMillis = 250) else snap(),
        label = "label_color"
    )

    // Error shake animation
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError && engine.animationsEnabled) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    10f at 50
                    -10f at 150
                    10f at 250
                    -10f at 350
                    0f at 400
                }
            )
        } else {
            offsetX.snapTo(0f)
        }
    }

    Column(modifier = modifier.offset(x = offsetX.value.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor
                )
            },
            placeholder = if (placeholder.isNotEmpty()) {
                {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else null,
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isFocused) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            readOnly = readOnly,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                // Container
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                errorContainerColor     = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                // Border — driven by animateColorAsState above
                focusedBorderColor    = borderColor,
                unfocusedBorderColor  = borderColor,
                errorBorderColor      = MaterialTheme.colorScheme.error,
                // Text
                focusedTextColor   = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                // Cursor
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .let { m -> if (singleLine) m.height(56.dp) else m.heightIn(min = 56.dp, max = 200.dp) }
                .onFocusChanged { isFocused = it.isFocused }
        )

        // Error message below field
        if (isError && !errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
