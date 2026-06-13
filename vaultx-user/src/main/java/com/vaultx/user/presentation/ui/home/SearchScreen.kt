package com.vaultx.user.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultx.user.data.model.AccountEntry
import com.vaultx.user.presentation.theme.*
import com.vaultx.user.presentation.ui.components.*
import com.vaultx.user.presentation.viewmodel.AccountViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SearchScreen — real-time search with highlighted matches
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToDetail: (String) -> Unit,
    onBack:             () -> Unit,
    viewModel:          AccountViewModel = hiltViewModel()
) {
    val query     by viewModel.searchQuery.collectAsState()
    val results   by viewModel.accounts.collectAsState()
    val focusReq  = remember { FocusRequester() }
    val focusMgr  = LocalFocusManager.current

    // Auto-focus search field on open
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        focusReq.requestFocus()
    }

    // Clear search on exit
    DisposableEffect(Unit) {
        onDispose { viewModel.onSearchQueryChanged("") }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }

                    // Inline search field — no label, just placeholder
                    OutlinedTextField(
                        value         = query,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder   = {
                            Text("Search accounts…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        },
                        leadingIcon   = {
                            Icon(Icons.Outlined.Search, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                        },
                        trailingIcon  = {
                            AnimatedVisibility(
                                visible = query.isNotEmpty(),
                                enter   = fadeIn(tween(150)),
                                exit    = fadeOut(tween(150))
                            ) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Outlined.Close, "Clear",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine    = true,
                        shape         = ShapeFull,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor      = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                            cursorColor             = MaterialTheme.colorScheme.primary,
                        ),
                        textStyle     = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusMgr.clearFocus() }),
                        modifier      = Modifier
                            .weight(1f)
                            .focusRequester(focusReq)
                    )
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState  = Pair(query, results),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label        = "search_results"
        ) { (q, entries) ->
            when {
                // Empty query — prompt
                q.isBlank() -> {
                    SearchEmptyPrompt()
                }
                // No results
                entries.isEmpty() -> {
                    SearchNoResults(query = q)
                }
                // Results list
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize().padding(padding),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text  = "${entries.size} result${if (entries.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(entries, key = { it.id }) { entry ->
                            SearchResultCard(
                                entry   = entry,
                                query   = q,
                                onClick = { onNavigateToDetail(entry.id) }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

// ── Search result card with highlighted match ──────────────────────────────────
@Composable
private fun SearchResultCard(
    entry:   AccountEntry,
    query:   String,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        shape    = ShapeCard,
        color    = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PlatformIcon(type = entry.platformType)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Highlighted platform label
                Text(
                    text  = highlightText(entry.platformLabel, query),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                // Highlighted subtitle
                val subtitle = entry.username ?: entry.email ?: entry.platformType.displayName
                Text(
                    text  = highlightText(subtitle, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp))
        }
    }
}

// ── Highlight matching substrings in yellow ───────────────────────────────────
@Composable
private fun highlightText(
    text:  String,
    query: String
): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val lower = text.lowercase()
    val qLower = query.lowercase()
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val idx = lower.indexOf(qLower, start)
            if (idx < 0) { append(text.substring(start)); break }
            append(text.substring(start, idx))
            withStyle(SpanStyle(
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )) {
                append(text.substring(idx, idx + query.length))
            }
            start = idx + query.length
        }
    }
}

// ── Empty states ──────────────────────────────────────────────────────────────
@Composable
private fun SearchEmptyPrompt() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Search, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp))
            Text("Search by platform or username",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchNoResults(query: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.SearchOff, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp))
            Text("No results for \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Try searching by platform name or username",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
