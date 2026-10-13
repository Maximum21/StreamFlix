package com.asadraza.streamflix.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme (Netflix-style)
 */
private val DarkColorScheme = darkColorScheme(
    primary = Red,
    onPrimary = TextPrimary,
    primaryContainer = RedDark,
    onPrimaryContainer = TextPrimary,

    secondary = TextSecondary,
    onSecondary = TextPrimary,

    tertiary = AccentBlue,
    onTertiary = TextPrimary,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = TextPrimary
)

/**
 * StreamFlix Theme
 *
 * Consistent theming across app
 * Wrap entire app in this theme
 * MainActivity
 *
 * @param darkTheme Whether to use dark theme (always true for Netflix style)
 * @param content The content to theme
 */
@Composable
fun StreamFlixTheme(
    darkTheme: Boolean = true, // Always dark for Netflix style
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}