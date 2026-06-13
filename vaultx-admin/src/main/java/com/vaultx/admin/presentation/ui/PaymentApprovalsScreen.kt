package com.vaultx.admin.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultx.admin.data.model.Payment
import com.vaultx.admin.presentation.AdminViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.admin.presentation.ui.components.VaultButton
import com.vaultx.admin.presentation.ui.components.VaultButtonVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentApprovalsScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val payments by viewModel.payments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pendingPayments = payments.filter { it.status == "pending" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    "Payment Approvals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (isLoading && pendingPayments.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pendingPayments.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No pending payments.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(pendingPayments, key = { it.paymentId }) { payment ->
                            PaymentApprovalCard(
                                payment = payment,
                                onApprove = { 
                                    val plan = viewModel.plans.value.find { it.id == payment.planId }
                                    val days = plan?.durationDays ?: 30
                                    viewModel.approvePayment(payment.paymentId, payment.uid, days) { _, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReject = { 
                                    viewModel.rejectPayment(payment.paymentId) { _, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun PaymentApprovalCard(
    payment: Payment,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(payment.submittedAt?.toDate()?.toString() ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("User: ${payment.userEmail.ifBlank { payment.uid }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("UTR: ${payment.utr}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Plan ID: ${payment.planId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VaultButton(
                    text = "Approve",
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                )
                VaultButton(
                    text = "Reject",
                    onClick = onReject,
                    variant = VaultButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
