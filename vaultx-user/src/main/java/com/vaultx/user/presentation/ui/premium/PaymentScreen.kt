package com.vaultx.user.presentation.ui.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.auth.ErrorBanner
import com.vaultx.user.presentation.ui.components.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.vaultx.user.presentation.viewmodel.PaymentUiState
import com.vaultx.user.presentation.viewmodel.PaymentViewModel

// ─────────────────────────────────────────────────────────────────────────────
// PaymentScreen — UPI QR display + UTR input + Firestore submission
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    planId:      String,
    onSubmitted: () -> Unit,
    onBack:      () -> Unit,
    viewModel:   PaymentViewModel = hiltViewModel()
) {
    val plan      by viewModel.plan.collectAsState()
    val utr       by viewModel.utr.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()
    val utrError  by viewModel.utrError.collectAsState()
    val upiId     by viewModel.upiId.collectAsState()
    val payeeName by viewModel.payeeName.collectAsState()
    val isLoading = uiState is PaymentUiState.Loading

    val context = LocalContext.current

    LaunchedEffect(planId) { viewModel.loadPlan(planId) }

    LaunchedEffect(uiState) {
        if (uiState is PaymentUiState.Submitted) {
            onSubmitted()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Complete Payment", style = MaterialTheme.typography.titleMedium) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Order summary ─────────────────────────────────────────────────
            plan?.let { p ->
                Surface(
                    shape    = ShapeCard,
                    color    = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Order Summary", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        OrderRow("Plan", p.name)
                        OrderRow("Duration", "${p.durationDays} days")
                        if (p.isRecommended) {
                            OrderRow("Tag", "RECOMMENDED",
                                valueColor = MaterialTheme.colorScheme.secondary)
                        }
                        
                        val appliedPromo by viewModel.appliedPromo.collectAsState()
                        var finalPrice = p.price
                        if (appliedPromo?.freePlanId == p.id) {
                            finalPrice = 0.0
                            OrderRow("Promo", "100% FREE", valueColor = MaterialTheme.colorScheme.primary)
                        } else if (appliedPromo?.discountPercent != null) {
                            val discountAmount = p.price * (appliedPromo!!.discountPercent!! / 100.0)
                            finalPrice = maxOf(0.0, p.price - discountAmount)
                            OrderRow("Promo", "-${appliedPromo!!.discountPercent}%", valueColor = MaterialTheme.colorScheme.primary)
                        }

                        HorizontalDivider(thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("₹${String.format(java.util.Locale.US, "%.2f", finalPrice)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ── Promo Code Input ─────────────────────────────────────────────
            val appliedPromo by viewModel.appliedPromo.collectAsState()
            val promoError by viewModel.promoError.collectAsState()
            var promoInput by remember { mutableStateOf("") }
            
            Surface(
                shape    = ShapeCard,
                color    = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Have a Promo Code?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VaultTextField(
                            value = promoInput,
                            onValueChange = { promoInput = it.uppercase() },
                            placeholder = "Enter code",
                            label = "Promo Code",
                            modifier = Modifier.weight(1f),
                            enabled = appliedPromo == null
                        )
                        Button(
                            onClick = { viewModel.applyPromoCode(promoInput) },
                            enabled = promoInput.isNotBlank() && appliedPromo == null,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(if (appliedPromo != null) "Applied" else "Apply")
                        }
                    }
                    if (promoError != null) {
                        Text(promoError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            val finalPrice = remember(plan, appliedPromo) {
                var price = plan?.price ?: 0.0
                if (appliedPromo?.freePlanId == plan?.id) {
                    price = 0.0
                } else if (appliedPromo?.discountPercent != null) {
                    val discountAmount = price * (appliedPromo!!.discountPercent!! / 100.0)
                    price = maxOf(0.0, price - discountAmount)
                }
                price
            }

            if (finalPrice > 0.0) {
                // ── UPI QR Code ───────────────────────────────────────────────────
                Surface(
                    shape    = ShapeCard,
                    color    = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Scan to Pay", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)

                        // ── QR Code generation ──
                        if (upiId.isNotBlank() && plan != null) {
                            val qrBitmap = remember(upiId, payeeName, finalPrice) {
                                generateUpiQrCode(upiId, payeeName, finalPrice.toString())
                            }
                            Surface(
                                shape    = ShapeCard,
                                color    = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(200.dp)
                            ) {
                                if (qrBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qrBitmap,
                                        contentDescription = "UPI QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            
                            OutlinedButton(onClick = {
                                val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode(payeeName)}&am=$finalPrice&cu=INR")
                                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignore or show toast if no UPI app
                                }
                            }) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open UPI App")
                            }
                            
                        } else {
                            CircularProgressIndicator()
                        }

                        // ── UPI details ───────────────────────────────────────────
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UpiDetailRow(label = "Pay to",  value = upiId)
                            UpiDetailRow(label = "Name",    value = payeeName)
                            plan?.let { p ->
                                UpiDetailRow(label = "Amount", value = "₹${String.format(java.util.Locale.US, "%.2f", finalPrice)}")
                            }
                        }

                        Text(
                            text      = "Open PhonePe / GPay / Paytm, scan the QR and complete the payment.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── UTR / Reference ID input ──────────────────────────────────────
                Surface(
                    shape    = ShapeCard,
                    color    = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Receipt, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Enter UTR / Reference ID",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "After payment, find the 12-digit UTR/Reference number in your payment app and enter it below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VaultTextField(
                            value           = utr,
                            onValueChange   = viewModel::onUtrChanged,
                            label           = "UTR / Reference ID",
                            placeholder     = "123456789012",
                            leadingIcon     = Icons.Outlined.Tag,
                            isError         = utrError != null,
                            errorMessage    = utrError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // ── Info notice ───────────────────────────────────────────────────
            if (finalPrice > 0.0) {
                Surface(
                    shape    = ShapeCard,
                    color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Outlined.Info, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text  = "After submission, your UTR will be reviewed within 24 hours. " +
                                    "Your account will be upgraded automatically once approved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Error banner ──────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState is PaymentUiState.Error) {
                ErrorBanner(message = (uiState as? PaymentUiState.Error)?.message ?: "")
            }

            // ── Submit button ─────────────────────────────────────────────────
            VaultButton(
                text      = if (finalPrice == 0.0) "Claim Free Premium" else "Submit for Approval",
                onClick   = {
                    viewModel.submitPayment(planId) { success, message ->
                        if (message != null) {
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isLoading = isLoading,
                enabled   = if (finalPrice == 0.0) true else utr.length == 12
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrderRow(
    label:      String,
    value:      String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
    }
}

@Composable
private fun UpiDetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

fun generateUpiQrCode(upiId: String, payeeName: String, amount: String): ImageBitmap? {
    val upiString = "upi://pay?pa=$upiId&pn=${android.net.Uri.encode(payeeName)}&am=$amount&cu=INR"
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(upiString, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
