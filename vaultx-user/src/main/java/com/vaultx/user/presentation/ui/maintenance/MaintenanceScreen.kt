package com.vaultx.user.presentation.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultx.user.presentation.theme.ShapeFull
import com.vaultx.user.presentation.ui.components.VaultButton

@Composable
fun MaintenanceScreen(
    message: String,
    onRetry: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Under Maintenance",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            VaultButton(
                text = "Check Again",
                onClick = onRetry
            )
        }
    }
}
