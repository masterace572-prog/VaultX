package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.vaultx.admin.presentation.ui.components.VaultTextField
import com.vaultx.admin.presentation.ui.components.VaultButton
import com.vaultx.admin.presentation.ui.components.VaultButtonVariant
import com.vaultx.admin.presentation.ui.components.VaultIconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.data.model.Plan
import com.vaultx.admin.presentation.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlansScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val plans by viewModel.plans.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<Plan?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingPlan = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Plan")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Inline Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
            ) {
                VaultIconButton(icon = Icons.Outlined.ArrowBack, onClick = onBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Manage Plans",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (plans.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                Text("No plans configured.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                VaultButton(
                    text = "Create Plan",
                    onClick = { editingPlan = null; showDialog = true },
                    leadingIcon = Icons.Outlined.Add,
                    fullWidth = false
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(plans) { plan ->
                    PlanCard(
                        plan = plan,
                        onEdit = { editingPlan = plan; showDialog = true },
                        onDelete = { 
                            viewModel.deletePlan(plan.id) { _, msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            }
        }
    }

    if (showDialog) {
        PlanDialog(
            plan = editingPlan,
            onDismiss = { showDialog = false },
            onSave = { savedPlan ->
                if (editingPlan == null) {
                    viewModel.createPlan(savedPlan) { _, msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.updatePlan(savedPlan) { _, msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun PlanCard(plan: Plan, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (plan.isRecommended) 2.dp else 1.dp,
            color = if (plan.isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                if (plan.isRecommended) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "Recommended", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("₹${plan.price} / ${plan.durationDays} days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                VaultButton(
                    text = "Delete",
                    onClick = onDelete,
                    variant = VaultButtonVariant.Ghost,
                    fullWidth = false
                )
                Spacer(Modifier.width(8.dp))
                VaultButton(
                    text = "Edit",
                    onClick = onEdit,
                    fullWidth = false
                )
            }
        }
    }
}

@Composable
fun PlanDialog(plan: Plan?, onDismiss: () -> Unit, onSave: (Plan) -> Unit) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var basePriceStr by remember { mutableStateOf(plan?.basePrice?.toString() ?: plan?.price?.toString() ?: "") }
    
    val initialDiscount = if (plan?.basePrice != null && plan.price < plan.basePrice && plan.basePrice > 0.0) {
        ((1.0 - (plan.price / plan.basePrice)) * 100.0).toString()
    } else {
        "0"
    }
    var discountPercentStr by remember { mutableStateOf(initialDiscount) }
    
    var durationStr by remember { mutableStateOf(plan?.durationDays?.toString() ?: "") }
    var featuresStr by remember { mutableStateOf(plan?.features?.joinToString("\n") ?: "") }
    var isRecommended by remember { mutableStateOf(plan?.isRecommended ?: false) }
    var orderIndexStr by remember { mutableStateOf(plan?.orderIndex?.toString() ?: "0") }

    val basePrice = basePriceStr.toDoubleOrNull() ?: 0.0
    val discount = discountPercentStr.toDoubleOrNull() ?: 0.0
    var calculated = basePrice * (1.0 - (discount / 100.0))
    val intPart = Math.round(calculated).toDouble()
    val diffTo99 = Math.abs(calculated - (intPart - 0.01))
    val diffTo00 = Math.abs(calculated - intPart)
    calculated = if (diffTo99 < diffTo00) (intPart - 0.01) else intPart
    if (calculated < 0.0) calculated = 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "New Plan" else "Edit Plan", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VaultTextField(value = name, onValueChange = { name = it }, label = "Name", modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VaultTextField(value = basePriceStr, onValueChange = { basePriceStr = it }, label = "Base Price (INR)", modifier = Modifier.weight(1f))
                    VaultTextField(value = discountPercentStr, onValueChange = { discountPercentStr = it }, label = "Discount (%)", modifier = Modifier.weight(1f))
                }
                Text("Calculated Price: ₹${String.format(java.util.Locale.US, "%.2f", calculated)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                VaultTextField(value = durationStr, onValueChange = { durationStr = it }, label = "Duration (Days)", modifier = Modifier.fillMaxWidth())
                VaultTextField(value = orderIndexStr, onValueChange = { orderIndexStr = it }, label = "Order Index (0 is first)", modifier = Modifier.fillMaxWidth())
                VaultTextField(value = featuresStr, onValueChange = { featuresStr = it }, label = "Features (One per line)", modifier = Modifier.height(100.dp).fillMaxWidth(), singleLine = false)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecommended, onCheckedChange = { isRecommended = it })
                    Text("Recommended Plan", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            VaultButton(
                text = "Save",
                onClick = {
                    val p = Plan(
                        id = plan?.id ?: "",
                        name = name,
                        price = calculated,
                        basePrice = basePrice.takeIf { it > calculated },
                        durationDays = durationStr.toIntOrNull() ?: 30,
                        features = featuresStr.split("\n").filter { it.isNotBlank() },
                        isActive = true,
                        isRecommended = isRecommended,
                        orderIndex = orderIndexStr.toIntOrNull() ?: 0
                    )
                    onSave(p)
                },
                fullWidth = false
            )
        },
        dismissButton = { VaultButton(text = "Cancel", onClick = onDismiss, variant = VaultButtonVariant.Ghost, fullWidth = false) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    )
}
