package com.pingpoint.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Signal,
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF0F5132),
    onPrimaryContainer = Color(0xFFB8F5D6),
    secondary = Sky,
    onSecondary = Color(0xFF003548),
    background = Ink,
    onBackground = Mist,
    surface = Color(0xFF0C1822),
    onSurface = Mist,
    surfaceVariant = Panel,
    onSurfaceVariant = Color(0xFFB7C7D3),
    error = Slow,
    onError = Color(0xFF3B0000),
    outline = Color(0xFF3D5160),
)

private val LightColorScheme = lightColorScheme(
    primary = SignalDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F5DE),
    onPrimaryContainer = Color(0xFF002113),
    secondary = SkyDark,
    onSecondary = Color.White,
    background = Color(0xFFF3F6F8),
    onBackground = Color(0xFF12202B),
    surface = Color.White,
    onSurface = Color(0xFF12202B),
    surfaceVariant = PanelLight,
    onSurfaceVariant = Color(0xFF3E5360),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFFB7C6D0),
)

@Composable
fun PingPointTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
