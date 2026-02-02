package ccc.client.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class TopLevelDestination(
    val route: String,
    val label: String
) {
    Home("home", "Home"),
    Browser("browser", "Browser"),
    Files("files", "Files"),
    Settings("settings", "Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CccApp(
    homeState: HomeUiState,
    diagnosticsProvider: () -> String,
    onRetry: () -> Unit,
    onOpenBrowserActivity: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: TopLevelDestination.Home.route

    fun navigateTo(destination: TopLevelDestination) {
        if (currentRoute == destination.route) return
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentRoute == TopLevelDestination.Browser.route) {
                        onOpenBrowserActivity()
                    } else {
                        navigateTo(TopLevelDestination.Browser)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Public, contentDescription = "Open Browser")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == TopLevelDestination.Home.route,
                    onClick = { navigateTo(TopLevelDestination.Home) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(TopLevelDestination.Home.label) }
                )
                NavigationBarItem(
                    selected = currentRoute == TopLevelDestination.Browser.route,
                    onClick = { navigateTo(TopLevelDestination.Browser) },
                    icon = { Icon(Icons.Default.Public, contentDescription = null) },
                    label = { Text(TopLevelDestination.Browser.label) }
                )
                NavigationBarItem(
                    selected = currentRoute == TopLevelDestination.Files.route,
                    onClick = { navigateTo(TopLevelDestination.Files) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text(TopLevelDestination.Files.label) }
                )
                NavigationBarItem(
                    selected = currentRoute == TopLevelDestination.Settings.route,
                    onClick = { navigateTo(TopLevelDestination.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(TopLevelDestination.Settings.label) }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(TopLevelDestination.Home.route) {
                HomeScreen(
                    state = homeState,
                    diagnosticsProvider = diagnosticsProvider,
                    onRetry = onRetry,
                    onOpenBrowserTab = { navigateTo(TopLevelDestination.Browser) }
                )
            }
            composable(TopLevelDestination.Browser.route) {
                BrowserScreen(onOpenBrowserActivity = onOpenBrowserActivity)
            }
            composable(TopLevelDestination.Files.route) {
                FilesScreen(onOpenBrowserActivity = onOpenBrowserActivity)
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
