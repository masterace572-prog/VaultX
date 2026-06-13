package com.vaultx.user.presentation.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.VaultButton
import com.vaultx.user.presentation.ui.components.VaultTextField
import com.vaultx.user.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// EditProfileScreen — Edit name + change password
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val currentName by settingsViewModel.userName.collectAsState()
    val currentEmail by settingsViewModel.userEmail.collectAsState()

    var name by remember { mutableStateOf(currentName) }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Change password state
    var showChangePassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
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
            Spacer(Modifier.height(8.dp))

            // ── Account Info Card ────────────────────────────────────────────
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Outlined.Person, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp).size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                currentName.ifBlank { "User" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                currentEmail.ifBlank { "No email" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Edit Name Section ────────────────────────────────────────────
            Text(
                "PERSONAL INFORMATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VaultTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Display Name",
                        leadingIcon = Icons.Outlined.Person
                    )

                    VaultButton(
                        text = "Save Name",
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                try {
                                    val user = FirebaseAuth.getInstance().currentUser
                                    if (user != null) {
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build()
                                        user.updateProfile(profileUpdates).await()
                                        FirebaseFirestore.getInstance().collection("users")
                                            .document(user.uid).update("name", name).await()
                                        successMessage = "Name updated successfully"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to update profile"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        isLoading = isLoading,
                        enabled = name.isNotBlank() && name != currentName
                    )
                }
            }

            // ── Change Password Section ──────────────────────────────────────
            Text(
                "SECURITY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { showChangePassword = !showChangePassword },
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Lock, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Change Password",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Requires current password",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                if (showChangePassword) Icons.Outlined.ExpandLess
                                else Icons.Outlined.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = showChangePassword) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            VaultTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = "Current Password *",
                                leadingIcon = Icons.Outlined.Lock,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            VaultTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = "New Password *",
                                leadingIcon = Icons.Outlined.LockReset,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            VaultTextField(
                                value = confirmNewPassword,
                                onValueChange = { confirmNewPassword = it },
                                label = "Confirm New Password *",
                                leadingIcon = Icons.Outlined.LockReset,
                                visualTransformation = PasswordVisualTransformation(),
                                isError = confirmNewPassword.isNotEmpty() && confirmNewPassword != newPassword,
                                errorMessage = if (confirmNewPassword.isNotEmpty() && confirmNewPassword != newPassword) "Passwords don't match" else null
                            )

                            VaultButton(
                                text = "Update Password",
                                onClick = {
                                    scope.launch {
                                        isChangingPassword = true
                                        errorMessage = null
                                        successMessage = null
                                        try {
                                            val user = FirebaseAuth.getInstance().currentUser
                                            val email = user?.email
                                            if (user != null && email != null) {
                                                // Re-authenticate with current password
                                                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                                                user.reauthenticate(credential).await()
                                                // Update to new password
                                                user.updatePassword(newPassword).await()
                                                successMessage = "Password changed successfully"
                                                currentPassword = ""
                                                newPassword = ""
                                                confirmNewPassword = ""
                                                showChangePassword = false
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = when {
                                                e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Current password is incorrect"
                                                e.message?.contains("weak-password") == true -> "New password is too weak (min 6 chars)"
                                                else -> e.message ?: "Failed to change password"
                                            }
                                        } finally {
                                            isChangingPassword = false
                                        }
                                    }
                                },
                                isLoading = isChangingPassword,
                                enabled = currentPassword.isNotEmpty() &&
                                    newPassword.length >= 6 &&
                                    newPassword == confirmNewPassword
                            )
                        }
                    }
                }
            }

            // ── Status Messages ──────────────────────────────────────────────
            AnimatedVisibility(visible = successMessage != null) {
                Surface(
                    shape = ShapeChip,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp))
                        Text(
                            successMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = ShapeChip,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                        Text(
                            errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
