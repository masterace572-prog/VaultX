package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.data.model.PromoCode
import com.vaultx.admin.presentation.AdminViewModel
import com.vaultx.admin.presentation.ui.components.VaultButton
import com.vaultx.admin.presentation.ui.components.VaultButtonVariant
import com.vaultx.admin.presentation.ui.components.VaultTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoCodesScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val promoCodes by viewModel.promoCodes.collectAsState()
    val plans by viewModel.plans.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingPromo by remember { mutableStateOf<PromoCode?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPromo = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Promo Code")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Inline Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Promo Codes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (promoCodes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No promo codes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(promoCodes) { promo ->
                    PromoCodeCard(
                        promo = promo,
                        planName = plans.find { it.id == promo.freePlanId }?.name,
                        onEdit = {
                            editingPromo = promo
                            showDialog = true
                        },
                        onDelete = { 
                            viewModel.deletePromoCode(promo.id) { _, msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        if (showDialog) {
            PromoCodeDialog(
                promo = editingPromo,
                plans = plans,
                onDismiss = { showDialog = false },
                onSave = {
                    if (editingPromo == null) {
                        viewModel.createPromoCode(it) { _, msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.updatePromoCode(it) { _, msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    showDialog = false
                }
            )
        }
    } // End of Column
    } // End of Scaffold padding block
}

@Composable
fun PromoCodeCard(
    promo: PromoCode,
    planName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(promo.code, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (promo.freePlanId != null) {
                Text("Grants free plan: ${planName ?: promo.freePlanId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (promo.discountPercent != null) {
                Text("Discount: ${promo.discountPercent}% off", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text("Uses: ${promo.currentUses} / ${promo.maxUses}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Status: ${if (promo.isActive) "Active" else "Inactive"}", style = MaterialTheme.typography.bodySmall, color = if (promo.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoCodeDialog(
    promo: PromoCode?,
    plans: List<com.vaultx.admin.data.model.Plan>,
    onDismiss: () -> Unit,
    onSave: (PromoCode) -> Unit
) {
    var code by remember { mutableStateOf(promo?.code ?: "") }
    var isDiscount by remember { mutableStateOf(promo?.freePlanId == null) }
    var discountPercent by remember { mutableStateOf(promo?.discountPercent?.toString() ?: "") }
    var freePlanId by remember { mutableStateOf(promo?.freePlanId ?: "") }
    var maxUses by remember { mutableStateOf(promo?.maxUses?.toString() ?: "100") }
    var isActive by remember { mutableStateOf(promo?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (promo == null) "Create Promo Code" else "Edit Promo Code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = "Code (e.g. SUMMER50)",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = promo == null // cannot change code once created
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isDiscount, onClick = { isDiscount = true })
                    Text("Percentage Discount")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = !isDiscount, onClick = { isDiscount = false })
                    Text("Free Plan")
                }

                if (isDiscount) {
                    VaultTextField(
                        value = discountPercent,
                        onValueChange = { discountPercent = it },
                        label = "Discount Percent (e.g. 50)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        VaultTextField(
                            value = plans.find { it.id == freePlanId }?.name ?: freePlanId,
                            onValueChange = {},
                            readOnly = true,
                            label = "Select Plan",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            plans.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.name) },
                                    onClick = {
                                        freePlanId = plan.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                VaultTextField(
                    value = maxUses,
                    onValueChange = { maxUses = it },
                    label = "Max Uses",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Is Active")
                }
            }
        },
        confirmButton = {
            VaultButton(
                text = "Save",
                variant = VaultButtonVariant.Ghost,
                fullWidth = false,
                onClick = {
                    onSave(
                        PromoCode(
                            id = promo?.id ?: code.uppercase(),
                            code = code.uppercase(),
                            discountPercent = if (isDiscount) discountPercent.toDoubleOrNull() else null,
                            freePlanId = if (!isDiscount) freePlanId.takeIf { it.isNotBlank() } else null,
                            maxUses = maxUses.toIntOrNull() ?: 100,
                            currentUses = promo?.currentUses ?: 0,
                            isActive = isActive
                        )
                    )
                }
            )
        },
        dismissButton = {
            VaultButton(text = "Cancel", variant = VaultButtonVariant.Ghost, fullWidth = false, onClick = onDismiss)
        }
    )
}
