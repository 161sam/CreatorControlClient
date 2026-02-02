package ccc.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ccc.client.ui.components.CccCard
import ccc.client.ui.components.PrimaryButton

@Composable
fun BrowserScreen(onOpenBrowserActivity: () -> Unit) {
    PlaceholderScreen(
        title = "Browser",
        description = "Open the full Browser activity to explore server commands and capabilities.",
        buttonLabel = "Open Browser Activity",
        onClick = onOpenBrowserActivity
    )
}

@Composable
fun FilesScreen(onOpenBrowserActivity: () -> Unit) {
    PlaceholderScreen(
        title = "Files",
        description = "Files are managed in the Browser activity for now.",
        buttonLabel = "Open Browser Activity",
        onClick = onOpenBrowserActivity
    )
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CccCard(title = "Settings") {
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CccCard(title = title) {
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(text = buttonLabel, onClick = onClick)
        }
    }
}
