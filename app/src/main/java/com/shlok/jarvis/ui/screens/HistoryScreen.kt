package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.ui.theme.ObsidianColors
import com.shlok.jarvis.ui.theme.ObsidianRounded
import com.shlok.jarvis.ui.theme.ObsidianSpacing
import com.shlok.jarvis.ui.theme.ObsidianTypography
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    var history by remember { mutableStateOf(emptyList<com.shlok.jarvis.data.CallHistoryEntry>()) }
    var logs by remember { mutableStateOf(emptyList<com.shlok.jarvis.storage.LogEntry>()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") } // all, voice, calls, modes, security

    LaunchedEffect(Unit) { try { HistoryRepository.historyFlow(ctx).collectLatest { history = it } } catch (_: Exception) {} }
    LaunchedEffect(Unit) { try { JarvisLogger.getLogsFlow(ctx).collectLatest { logs = it.take(20) } } catch (_: Exception) {} }

    // Combine into timeline items
    data class TimelineItem(val time: Long, val category: String, val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val badge: String, val color: androidx.compose.ui.graphics.Color)

    val timeline = remember(history, logs, query, filter) {
        val items = mutableListOf<TimelineItem>()
        history.forEach { e ->
            val isSim = e.isSimulated
            items.add(
                TimelineItem(
                    time = e.timestampMillis,
                    category = "calls",
                    title = if (e.disposition == com.shlok.jarvis.data.CallDisposition.HANDLED_BY_JARVIS) "CALL INTERCEPTED & TRANSCRIBED" else "CALL ${e.disposition}",
                    subtitle = "Caller: ${e.callerName ?: e.callerNumber}. ${e.jarvisResponse ?: ""} ${if (isSim) "(Simulated)" else ""}",
                    icon = Icons.Default.Call,
                    badge = if (e.disposition == com.shlok.jarvis.data.CallDisposition.BLOCKED) "SPAM" else "VOICE AGENT",
                    color = if (e.disposition == com.shlok.jarvis.data.CallDisposition.BLOCKED) ObsidianColors.Error else ObsidianColors.Secondary
                )
            )
        }
        logs.forEach { l ->
            val cat = when {
                l.event.contains("MODE") -> "modes"
                l.event.contains("VOICE") || l.event.contains("WAKE") -> "voice"
                l.event.contains("CALL") -> "calls"
                l.event.contains("SECURITY") || l.event.contains("SYSTEM") -> "security"
                else -> "security"
            }
            items.add(
                TimelineItem(
                    time = l.timestamp,
                    category = cat,
                    title = l.event.replace("_", " "),
                    subtitle = l.details,
                    icon = when (cat) {
                        "modes" -> Icons.Default.Person
                        "voice" -> Icons.Default.GraphicEq
                        "security" -> Icons.Default.Memory
                        else -> Icons.Default.History
                    },
                    badge = when (cat) {
                        "modes" -> "AUTONOMOUS"
                        "voice" -> "QUERY"
                        "security" -> "TELEMETRY"
                        else -> "LOG"
                    },
                    color = when (cat) {
                        "modes" -> ObsidianColors.Primary
                        "voice" -> ObsidianColors.Primary
                        "security" -> ObsidianColors.OutlineVariant
                        else -> ObsidianColors.Secondary
                    }
                )
            )
        }
        var filtered = items.sortedByDescending { it.time }
        if (filter != "all") filtered = filtered.filter { it.category == filter }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = filtered.filter { it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q) }
        }
        filtered
    }

    Column(
        Modifier.fillMaxSize().background(ObsidianColors.Background).verticalScroll(rememberScrollState()).padding(horizontal = ObsidianSpacing.Margin).padding(top = 12.dp, bottom = 12.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("AUDIT ARCHIVE", color = ObsidianColors.Primary, style = ObsidianTypography.LabelMd, letterSpacing = 1.5.sp)
                Text("Activity Timeline", color = ObsidianColors.OnSurface, style = ObsidianTypography.HeadlineSm, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text("LIVE SYNC", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Audit log of all autonomous intelligence decisions and environmental intercepts.", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
        Spacer(Modifier.height(16.dp))

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter by keyword, entity, or action...", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.BodySm) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = ObsidianColors.OutlineVariant, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(16.dp)) }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianColors.SurfaceContainerLow,
                unfocusedContainerColor = ObsidianColors.SurfaceContainerLow,
                focusedBorderColor = ObsidianColors.Hairline,
                unfocusedBorderColor = ObsidianColors.Hairline,
                cursorColor = ObsidianColors.Primary
            ),
            textStyle = ObsidianTypography.BodySm.copy(color = ObsidianColors.OnSurface)
        )

        Spacer(Modifier.height(12.dp))

        // Filter pills
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all" to "All", "voice" to "Voice", "calls" to "Calls", "modes" to "Modes", "security" to "Security").forEach { (key, label) ->
                val selected = filter == key
                Surface(
                    shape = RoundedCornerShape(ObsidianRounded.Pill),
                    color = if (selected) ObsidianColors.SurfaceContainerHigh else ObsidianColors.SurfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ObsidianColors.PrimaryContainer.copy(alpha = 0.3f) else ObsidianColors.Hairline),
                    onClick = { filter = key }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape).background(if (selected) ObsidianColors.PrimaryContainer else Color.Transparent)
                        )
                        Text(label, color = if (selected) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Timeline with vertical line
        if (timeline.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No activity yet. Try a voice command or enable a mode.", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
            }
        } else {
            Box(Modifier.fillMaxWidth()) {
                // Vertical line
                Box(
                    Modifier.padding(start = 19.dp).fillMaxHeight().width(2.dp).background(ObsidianColors.SurfaceContainerHigh).align(Alignment.TopStart)
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    timeline.take(12).forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(20.dp))
                            }
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(item.time)),
                                        color = when (item.category) {
                                            "calls" -> ObsidianColors.Secondary
                                            "voice" -> ObsidianColors.Primary
                                            else -> ObsidianColors.Primary
                                        },
                                        style = ObsidianTypography.LabelSm,
                                        letterSpacing = 1.sp
                                    )
                                    Box(
                                        Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(
                                            when (item.category) {
                                                "modes" -> ObsidianColors.PrimaryContainer.copy(alpha = 0.1f)
                                                "calls" -> ObsidianColors.SurfaceContainerHigh
                                                else -> ObsidianColors.SurfaceContainerHigh
                                            }
                                        ).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(item.badge, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(item.title, color = ObsidianColors.OnSurface, style = ObsidianTypography.HeadlineSm, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(item.subtitle, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm, maxLines = 3)
                                if (item.category == "calls") {
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {}) {
                                            Icon(Icons.Default.Description, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("View Summary", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
                                        }
                                        Text("01:42 duration", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
                                    }
                                }
                                if (item.category == "security") {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ObsidianColors.SurfaceContainerHighest)) {
                                            Box(Modifier.fillMaxHeight().fillMaxWidth(0.99f).clip(RoundedCornerShape(2.dp)).background(ObsidianColors.Primary))
                                        }
                                        Text("99.4%", color = ObsidianColors.Primary, style = ObsidianTypography.LabelSm)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Footer
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Lock, null, tint = ObsidianColors.OutlineVariant, modifier = Modifier.size(14.dp))
                Text("End of log for today • Encrypted on-device", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("HASH: 4F92A8...901B", color = ObsidianColors.SurfaceContainerHighest, style = ObsidianTypography.LabelSm)
        }

        Spacer(Modifier.height(16.dp))
    }
}
