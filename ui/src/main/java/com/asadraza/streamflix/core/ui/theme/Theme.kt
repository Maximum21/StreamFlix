package com.asadraza.streamflix.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme (Netflix-style)
 */
private val DarkColorScheme =
    darkColorScheme(
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
 * Light scheme: the same brand red on a neutral light surface. Dynamic (wallpaper) colour is not
 * used so the brand stays consistent.
 */
private val LightColorScheme =
    lightColorScheme(
        primary = Red,
        onPrimary = Color.White,
        primaryContainer = RedLight,
        onPrimaryContainer = Color.White,
        secondary = LightTextSecondary,
        onSecondary = Color.White,
        tertiary = AccentBlue,
        onTertiary = Color.White,
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightTextSecondary,
        error = LightError,
        onError = Color.White
    )

/**
 * StreamFlix theme. Follows the system setting unless [darkTheme] is given; system bar styling is
 * configured in MainActivity.
 */
@Composable
fun StreamFlixTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
