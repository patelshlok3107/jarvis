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

// Obsidian Intelligence — STITCH authoritative palette
private val ObsidianScheme = darkColorScheme(
    primary = ObsidianColors.PrimaryContainer, // #38E1FF accent
    onPrimary = ObsidianColors.OnPrimaryContainer,
    primaryContainer = ObsidianColors.PrimaryContainer,
    onPrimaryContainer = ObsidianColors.OnPrimaryContainer,
    secondary = ObsidianColors.Secondary,
    onSecondary = ObsidianColors.OnSecondary,
    secondaryContainer = ObsidianColors.SecondaryContainer,
    onSecondaryContainer = ObsidianColors.OnSecondaryContainer,
    tertiary = ObsidianColors.Tertiary,
    onTertiary = ObsidianColors.OnTertiary,
    background = ObsidianColors.Background, // #121315
    onBackground = ObsidianColors.OnBackground,
    surface = ObsidianColors.Surface, // #121315
    onSurface = ObsidianColors.OnSurface,
    surfaceVariant = ObsidianColors.SurfaceVariant,
    onSurfaceVariant = ObsidianColors.OnSurfaceVariant,
    outline = ObsidianColors.Outline,
    outlineVariant = ObsidianColors.OutlineVariant,
    error = ObsidianColors.Error,
    onError = ObsidianColors.OnError,
    errorContainer = ObsidianColors.ErrorContainer,
    onErrorContainer = ObsidianColors.OnErrorContainer,
    surfaceDim = ObsidianColors.SurfaceDim,
    surfaceBright = ObsidianColors.SurfaceBright,
    surfaceContainer = ObsidianColors.SurfaceContainer,
    surfaceContainerHigh = ObsidianColors.SurfaceContainerHigh,
    surfaceContainerHighest = ObsidianColors.SurfaceContainerHighest,
    surfaceContainerLow = ObsidianColors.SurfaceContainerLow,
    surfaceContainerLowest = ObsidianColors.SurfaceContainerLowest,
    inversePrimary = ObsidianColors.InversePrimary,
    inverseSurface = ObsidianColors.InverseSurface,
    inverseOnSurface = ObsidianColors.InverseOnSurface,
    surfaceTint = ObsidianColors.SurfaceTint,
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ObsidianScheme, content = content)
}

@Composable
fun ObsidianTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ObsidianScheme, content = content)
}
