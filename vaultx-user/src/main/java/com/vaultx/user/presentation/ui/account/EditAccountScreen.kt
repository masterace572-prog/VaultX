package com.vaultx.user.presentation.ui.account

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.auth.ErrorBanner
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountUiState
import com.vaultx.user.presentation.viewmodel.AccountViewModel

import androidx.compose.ui.text.font.FontWeight

// ─────────────────────────────────────────────────────────────────────────────
// EditAccountScreen — pre-populated form mirroring AddAccount
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    entryId:   String,
    onSaved:   () -> Unit,
    onCancel:  () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val platformType  by viewModel.formPlatformType.collectAsState()
    val platformLabel by viewModel.formPlatformLabel.collectAsState()
    val username      by viewModel.formUsername.collectAsState()
    val password      by viewModel.formPassword.collectAsState()
    val isGame        by viewModel.formIsGame.collectAsState()
    val gameName      by viewModel.formGameName.collectAsState()
    val gameId        by viewModel.formGameId.collectAsState()
    val gameDescription by viewModel.formGameDescription.collectAsState()
    val uiState       by viewModel.uiState.collectAsState()

    var passVisible   by remember { mutableStateOf(false) }

    val isLoading = uiState is AccountUiState.Loading
    val context   = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(entryId) { viewModel.loadAccountForEdit(entryId) }

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.Success) {
            viewModel.resetForm()
            onSaved()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            // ── Platform display (non-editable in edit mode) ──────────────────
            Surface(
                shape    = ShapeCard,
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    PlatformIcon(type = platformType, size = 44.dp)
                    Text(platformType.displayName, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            VaultTextField(
                value = platformLabel, onValueChange = viewModel::onPlatformLabelChanged,
                label = "Account Label", leadingIcon = Icons.Outlined.Label
            )
            
            VaultTextField(
                value = username, onValueChange = viewModel::onUsernameChanged,
                label = "Username or Email *", placeholder = "you@example.com or @username", leadingIcon = Icons.Outlined.Person
            )
            VaultTextField(
                value = password, onValueChange = viewModel::onPasswordChanged,
                label = "Password *", leadingIcon = Icons.Outlined.Lock,
                visualTransformation = if (passVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    VaultIconButton(
                        icon    = if (passVisible) Icons.Outlined.VisibilityOff
                                  else Icons.Outlined.Visibility,
                        onClick = { passVisible = !passVisible }
                    )
                }
            )

            // Game toggle + animated fields (same as AddAccount)
            GameAccountToggle(isGame = isGame, onToggle = viewModel::onIsGameChanged)

            AnimatedVisibility(
                visible = isGame,
                enter   = expandVertically(tween(350, easing = EaseOutCubic)) + fadeIn(tween(300)),
                exit    = shrinkVertically(tween(300)) + fadeOut(tween(250))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Spacer(Modifier.height(0.dp))
                    VaultTextField(value = gameName, onValueChange = viewModel::onGameNameChanged,
                        label = "Game Name *", placeholder = "e.g. Valorant", leadingIcon = Icons.Outlined.SportsEsports)
                    VaultTextField(value = gameId, onValueChange = viewModel::onGameIdChanged,
                        label = "In-Game ID (optional)", leadingIcon = Icons.Outlined.Tag)
                    VaultTextField(value = gameDescription, onValueChange = viewModel::onGameDescriptionChanged,
                        label = "Description (optional)", placeholder = "e.g. Asia Server", leadingIcon = Icons.Outlined.Description)
                }
            }

            AnimatedVisibility(visible = uiState is AccountUiState.Error) {
                ErrorBanner(message = (uiState as? AccountUiState.Error)?.message ?: "")
            }

            VaultButton(
                text      = "Save Changes",
                onClick   = {
                    viewModel.updateAccount { success, message ->
                        if (message != null) {
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isLoading = isLoading,
                enabled   = password.isNotEmpty()
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Re-use the composable from AddAccountScreen file scope
@Composable
private fun GameAccountToggle(isGame: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape    = ShapeCard,
        color    = if (isGame) PlatformGame.copy(alpha = 0.08f)
                   else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SportsEsports, null,
                    tint = if (isGame) PlatformGame else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
                Text("Game Account", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Switch(
                checked = isGame, onCheckedChange = onToggle,
                colors  = SwitchDefaults.colors(
                    checkedTrackColor = PlatformGame,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}

private val EaseOutCubic = androidx.compose.animation.core.CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
