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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
fun AddAccountScreen(
    onSaved:  () -> Unit,
    onCancel: () -> Unit,
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

    val isLoading = uiState is AccountUiState.Loading
    var passVisible       by remember { mutableStateOf(false) }
    var showPlatformSheet by remember { mutableStateOf(false) }
    val context           = androidx.compose.ui.platform.LocalContext.current
    val haptic            = LocalHapticFeedback.current

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
                text = "Add Item",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Segmented picker
            EntryTypeSegmentedControl(
                selectedType = entryType,
                onTypeSelected = { type ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onEntryTypeChanged(type)
                    viewModel.onIsGameChanged(type == EntryType.GAME)
                }
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            AnimatedContent(
                targetState = entryType,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "form_content"
            ) { targetType ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (targetType) {
                        EntryType.LOGIN -> {
                            PlatformSelectorCard(
                                selected  = platformType,
                                onClick   = { showPlatformSheet = true }
                            )

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

                        EntryType.CARD -> {
                            CardPreview(
                                cardNumber = cardNumber,
                                cardHolder = cardHolder,
                                cardExpiry = cardExpiry
                            )

                            Spacer(Modifier.height(4.dp))

                            VaultTextField(
                                value         = platformLabel,
                                onValueChange = viewModel::onPlatformLabelChanged,
                                label         = "Card Label *",
                                placeholder   = "e.g. Personal Visa",
                                leadingIcon   = Icons.Outlined.Label,
                                isError       = uiState is AccountUiState.Error && platformLabel.isBlank(),
                                errorMessage  = "Card label is required"
                            )

                            VaultTextField(
                                value         = cardHolder,
                                onValueChange = viewModel::onCardHolderChanged,
                                label         = "Cardholder Name",
                                placeholder   = "JOHN DOE",
                                leadingIcon   = Icons.Outlined.Person
                            )

                            VaultTextField(
                                value         = cardNumber,
                                onValueChange = { val clean = it.filter { c -> c.isDigit() }.take(16); viewModel.onCardNumberChanged(clean) },
                                label         = "Card Number *",
                                placeholder   = "0000 0000 0000 0000",
                                leadingIcon   = Icons.Outlined.CreditCard,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError       = uiState is AccountUiState.Error && cardNumber.isBlank(),
                                errorMessage  = "Card number is required"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                VaultTextField(
                                    value         = cardExpiry,
                                    onValueChange = {
                                        var clean = it.replace("/", "").filter { c -> c.isDigit() }.take(4)
                                        if (clean.length > 2) {
                                            clean = clean.substring(0, 2) + "/" + clean.substring(2)
                                        }
                                        viewModel.onCardExpiryChanged(clean)
                                    },
                                    label         = "Expiry (MM/YY)",
                                    placeholder   = "MM/YY",
                                    leadingIcon   = Icons.Outlined.CalendarToday,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier      = Modifier.weight(1f)
                                )

                                VaultTextField(
                                    value         = cardCvv,
                                    onValueChange = { viewModel.onCardCvvChanged(it.filter { c -> c.isDigit() }.take(4)) },
                                    label         = "CVV",
                                    placeholder   = "000",
                                    leadingIcon   = Icons.Outlined.Pin,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier      = Modifier.weight(1f)
                                )
                            }

                            VaultTextField(
                                value         = cardPin,
                                onValueChange = { viewModel.onCardPinChanged(it.filter { c -> c.isDigit() }.take(6)) },
                                label         = "Card PIN (optional)",
                                placeholder   = "0000",
                                leadingIcon   = Icons.Outlined.Lock,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        EntryType.NOTE -> {
                            VaultTextField(
                                value         = platformLabel,
                                onValueChange = viewModel::onPlatformLabelChanged,
                                label         = "Note Title *",
                                placeholder   = "e.g. WiFi Password, Lockbox Code",
                                leadingIcon   = Icons.Outlined.Label,
                                isError       = uiState is AccountUiState.Error && platformLabel.isBlank(),
                                errorMessage  = "Note title is required"
                            )

                            VaultTextField(
                                value         = noteContent,
                                onValueChange = viewModel::onNoteContentChanged,
                                label         = "Note Content *",
                                placeholder   = "Type your secure note here...",
                                leadingIcon   = Icons.Outlined.Description,
                                singleLine    = false,
                                maxLines      = 10,
                                isError       = uiState is AccountUiState.Error && noteContent.isBlank(),
                                errorMessage  = "Note content is required"
                            )
                        }

                        EntryType.GAME -> {
                            PlatformSelectorCard(
                                selected  = platformType,
                                onClick   = { showPlatformSheet = true }
                            )

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
                    }
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
                EntryType.GAME -> platformLabel.isNotEmpty() && gameName.isNotEmpty() && password.isNotEmpty()
            }

            VaultButton(
                text      = "Save Item",
                onClick   = {
                    viewModel.saveAccount { success, message ->
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

    if (showPlatformSheet) {
        PlatformPickerSheet(
            selected = platformType,
            entryType = entryType,
            onSelect = { type ->
                viewModel.onPlatformTypeChanged(type)
                showPlatformSheet = false
            },
            onDismiss = { showPlatformSheet = false }
        )
    }
}

@Composable
fun EntryTypeSegmentedControl(
    selectedType: EntryType,
    onTypeSelected: (EntryType) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf(
        EntryType.LOGIN to "Login",
        EntryType.CARD to "Card",
        EntryType.NOTE to "Note",
        EntryType.GAME to "Game"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            types.forEach { (type, label) ->
                val isSelected = selectedType == type
                val backgroundAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_text"
                )
                val indicatorColor = MaterialTheme.colorScheme.primary

                Surface(
                    onClick = { onTypeSelected(type) },
                    shape = RoundedCornerShape(8.dp),
                    color = indicatorColor.copy(alpha = backgroundAlpha),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentColor = textColor
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardPreview(
    cardNumber: String,
    cardHolder: String,
    cardExpiry: String,
    modifier: Modifier = Modifier
) {
    val cleanNumber = cardNumber.replace(" ", "")
    val cardBrand = when {
        cleanNumber.startsWith("4") -> "VISA"
        cleanNumber.startsWith("5") -> "MASTERCARD"
        cleanNumber.startsWith("3") -> "AMEX"
        else -> "CARD"
    }

    val brandColor = when (cardBrand) {
        "VISA" -> Color(0xFF1E293B)
        "MASTERCARD" -> Color(0xFF3B0764)
        "AMEX" -> Color(0xFF0F172A)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = brandColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.minDimension * 0.7f,
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.1f)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.size(30.dp, 22.dp)
                    ) {}
                    
                    Text(
                        text = cardBrand,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                val formattedNumber = remember(cardNumber) {
                    val formatted = java.lang.StringBuilder()
                    val clean = cardNumber.replace(" ", "").take(16)
                    for (i in clean.indices) {
                        formatted.append(clean[i])
                        if ((i + 1) % 4 == 0 && i < clean.length - 1) {
                            formatted.append("  ")
                        }
                    }
                    formatted.toString().ifEmpty { "••••  ••••  ••••  ••••" }
                }

                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CARDHOLDER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 8.sp
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = cardHolder.uppercase().ifEmpty { "YOUR NAME" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "EXPIRES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 8.sp
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = cardExpiry.ifEmpty { "MM/YY" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

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
    entryType: EntryType,
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
            val platforms = if (entryType == EntryType.GAME) {
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
