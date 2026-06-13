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
import com.vaultx.user.data.model.PlatformType
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountUiState
import com.vaultx.user.presentation.viewmodel.AccountViewModel

// ─────────────────────────────────────────────────────────────────────────────
// AddAccountScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onSaved:  () -> Unit,
    onCancel: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val platformType   by viewModel.formPlatformType.collectAsState()
    val platformLabel  by viewModel.formPlatformLabel.collectAsState()
    val username       by viewModel.formUsername.collectAsState()
    val password       by viewModel.formPassword.collectAsState()
    val isGame         by viewModel.formIsGame.collectAsState()
    val gameName       by viewModel.formGameName.collectAsState()
    val gameId         by viewModel.formGameId.collectAsState()
    val gameDescription by viewModel.formGameDescription.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()

    val isLoading = uiState is AccountUiState.Loading
    var passVisible       by remember { mutableStateOf(false) }
    var showPlatformSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.Success) {
            viewModel.resetForm()
            onSaved()
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.resetForm() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Add Account", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Close, "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Platform Selector ─────────────────────────────────────────────
            PlatformSelectorCard(
                selected  = platformType,
                onClick   = { showPlatformSheet = true }
            )

            // ── Label ─────────────────────────────────────────────────────────
            VaultTextField(
                value         = platformLabel,
                onValueChange = viewModel::onPlatformLabelChanged,
                label         = if (isGame) "Account Label / Nickname *" else "Account Label",
                placeholder   = if (isGame) "e.g. My Main Account, Level 70 Smurf" else "e.g. Personal Instagram",
                leadingIcon   = Icons.Outlined.Label
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            if (!isGame) {
                // ── Username / email (at least one required) ────────────────
                VaultTextField(
                    value         = username,
                    onValueChange = viewModel::onUsernameChanged,
                    label         = "Username or Email *",
                    placeholder   = "you@example.com or @username",
                    leadingIcon   = Icons.Outlined.Person,
                    isError       = uiState is AccountUiState.Error && username.isBlank(),
                    errorMessage  = if (uiState is AccountUiState.Error && username.isBlank()) "Username or email is required" else null
                )
            }

            // ── Password row ──────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultTextField(
                    value         = password,
                    onValueChange = viewModel::onPasswordChanged,
                    label         = "Password *",
                    leadingIcon   = Icons.Outlined.Lock,
                    isError       = uiState is AccountUiState.Error &&
                                    password.isBlank(),
                    errorMessage  = "Password is required",
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
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // ── Game account toggle ───────────────────────────────────────────
            GameAccountToggle(
                isGame    = isGame,
                onToggle  = viewModel::onIsGameChanged
            )

            // ── Game fields (AnimatedVisibility) ─────────────────────────────
            AnimatedVisibility(
                visible      = isGame,
                enter        = expandVertically(tween(350, easing = EaseOutCubic)) + fadeIn(tween(300)),
                exit         = shrinkVertically(tween(300, easing = EaseInCubic)) + fadeOut(tween(250))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Spacer(Modifier.height(0.dp))

                    VaultTextField(
                        value         = gameName,
                        onValueChange = viewModel::onGameNameChanged,
                        label         = "Game Name *",
                        placeholder   = "e.g. Valorant, Genshin Impact",
                        leadingIcon   = Icons.Outlined.SportsEsports
                    )
                    VaultTextField(
                        value         = gameId,
                        onValueChange = viewModel::onGameIdChanged,
                        label         = "In-Game ID (optional)",
                        leadingIcon   = Icons.Outlined.Tag
                    )
                    VaultTextField(
                        value         = gameDescription,
                        onValueChange = viewModel::onGameDescriptionChanged,
                        label         = "Description (optional)",
                        placeholder   = "e.g. Smurf account, Asia Server",
                        leadingIcon   = Icons.Outlined.Description
                    )
                }
            }

            // ── Error banner ──────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState is AccountUiState.Error) {
                val msg = (uiState as? AccountUiState.Error)?.message ?: ""
                com.vaultx.user.presentation.ui.auth.ErrorBanner(message = msg)
            }

            Spacer(Modifier.height(8.dp))

            // ── Save button ───────────────────────────────────────────────────
            VaultButton(
                text      = "Save Account",
                onClick   = viewModel::saveAccount,
                isLoading = isLoading,
                enabled   = password.isNotEmpty() && (
                    if (isGame) platformLabel.isNotEmpty() && gameName.isNotEmpty()
                    else username.isNotEmpty()
                )
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Platform bottom sheet ─────────────────────────────────────────────────
    if (showPlatformSheet) {
        PlatformPickerSheet(
            selected = platformType,
            isGame   = isGame,
            onSelect = { type ->
                viewModel.onPlatformTypeChanged(type)
                showPlatformSheet = false
            },
            onDismiss = { showPlatformSheet = false }
        )
    }
}

// ── Platform selector card ─────────────────────────────────────────────────────
@Composable
private fun PlatformSelectorCard(selected: PlatformType, onClick: () -> Unit) {
    Surface(
        onClick        = onClick,
        shape          = ShapeCard,
        color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PlatformIcon(type = selected, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Platform", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected.displayName, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.Outlined.ExpandMore, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformPickerSheet(
    selected:  PlatformType,
    isGame:    Boolean,
    onSelect:  (PlatformType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape            = ShapeBottomSheet,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text     = "Choose Platform",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            val platforms = if (isGame) {
                listOf(PlatformType.GOOGLE, PlatformType.FACEBOOK, PlatformType.TWITTER, PlatformType.DISCORD)
            } else {
                PlatformType.values().filter { it != PlatformType.GAME }.toList()
            }
            platforms.forEach { type ->
                Surface(
                    onClick = { onSelect(type) },
                    color   = if (type == selected)
                                  MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                              else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        PlatformIcon(type = type, size = 40.dp)
                        Text(type.displayName, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        if (type == selected) {
                            Icon(Icons.Outlined.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// Password Generator Removed

// ── Game account toggle ────────────────────────────────────────────────────────
@Composable
private fun GameAccountToggle(isGame: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape  = ShapeCard,
        color  = if (isGame) PlatformGame.copy(alpha = 0.08f)
                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.SportsEsports, null,
                    tint = if (isGame) PlatformGame else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
                Column {
                    Text("Game Account", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Add game-specific details", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked         = isGame,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor   = PlatformGame,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// ── Easing curves ─────────────────────────────────────────────────────────────
private val EaseOutCubic = androidx.compose.animation.core.CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInCubic  = androidx.compose.animation.core.CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
