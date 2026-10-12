package com.omnidot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorOrangeDeep = androidx.compose.ui.graphics.Color(0xFF8A3100)

private val OmniDotColors = darkColorScheme(
    primary = OmniOrange,
    onPrimary = PureWhite,
    primaryContainer = ColorOrangeDeep,
    onPrimaryContainer = PureWhite,
    background = OledBlack,
    onBackground = PureWhite,
    surface = SurfaceDark,
    onSurface = PureWhite,
    surfaceVariant = DotInactive,
    onSurfaceVariant = MutedText,
)

@Composable
fun OmniDotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OmniDotColors, content = content)
}

