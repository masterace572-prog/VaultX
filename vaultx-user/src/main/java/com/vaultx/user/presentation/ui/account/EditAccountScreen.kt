package com.vaultx.user.presentation.ui.account

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.EntryType
import com.vaultx.user.data.model.PlatformType
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountUiState
import com.vaultx.user.presentation.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    entryId:   String,
    onSaved:   () -> Unit,
    onCancel:  () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val entryType      by viewModel.formEntryType.collectAsState()
    val platformType   by viewModel.formPlatformType.collectAsState()
    val platformLabel  by viewModel.formPlatformLabel.collectAsState()
    val username       by viewModel.formUsername.collectAsState()
    val password       by viewModel.formPassword.collectAsState()
    val websiteUrl     by viewModel.formWebsiteUrl.collectAsState()
    val appPackageName by viewModel.formAppPackageName.collectAsState()
    val gameName       by viewModel.formGameName.collectAsState()
    val gameId         by viewModel.formGameId.collectAsState()
    val gameDescription by viewModel.formGameDescription.collectAsState()
    val noteContent    by viewModel.formNoteContent.collectAsState()
    val cardHolder     by viewModel.formCardHolder.collectAsState()
    val cardNumber     by viewModel.formCardNumber.collectAsState()
    val cardExpiry     by viewModel.formCardExpiry.collectAsState()
    val cardCvv        by viewModel.formCardCvv.collectAsState()
    val cardPin        by viewModel.formCardPin.collectAsState()

    val uiState        by viewModel.uiState.collectAsState()

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

    DisposableEffect(Unit) { onDispose { viewModel.resetForm() } }

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
                text = "Edit Item",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // ── Type specific header display ──────────────────────────────────
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
                    val headerIcon = when (entryType) {
                        EntryType.LOGIN -> platformVisual(platformType).icon
                        EntryType.GAME -> Icons.Outlined.SportsEsports
                        EntryType.NOTE -> Icons.Outlined.Description
                        EntryType.CARD -> Icons.Outlined.CreditCard
                    }
                    val headerColor = when (entryType) {
                        EntryType.LOGIN -> platformVisual(platformType).color
                        EntryType.GAME -> PlatformGame
                        EntryType.NOTE -> AccentPurpleDark
                        EntryType.CARD -> AccentBlueLight
                    }
                    val headerText = when (entryType) {
                        EntryType.LOGIN -> "Login Account (${platformType.displayName})"
                        EntryType.GAME -> "Game Account"
                        EntryType.NOTE -> "Secure Note"
                        EntryType.CARD -> "Payment Card"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = headerColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = headerColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Dynamic forms fields based on type
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (entryType) {
                    EntryType.LOGIN -> {
                        VaultTextField(
                            value         = platformLabel,
                            onValueChange = viewModel::onPlatformLabelChanged,
                            label         = "Account Label",
                            placeholder   = "e.g. Personal Instagram",
                            leadingIcon   = Icons.Outlined.Label
                        )

                        VaultTextField(
                            value         = username,
                            onValueChange = viewModel::onUsernameChanged,
                            label         = "Username or Email *",
                            placeholder   = "you@example.com or @username",
                            leadingIcon   = Icons.Outlined.Person,
                            isError       = uiState is AccountUiState.Error && username.isBlank(),
                            errorMessage  = "Username or email is required"
                        )

                        VaultTextField(
                            value         = password,
                            onValueChange = viewModel::onPasswordChanged,
                            label         = "Password *",
                            leadingIcon   = Icons.Outlined.Lock,
                            isError       = uiState is AccountUiState.Error && password.isBlank(),
                            errorMessage  = "Password is required",
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                VaultIconButton(
                                    icon    = if (passVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    onClick = { passVisible = !passVisible }
                                )
                            }
                        )

                        VaultTextField(
                            value         = websiteUrl,
                            onValueChange = viewModel::onWebsiteUrlChanged,
                            label         = "Website URL (optional)",
                            placeholder   = "https://example.com",
                            leadingIcon   = Icons.Outlined.Link
                        )
                    }



                    EntryType.GAME -> {
                        VaultTextField(
                            value         = platformLabel,
                            onValueChange = viewModel::onPlatformLabelChanged,
                            label         = "Account Nickname *",
                            placeholder   = "e.g. My Main Account",
                            leadingIcon   = Icons.Outlined.Label,
                            isError       = uiState is AccountUiState.Error && platformLabel.isBlank(),
                            errorMessage  = "Nickname is required"
                        )

                        VaultTextField(
                            value         = gameName,
                            onValueChange = viewModel::onGameNameChanged,
                            label         = "Game Name *",
                            placeholder   = "e.g. Valorant",
                            leadingIcon   = Icons.Outlined.SportsEsports,
                            isError       = uiState is AccountUiState.Error && gameName.isBlank(),
                            errorMessage  = "Game name is required"
                        )

                        VaultTextField(
                            value         = gameId,
                            onValueChange = viewModel::onGameIdChanged,
                            label         = "In-Game ID (optional)",
                            placeholder   = "e.g. Player#1234",
                            leadingIcon   = Icons.Outlined.Tag
                        )

                        VaultTextField(
                            value         = username,
                            onValueChange = viewModel::onUsernameChanged,
                            label         = "Username or Email *",
                            placeholder   = "you@example.com or @username",
                            leadingIcon   = Icons.Outlined.Person,
                            isError       = uiState is AccountUiState.Error && username.isBlank(),
                            errorMessage  = "Username or email is required"
                        )

                        VaultTextField(
                            value         = password,
                            onValueChange = viewModel::onPasswordChanged,
                            label         = "Password *",
                            leadingIcon   = Icons.Outlined.Lock,
                            isError       = uiState is AccountUiState.Error && password.isBlank(),
                            errorMessage  = "Password is required",
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                VaultIconButton(
                                    icon    = if (passVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    onClick = { passVisible = !passVisible }
                                )
                            }
                        )

                        VaultTextField(
                            value         = gameDescription,
                            onValueChange = viewModel::onGameDescriptionChanged,
                            label         = "Description (optional)",
                            placeholder   = "e.g. Asia Server",
                            leadingIcon   = Icons.Outlined.Description
                        )
                    }
                    else -> {}
                }
            }

            AnimatedVisibility(visible = uiState is AccountUiState.Error) {
                val msg = (uiState as? AccountUiState.Error)?.message ?: ""
                com.vaultx.user.presentation.ui.auth.ErrorBanner(message = msg)
            }

            Spacer(Modifier.height(8.dp))

            val isFormValid = when (entryType) {
                EntryType.LOGIN -> username.isNotEmpty() && password.isNotEmpty()
                EntryType.CARD -> platformLabel.isNotEmpty() && cardNumber.isNotEmpty()
                EntryType.NOTE -> platformLabel.isNotEmpty() && noteContent.isNotEmpty()
                EntryType.GAME -> platformLabel.isNotEmpty() && gameName.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()
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
                enabled   = isFormValid
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
