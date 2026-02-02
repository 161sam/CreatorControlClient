package ccc.client.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ccc.client.ui.components.CccCard
import ccc.client.ui.components.MonospaceBlock
import ccc.client.ui.components.PrimaryButton
import ccc.client.ui.components.SecondaryButton
import ccc.client.ui.components.StatusPill

@Composable
fun HomeScreen(
    state: HomeUiState,
    diagnosticsProvider: () -> String,
    onRetry: () -> Unit,
    onOpenBrowserTab: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var diagnosticsExpanded by remember { mutableStateOf(false) }
    var diagnosticsText by remember { mutableStateOf("") }

    val statusColor = when (state.statusLevel) {
        StatusLevel.Ok -> MaterialTheme.colorScheme.primary
        StatusLevel.Error -> MaterialTheme.colorScheme.error
        StatusLevel.Neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Server Status", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = statusColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(text = state.statusText, color = statusColor)
                    if (state.isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loading…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        item {
            CccCard(title = "Connection") {
                state.detailsText.lines().forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            CccCard(title = "Server Details") {
                if (state.messageLines.isEmpty()) {
                    Text(
                        text = "No details yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    state.messageLines.forEach { line ->
                        Text(text = line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            CccCard(title = "Actions") {
                PrimaryButton(
                    text = "Open Browser",
                    onClick = onOpenBrowserTab,
                    enabled = !state.isLoading
                )
                SecondaryButton(
                    text = "Retry",
                    onClick = onRetry,
                    enabled = !state.isLoading
                )
                SecondaryButton(
                    text = "Copy diagnostics",
                    onClick = {
                        val payload = diagnosticsProvider()
                        diagnosticsText = payload
                        clipboardManager.setText(AnnotatedString(payload))
                        Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT)
                            .show()
                    },
                    enabled = !state.isLoading
                )
            }
        }
        item {
            CccCard(title = "Diagnostics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diagnostics payload",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = {
                            diagnosticsExpanded = !diagnosticsExpanded
                            if (diagnosticsExpanded) {
                                diagnosticsText = diagnosticsProvider()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (diagnosticsExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (diagnosticsExpanded) "Hide" else "Show")
                    }
                }
                if (diagnosticsExpanded) {
                    MonospaceBlock(text = diagnosticsText)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
