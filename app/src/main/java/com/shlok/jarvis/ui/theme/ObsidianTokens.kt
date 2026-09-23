package com.shlok.jarvis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Obsidian Intelligence Design System — extracted from stitch_jarvis_android_redesign_system
 * Source of truth: obsidian_intelligence/DESIGN.md + HTML tailwind config
 * Colors, typography, spacing, shapes, elevation — centralized, no hard-coded values scattered.
 */
object ObsidianColors {
    // Primary surfaces — void blacks
    val Surface = Color(0xFF121315)
    val SurfaceDim = Color(0xFF121315)
    val SurfaceBright = Color(0xFF38393B)
    val SurfaceContainerLowest = Color(0xFF0D0E10)
    val SurfaceContainerLow = Color(0xFF1A1C1D)
    val SurfaceContainer = Color(0xFF1E2021)
    val SurfaceContainerHigh = Color(0xFF292A2B)
    val SurfaceContainerHighest = Color(0xFF343536)
    val SurfaceVariant = Color(0xFF343536)
    val Background = Color(0xFF121315)
    val BackgroundVoid = Color(0xFF050607) // DESIGN.md Base Void
    val SurfaceLevel1 = Color(0xFF0B0D0F)
    val SurfaceLevel2 = Color(0xFF101316)

    // Text hierarchy
    val OnSurface = Color(0xFFE3E2E4)
    val OnSurfaceVariant = Color(0xFFBBC9CD)
    val OnBackground = Color(0xFFE3E2E4)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFF8B9198)
    val TextTertiary = Color(0xFF4B535B)
    val TextMono = Color(0xFF859397)

    // Structural
    val Outline = Color(0xFF859397)
    val OutlineVariant = Color(0xFF3C494C)
    val Hairline = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
    val HairlineStrong = Color(0x0FFFFFFF) // rgba(255,255,255,0.04)
    val InverseSurface = Color(0xFFE3E2E4)
    val InverseOnSurface = Color(0xFF2F3032)

    // Accents — bioluminescent, strictly for live feedback
    val Primary = Color(0xFFC2F3FF)
    val OnPrimary = Color(0xFF00363F)
    val PrimaryContainer = Color(0xFF38E1FF)
    val OnPrimaryContainer = Color(0xFF00616F)
    val PrimaryFixed = Color(0xFFA6EEFF)
    val PrimaryFixedDim = Color(0xFF27D9F7)
    val SurfaceTint = Color(0xFF27D9F7)
    val OnPrimaryFixed = Color(0xFF001F25)
    val OnPrimaryFixedVariant = Color(0xFF004E5B)
    val InversePrimary = Color(0xFF006878)

    val Secondary = Color(0xFF93CCFF)
    val OnSecondary = Color(0xFF003351)
    val SecondaryContainer = Color(0xFF3198DC)
    val OnSecondaryContainer = Color(0xFF002C47)
    val SecondaryFixed = Color(0xFFCCE5FF)
    val SecondaryFixedDim = Color(0xFF93CCFF)

    val Tertiary = Color(0xFFD6EEFF)
    val OnTertiary = Color(0xFF00354A)
    val TertiaryContainer = Color(0xFF92D7FF)
    val OnTertiaryContainer = Color(0xFF005E81)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // Accent gradients
    val AccentCyan = Color(0xFF38E1FF)
    val AccentBlue = Color(0xFF38BDF8)
    val AccentDeep = Color(0xFF0284C7)
}

object ObsidianSpacing {
    val Gutter = 16.dp
    val Margin = 20.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
    val Xxl = 40.dp
}

object ObsidianRounded {
    val Sm = 4.dp
    val Default = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Full = 9999.dp
    val Card = 20.dp // cards primary modules 20-24px
    val CardLarge = 24.dp
    val Pill = 9999.dp
}

object ObsidianElevation {
    val Card = 0.dp // tonal, not shadow
    val Floating = 8.dp
}

// Typography — Inter for conversational, JetBrains Mono for technical
private val InterFamily = FontFamily.SansSerif // Use system sans as Inter proxy; add R.font.inter if bundled
private val MonoFamily = FontFamily.Monospace // JetBrains Mono proxy

object ObsidianTypography {
    val DisplayLg = TextStyle(fontFamily = InterFamily, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, lineHeight = 44.sp, letterSpacing = (-0.72).sp)
    val HeadlineLg = TextStyle(fontFamily = InterFamily, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp, letterSpacing = (-0.42).sp)
    val HeadlineMd = TextStyle(fontFamily = InterFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp, letterSpacing = (-0.22).sp)
    val HeadlineSm = TextStyle(fontFamily = InterFamily, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, letterSpacing = (-0.09).sp)
    val BodyLg = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
    val BodyMd = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.07.sp)
    val BodySm = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.12.sp)
    val LabelMd = TextStyle(fontFamily = MonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.96.sp)
    val LabelSm = TextStyle(fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 1.2.sp)
    // Custom for status badges
    val MonoCaps = TextStyle(fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
}
