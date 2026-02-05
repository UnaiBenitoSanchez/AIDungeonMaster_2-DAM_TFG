package com.example.aidungeonmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37), // Oro
    secondary = Color(0xFF8B4513), // Marrón cuero
    tertiary = Color(0xFF2E8B57), // Verde bosque
    background = Color(0xFF0A0A0A), // Negro carbón
    surface = Color(0xFF1A1A1A), // Gris oscuro
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE6D8B8), // Papiro/pergamino
    onSurface = Color(0xFFE6D8B8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B4513),
    secondary = Color(0xFFD4AF37),
    tertiary = Color(0xFF2E8B57),
    background = Color(0xFFF5F0E1), // Color pergamino
    surface = Color(0xFFFFF8E1),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun AIDungeonMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}