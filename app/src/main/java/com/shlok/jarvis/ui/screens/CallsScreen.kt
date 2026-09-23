package com.shlok.jarvis.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.ui.theme.ObsidianColors
import com.shlok.jarvis.ui.theme.ObsidianRounded
import com.shlok.jarvis.ui.theme.ObsidianSpacing
import com.shlok.jarvis.ui.theme.ObsidianTypography
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CallsScreen() {
    val ctx = LocalContext.current
    var isScreening by remember { mutableStateOf(PermissionManager.isCallScreeningGranted(ctx)) }
    var isDialer by remember { mutableStateOf(PermissionManager.isDefaultDialer(ctx)) }
    var currentMode by remember { mutableStateOf(SmartMode.AVAILABLE) }
    var filter by remember { mutableStateOf("all") } // all, screened, transcribed
    var history by remember { mutableStateOf(emptyList<com.shlok.jarvis.data.CallHistoryEntry>()) }
    var scheduledEnd by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        isScreening = PermissionManager.isCallScreeningGranted(ctx)
        isDialer = PermissionManager.isDefaultDialer(ctx)
    }

    LaunchedEffect(Unit) {
        try {
            val prefs = JarvisPreferences(ctx)
            prefs.statusFlow.collectLatest { status -> currentMode = SmartMode.fromStatus(status) }
        } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try {
            val list = com.shlok.jarvis.storage.ScheduledModeStore.flow(ctx)
            list.collectLatest { l -> scheduledEnd = l.firstOrNull()?.let { java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(it.endTime)) } }
        } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try { HistoryRepository.historyFlow(ctx).collectLatest { history = it } } catch (_: Exception) {}
    }

    val filtered = when (filter) {
        "screened" -> history.filter { it.disposition == com.shlok.jarvis.data.CallDisposition.HANDLED_BY_JARVIS && !it.isSimulated }
        "transcribed" -> history.filter { it.transcript != null }
        else -> history
    }

    Column(
        Modifier.fillMaxSize().background(ObsidianColors.Background).verticalScroll(rememberScrollState()).padding(horizontal = ObsidianSpacing.Margin).padding(top = 12.dp, bottom = 12.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Call Assistant", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text("AGENT LIVE", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Hero card
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
        ) {
            Box(Modifier.size(128.dp).align(Alignment.TopEnd).clip(CircleShape).background(ObsidianColors.PrimaryContainer.copy(alpha = 0.06f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Autonomous Call Screening", color = ObsidianColors.OnSurface, style = ObsidianTypography.HeadlineSm, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                        Text(
                            if (isScreening) "Active • Filtering unknown & scheduled calls" else "Inactive • Setup required",
                            color = ObsidianColors.OnSurfaceVariant,
                            style = ObsidianTypography.BodySm
                        )
                    }
                }
                // Toggle
                val toggleOn = isScreening
                Surface(
                    modifier = Modifier.size(width = 48.dp, height = 28.dp),
                    shape = RoundedCornerShape(ObsidianRounded.Pill),
                    color = if (toggleOn) ObsidianColors.SurfaceContainerHighest else ObsidianColors.SurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianColors.Hairline),
                    onClick = {
                        if (!toggleOn) {
                            PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) }
                                ?: ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        }
                        refresh()
                    }
                ) {
                    Box(Modifier.fillMaxSize().padding(2.dp), contentAlignment = if (toggleOn) Alignment.CenterEnd else Alignment.CenterStart) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(if (toggleOn) ObsidianColors.PrimaryContainer else ObsidianColors.Outline),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, null, tint = if (toggleOn) ObsidianColors.OnPrimaryContainer else ObsidianColors.Surface, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Current Call Handling
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Current Call Handling", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                Row(
                    Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(12.dp))
                    Text(scheduledEnd?.let { "Until $it" } ?: "No schedule", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainer).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DoNotDisturbOn, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("${currentMode.displayName} Mode Engaged", color = ObsidianColors.OnSurface, style = ObsidianTypography.HeadlineSm, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (currentMode) {
                                SmartMode.BUSY -> "Calendar sync: Executive Sync Call"
                                SmartMode.MEETING -> "In a meeting"
                                SmartMode.EXAM -> "Exam protection active"
                                SmartMode.DRIVING -> "Driving protection"
                                else -> "Call intercept active"
                            },
                            color = ObsidianColors.OnSurfaceVariant,
                            style = ObsidianTypography.BodySm
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(8.dp)).padding(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = ObsidianColors.Primary, modifier = Modifier.size(14.dp))
                            Text("JARVIS SPOKEN RESPONSE", color = ObsidianColors.Primary, style = ObsidianTypography.LabelSm)
                        }
                        Text("NEURAL TTS • CALM", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (currentMode) {
                            SmartMode.BUSY -> "“Shlok is currently busy in a meeting. Please state your urgent message and JARVIS will summarize it immediately.”"
                            SmartMode.EXAM -> "“Hello, I'm JARVIS, Shlok's AI assistant. He's currently in an exam and can't take the call. Please leave a message.”"
                            SmartMode.DRIVING -> "“Hello, I'm JARVIS, Shlok's AI assistant. He's currently driving and can't safely answer the call. Please leave a message.”"
                            SmartMode.SLEEPING -> "“Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message.”"
                            SmartMode.MEETING -> "“Hello, I'm JARVIS, Shlok's AI assistant. He's currently in a meeting and can't take the call. Please leave a message.”"
                            else -> "“${if (currentMode == SmartMode.AVAILABLE) "Shlok is available." else currentMode.callResponse}”"
                        },
                        color = ObsidianColors.OnSurface,
                        style = ObsidianTypography.BodyMd
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(2, 3, 2, 4, 1, 3, 2).forEachIndexed { idx, h ->
                            Box(
                                Modifier.width(2.dp).height((h * 4).dp).clip(RoundedCornerShape(1.dp)).background(
                                    if (idx % 2 == 0) ObsidianColors.PrimaryContainer.copy(alpha = 0.6f) else ObsidianColors.PrimaryContainer
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianColors.SurfaceContainerHigh,
                        onClick = {}
                    ) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.EditNote, null, tint = ObsidianColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Voice Script", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodySm)
                        }
                    }
                    Surface(modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianColors.SurfaceContainerHigh,
                        onClick = {}
                    ) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Tune, null, tint = ObsidianColors.Secondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Override Rules", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodySm)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Recent Handled Calls
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Handled Calls", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                Text("Auto-Archiving in 48h", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All (${history.size})", "screened" to "Screened (${history.count { it.disposition == com.shlok.jarvis.data.CallDisposition.HANDLED_BY_JARVIS }})", "transcribed" to "Transcribed (${history.count { it.transcript != null }})").forEach { (key, label) ->
                    val selected = filter == key
                    Surface(
                        shape = RoundedCornerShape(ObsidianRounded.Pill),
                        color = if (selected) ObsidianColors.SurfaceContainerHigh else ObsidianColors.SurfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ObsidianColors.PrimaryContainer.copy(alpha = 0.3f) else ObsidianColors.Hairline),
                        onClick = { filter = key }
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selected) Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                            Text(label, color = if (selected) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No handled calls yet. Enable a mode and call this phone to test.", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                }
            } else {
                filtered.take(6).forEach { entry ->
                    val fmt = try { SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(entry.timestampMillis)) } catch (_: Exception) { "" }
                    val initials = (entry.callerName ?: entry.callerNumber).take(2).uppercase()
                    val isEmergency = entry.callerName?.contains("Elena") == true
                    val isSpam = entry.disposition == com.shlok.jarvis.data.CallDisposition.BLOCKED
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(
                                        when {
                                            isSpam -> ObsidianColors.ErrorContainer
                                            isEmergency -> ObsidianColors.SurfaceContainerHigh
                                            else -> ObsidianColors.SecondaryContainer
                                        }
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSpam) Icon(Icons.Default.CallEnd, null, tint = ObsidianColors.OnErrorContainer, modifier = Modifier.size(18.dp))
                                    else Text(initials, color = if (isEmergency) ObsidianColors.OnSurface else ObsidianColors.OnSecondaryContainer, style = ObsidianTypography.HeadlineSm, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(entry.callerName ?: entry.callerNumber, color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("${entry.callerName?.let { "" } ?: ""}Today • $fmt", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val badgeLabel: String
                                val badgeColor: androidx.compose.ui.graphics.Color
                                val badgeBg: androidx.compose.ui.graphics.Color
                                when {
                                    isSpam -> { badgeLabel = "SPAM BLOCKED"; badgeColor = ObsidianColors.Error; badgeBg = ObsidianColors.ErrorContainer }
                                    isEmergency -> { badgeLabel = "EMERGENCY PASSED"; badgeColor = ObsidianColors.Secondary; badgeBg = ObsidianColors.SurfaceContainerHigh }
                                    else -> { badgeLabel = "HANDLED BY JARVIS"; badgeColor = ObsidianColors.Primary; badgeBg = ObsidianColors.SurfaceContainerHighest }
                                }
                                Box(
                                    Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(badgeBg.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(badgeLabel, color = badgeColor, style = ObsidianTypography.LabelSm)
                                }
                                if (!isSpam) Text(entry.status.displayName, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainer).padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = ObsidianColors.Primary, modifier = Modifier.size(14.dp))
                                    Text("AI Summary", color = ObsidianColors.Primary, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(entry.jarvisResponse ?: "No summary", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodySm)
                                if (entry.transcript != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Transcript: ${entry.transcript}", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                                }
                                if (entry.isSimulated) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Simulated — grant Default Dialer for real answering", color = ObsidianColors.Secondary, style = ObsidianTypography.LabelSm)
                                }
                            }
                        }
                        if (!isSpam) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = {},
                                    shape = RoundedCornerShape(ObsidianRounded.Pill),
                                    color = ObsidianColors.SurfaceContainerHigh
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.PlayArrow, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                                        Text("Audio (0:24)", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodySm)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Read Transcript", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                                    Icon(Icons.Default.ChevronRight, null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(ObsidianRounded.Xl),
            color = ObsidianColors.SurfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianColors.Hairline)
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Add, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Configure Custom Call Rule", color = ObsidianColors.OnSurface, style = ObsidianTypography.LabelMd, letterSpacing = 1.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
