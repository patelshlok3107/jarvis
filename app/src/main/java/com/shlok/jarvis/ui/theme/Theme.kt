package com.shlok.jarvis.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDark = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    secondary = Color(0xFF0A84FF),
    background = Color(0xFF05070A),
    surface = Color(0xFF0E1218),
    onBackground = Color(0xFFE8F1FF),
    onSurface = Color(0xFFE8F1FF),
    error = Color(0xFFFF3B30),
    outline = Color(0xFF1E2A3A)
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JarvisDark, content = content)
}
