package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NotifyColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = NotifyBackground,
    primaryContainer = SpotifyGreen.copy(alpha = 0.2f),
    onPrimaryContainer = SpotifyGreenBright,
    secondary = ElectricCyan,
    onSecondary = NotifyBackground,
    secondaryContainer = ElectricCyan.copy(alpha = 0.2f),
    onSecondaryContainer = ElectricCyan,
    tertiary = NeonPurple,
    background = NotifyBackground,
    onBackground = TextPrimary,
    surface = NotifySurface,
    onSurface = TextPrimary,
    surfaceVariant = NotifySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NotifySurfaceElevated
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Notify is committed to a rich, sleek, pitch-black Spotify dark aesthetic
    val colorScheme = NotifyColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = NotifyBackground.toArgb()
                window.navigationBarColor = NotifyBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
