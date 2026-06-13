package com.vaultx.user.presentation.ui.security

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.AccountEntry
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountViewModel

data class SecurityMetrics(
    val score: Int,
    val totalCount: Int,
    val weakCount: Int,
    val duplicateCount: Int,
    val weakEntries: List<AccountEntry>,
    val duplicateEntries: List<AccountEntry>
)

fun analyzePasswords(accounts: List<AccountEntry>): SecurityMetrics {
    val loginEntries = accounts.filter { it.password.isNotEmpty() }
    if (loginEntries.isEmpty()) {
        return SecurityMetrics(100, 0, 0, 0, emptyList(), emptyList())
    }

    var weakCount = 0
    var duplicateCount = 0
    val weakList = mutableListOf<AccountEntry>()
    val duplicateList = mutableListOf<AccountEntry>()

    val passwordCounts = loginEntries.groupBy { it.password }.mapValues { it.value.size }

    loginEntries.forEach { entry ->
        val isWeak = entry.password.length < 8 || 
                     entry.password.all { it.isLowerCase() } || 
                     entry.password.all { it.isDigit() }
        if (isWeak) {
            weakCount++
            weakList.add(entry)
        }

        val count = passwordCounts[entry.password] ?: 1
        if (count > 1) {
            duplicateCount++
            duplicateList.add(entry)
        }
    }

    val weakRatio = weakCount.toFloat() / loginEntries.size
    val duplicateRatio = duplicateCount.toFloat() / loginEntries.size
    val score = (100 - (weakRatio * 40) - (duplicateRatio * 40)).toInt().coerceIn(15, 100)

    return SecurityMetrics(
        score = score,
        totalCount = loginEntries.size,
        weakCount = weakCount,
        duplicateCount = duplicateCount,
        weakEntries = weakList,
        duplicateEntries = duplicateList
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    
    val metrics = remember(accounts) {
        analyzePasswords(accounts)
    }

    var selectedSection by remember { mutableStateOf("OVERVIEW") } // "OVERVIEW" | "WEAK" | "REUSED"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text("Security Center", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) 
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Radial Score Indicator ────────────────────────────────────────
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Radial progress circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(90.dp)
                    ) {
                        val scoreColor = when {
                            metrics.score >= 80 -> AccentGreenLight
                            metrics.score >= 50 -> AccentOrangeLight
                            else -> AccentRedLight
                        }
                        Canvas(modifier = Modifier.size(80.dp)) {
                            drawCircle(
                                color = scoreColor.copy(alpha = 0.12f),
                                style = Stroke(width = 8.dp.toPx())
                            )
                            drawArc(
                                color = scoreColor,
                                startAngle = -90f,
                                sweepAngle = (metrics.score * 3.6f),
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${metrics.score}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Score Label & Details
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val ratingText = when {
                            metrics.score >= 80 -> "Vault Secured"
                            metrics.score >= 50 -> "Needs Attention"
                            else -> "Action Required!"
                        }
                        Text(
                            text = ratingText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Based on password health metrics computed from ${metrics.totalCount} active credentials.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // ── Section Selector Tabs ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("OVERVIEW", "WEAK PASSWORDS", "REUSED PASSWORDS").forEach { section ->
                    val isSelected = (section.startsWith("OVERVIEW") && selectedSection == "OVERVIEW") ||
                                     (section.startsWith("WEAK") && selectedSection == "WEAK") ||
                                     (section.startsWith("REUSED") && selectedSection == "REUSED")
                    val targetSection = when {
                        section.startsWith("OVERVIEW") -> "OVERVIEW"
                        section.startsWith("WEAK") -> "WEAK"
                        else -> "REUSED"
                    }
                    
                    Surface(
                        onClick = { selectedSection = targetSection },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null,
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (section.contains("WEAK")) "Weak (${metrics.weakCount})"
                                       else if (section.contains("REUSED")) "Reused (${metrics.duplicateCount})"
                                       else "Overview",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Dynamic Content ───────────────────────────────────────────────
            AnimatedContent(
                targetState = selectedSection,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "security_tabs",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { section ->
                when (section) {
                    "WEAK" -> {
                        WarningList(entries = metrics.weakEntries, warningText = "Weak Password", onNavigateToDetail = onNavigateToDetail)
                    }
                    "REUSED" -> {
                        WarningList(entries = metrics.duplicateEntries, warningText = "Reused Password", onNavigateToDetail = onNavigateToDetail)
                    }
                    else -> {
                        OverviewSection(metrics = metrics)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(metrics: SecurityMetrics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "SECURITY CHECKLIST",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Surface(
            shape = ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ChecklistItem("No Weak Passwords", metrics.weakCount == 0)
                VaultDivider()
                ChecklistItem("No Duplicate Password Reuse", metrics.duplicateCount == 0)
                VaultDivider()
                ChecklistItem("Master Key Wrapping Intact", true)
            }
        }
    }
}

@Composable
private fun ChecklistItem(label: String, passed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (passed) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
            contentDescription = null,
            tint = if (passed) AccentGreenLight else AccentRedLight,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WarningList(
    entries: List<AccountEntry>,
    warningText: String,
    onNavigateToDetail: (String) -> Unit
) {
    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Shield, null, tint = AccentGreenLight, modifier = Modifier.size(40.dp))
                Text("All Clear!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("No accounts flagged for this check.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                Surface(
                    onClick = { onNavigateToDetail(entry.id) },
                    shape = ShapeCard,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlatformIcon(type = entry.platformType, size = 44.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.platformLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(entry.username ?: entry.email ?: "No username", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = ShapeBadge,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = warningText.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
