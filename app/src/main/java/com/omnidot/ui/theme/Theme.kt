package com.omnidot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Teal, onPrimary = Color(0xFF0B1F1C),
    primaryContainer = Color(0xFF1F3A36), onPrimaryContainer = Color(0xFFCFEDE7),
    background = DarkBackground, onBackground = Color(0xFFE7EAEC),
    surface = DarkSurface, onSurface = Color(0xFFE7EAEC),
    surfaceVariant = DarkSurfaceHigh, onSurfaceVariant = DarkMuted,
    surfaceContainer = DarkSurface, surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline, outlineVariant = DarkOutline,
    error = Color(0xFFE5867A),
)

private val LightColors = lightColorScheme(
    primary = TealDeep, onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EEEA), onPrimaryContainer = Color(0xFF0E3B35),
    background = LightBackground, onBackground = Color(0xFF1B1F22),
    surface = LightSurface, onSurface = Color(0xFF1B1F22),
    surfaceVariant = LightSurfaceHigh, onSurfaceVariant = LightMuted,
    surfaceContainer = LightSurface, surfaceContainerHigh = LightSurfaceHigh,
    outline = LightOutline, outlineVariant = LightOutline,
    error = Color(0xFFB3261E),
)

@Composable
fun OmniDotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
