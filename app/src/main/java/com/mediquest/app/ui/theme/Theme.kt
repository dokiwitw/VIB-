package com.mediquest.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary   = NeonCyan,
    secondary = NeonLime,
    tertiary  = NeonMagenta,
    background = CyberBlack,
    surface    = CyberDark,
    onPrimary  = Color.Black,
    onSurface  = Color.White
)

@Composable
fun VIBTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content     = content
    )
}
