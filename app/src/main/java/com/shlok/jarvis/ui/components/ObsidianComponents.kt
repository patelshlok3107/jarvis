package com.shlok.jarvis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shlok.jarvis.ui.theme.ObsidianColors
import com.shlok.jarvis.ui.theme.ObsidianRounded
import com.shlok.jarvis.ui.theme.ObsidianSpacing

@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    rounded: Dp = ObsidianRounded.Card,
    containerColor: Color = ObsidianColors.SurfaceContainerLow,
    borderColor: Color = ObsidianColors.Hairline,
    contentPadding: Dp = ObsidianSpacing.Md,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(rounded),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun ObsidianPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val bg = if (selected) ObsidianColors.PrimaryContainer else ObsidianColors.SurfaceContainerLow
    val contentColor = if (selected) ObsidianColors.OnPrimaryContainer else ObsidianColors.OnSurfaceVariant
    val border = if (selected) ObsidianColors.PrimaryContainer.copy(alpha = 0.4f) else ObsidianColors.Hairline
    val dotColor = if (selected) ObsidianColors.OnPrimaryContainer else ObsidianColors.OutlineVariant.copy(alpha = 0.6f)

    Surface(
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(ObsidianRounded.Pill),
        color = bg,
        border = BorderStroke(1.dp, border),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(dotColor)
                )
            }
            Text(text, color = contentColor, style = com.shlok.jarvis.ui.theme.ObsidianTypography.LabelMd, maxLines = 1)
        }
    }
}

@Composable
fun ObsidianStatusDot(active: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) ObsidianColors.PrimaryContainer else ObsidianColors.OutlineVariant
    Box(
        modifier
            .size(6.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
fun ObsidianHairlineDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(ObsidianColors.HairlineStrong))
}
