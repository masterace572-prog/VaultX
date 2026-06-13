package com.vaultx.user.presentation.ui.premium

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.Plan
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.PremiumViewModel

// ─────────────────────────────────────────────────────────────────────────────
// PremiumScreen — plan cards, feature list, upgrade CTA
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onSelectPlan: (String) -> Unit,
    onBack:       () -> Unit,
    viewModel:    PremiumViewModel = hiltViewModel()
) {
    val plans     by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userTier  by viewModel.userTier.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Go Premium", style = MaterialTheme.typography.titleMedium) },
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
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Crown header ──────────────────────────────────────────────────
            item {
                Column(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = DarkBadgePremium.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.WorkspacePremium, null,
                                tint = DarkBadgePremium, modifier = Modifier.size(40.dp))
                        }
                    }
                    Text(
                        text  = "VaultX Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text  = "Unlock unlimited accounts, cloud sync, and more.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Active premium status
                    if (userTier?.isPremium == true) {
                        Surface(shape = ShapeCard,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp))
                                Text(
                                    text  = "You're Premium · ${userTier?.daysLeft} days remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // ── Plan cards ────────────────────────────────────────────────────
            item {
                Text(
                    text  = "CHOOSE A PLAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                items(2) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)
                        .shimmerEffect())
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan     = plan,
                        onClick  = { 
                            val appliedPromoState = viewModel.appliedPromo.value
                            if (appliedPromoState?.freePlanId == plan.id) {
                                viewModel.submitFreePromoCode(appliedPromoState, plan) { success, msg ->
                                    android.widget.Toast.makeText(context, msg ?: "Unknown error", android.widget.Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        onBack()
                                    }
                                }
                            } else {
                                onSelectPlan(plan.id) 
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Plan card ──────────────────────────────────────────────────────────────────
@Composable
private fun PlanCard(plan: Plan, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        shape    = ShapeCard,
        color    = MaterialTheme.colorScheme.surface,
        border   = if (plan.isRecommended) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        ) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Header row: duration badge + name + price ──
            Row(
                modifier              = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Duration badge
                Surface(
                    shape = ShapeChip,
                    color = DarkBadgePremium.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = plan.durationDays.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = DarkBadgePremium
                        )
                        Text("days", style = MaterialTheme.typography.labelSmall, color = DarkBadgePremium)
                    }
                }

                // Name + pricing
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(plan.name, style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (plan.isRecommended) {
                            Surface(shape = ShapeBadge,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)) {
                                Text("BEST VALUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (plan.basePrice != null && plan.basePrice > plan.price) {
                            Text(
                                text = "₹${String.format(java.util.Locale.US, "%.0f", plan.basePrice)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("₹${String.format(java.util.Locale.US, "%.2f", plan.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Icon(Icons.Outlined.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }

            // ── Feature list ──
            if (plan.features.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plan.features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Check, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
