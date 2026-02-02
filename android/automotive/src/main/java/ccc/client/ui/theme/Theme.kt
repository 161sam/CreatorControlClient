package ccc.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CccDarkPrimary,
    onPrimary = CccDarkOnPrimary,
    secondary = CccDarkSecondary,
    onSecondary = CccDarkOnSecondary,
    background = CccDarkBackground,
    onBackground = CccDarkOnBackground,
    surface = CccDarkSurface,
    onSurface = CccDarkOnSurface,
    surfaceVariant = CccDarkSurfaceVariant,
    outline = CccDarkOutline,
    error = CccDarkError,
    onError = CccDarkOnError
)

@Composable
fun CccTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CccTypography,
        content = content
    )
}
