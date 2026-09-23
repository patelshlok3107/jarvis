package com.shlok.jarvis.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import com.shlok.jarvis.ui.theme.ObsidianColors
import com.shlok.jarvis.ui.theme.ObsidianRounded
import com.shlok.jarvis.ui.theme.ObsidianSpacing
import com.shlok.jarvis.ui.theme.ObsidianTypography
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreenPremium(
    prefs: JarvisPreferences,
    onOpenSettings: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(JarvisStatus.AVAILABLE) }
    var smartMode by remember { mutableStateOf(SmartMode.AVAILABLE) }
    var userName by remember { mutableStateOf("Shlok") }
    var upcoming by remember { mutableStateOf<com.shlok.jarvis.engine.ScheduledMode?>(null) }
    var micGranted by remember { mutableStateOf(false) }
    var screeningGranted by remember { mutableStateOf(false) }
    var isVoiceAvailable by remember { mutableStateOf(false) }
    var wakeState by remember { mutableStateOf("READY") }

    // Collect real state
    LaunchedEffect(Unit) {
        try { prefs.statusFlow.collectLatest { s -> status = s; smartMode = SmartMode.fromStatus(s) } } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try {
            val d = prefs.dataFlow.first()
            userName = d[PrefKeys.USER_NAME] ?: "Shlok"
        } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try { com.shlok.jarvis.storage.ScheduledModeStore.flow(ctx).collectLatest { list -> upcoming = list.firstOrNull() } } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        micGranted = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        screeningGranted = PermissionManager.isCallScreeningGranted(ctx)
        try { isVoiceAvailable = com.shlok.jarvis.voice.SpeechRecognizerWakeWordEngine().isAvailable(ctx) } catch (_: Exception) {}
        try { com.shlok.jarvis.storage.VoiceDiagnostics.stateFlow(ctx).collectLatest { wakeState = it } } catch (_: Exception) {}
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val infinite = rememberInfiniteTransition(label = "orb")
    val rotationOuter by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "rotOuter")
    val rotationInner by infinite.animateFloat(360f, 0f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "rotInner")
    val pulse by infinite.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    Column(
        Modifier.fillMaxSize().background(ObsidianColors.Background).verticalScroll(rememberScrollState()).padding(horizontal = ObsidianSpacing.Margin).padding(top = 12.dp, bottom = 12.dp)
    ) {
        // Top greeting & sync
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(greeting, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm, letterSpacing = 0.5.sp)
                Text(userName, color = ObsidianColors.OnSurface, style = ObsidianTypography.HeadlineMd, fontWeight = FontWeight.Light)
            }
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerLow).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text("99.8% SYNC", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Orb section
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(192.dp).clickable { onOpenAssistant() },
                contentAlignment = Alignment.Center
            ) {
                // Ambient glow
                Box(
                    Modifier.size(208.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer.copy(alpha = 0.08f))
                )
                // Outer rings with rotation
                Canvas(Modifier.fillMaxSize().graphicsLayer(rotationZ = rotationOuter)) {
                    val accent = ObsidianColors.PrimaryContainer
                    val muted = ObsidianColors.OutlineVariant
                    drawCircle(muted.copy(alpha = 0.6f), style = Stroke(width = 0.75.dp.toPx()))
                    drawCircle(muted.copy(alpha = 0.4f), radius = size.minDimension / 2 * 0.85f, style = Stroke(width = 0.5.dp.toPx()))
                    drawCircle(accent.copy(alpha = 0.5f), radius = size.minDimension / 2 * 0.77f, style = Stroke(width = 1.dp.toPx()))
                }
                Canvas(Modifier.size(144.dp).graphicsLayer(rotationZ = rotationInner)) {
                    val accent = ObsidianColors.PrimaryContainer
                    val muted = ObsidianColors.OutlineVariant
                    drawCircle(muted.copy(alpha = 0.5f), style = Stroke(width = 0.75.dp.toPx()))
                    // Arc for accent
                    drawArc(accent.copy(alpha = 0.6f), -90f, 120f, false, style = Stroke(width = 1.25.dp.toPx()))
                }
                // Core nucleus with pulse
                Box(
                    Modifier.size(96.dp).graphicsLayer(scaleX = pulse, scaleY = pulse).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(ObsidianColors.SurfaceContainerLowest, ObsidianColors.OnPrimaryContainer, ObsidianColors.PrimaryContainer)))
                        .border(1.dp, ObsidianColors.PrimaryContainer.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(ObsidianColors.SurfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(16.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("J.A.R.V.I.S", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text(
                    smartMode.displayName,
                    color = ObsidianColors.Primary,
                    style = ObsidianTypography.LabelMd,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (smartMode) {
                    SmartMode.AVAILABLE -> "“How can I help you, $userName?”"
                    SmartMode.BUSY -> "“Handling your calls, $userName.”"
                    SmartMode.EXAM -> "“Exam mode active — silence enforced.”"
                    SmartMode.MEETING -> "“In a meeting — screening calls.”"
                    SmartMode.SLEEPING, SmartMode.RESTING -> "“Resting — quiet handling.”"
                    else -> "“${smartMode.displayName} mode active.”"
                },
                color = ObsidianColors.OnSurface,
                style = ObsidianTypography.HeadlineSm,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(Modifier.height(20.dp))

        // Quick Modes
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Quick Modes", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                Text("TACTILE OVERRIDE", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(SmartMode.AVAILABLE, SmartMode.BUSY, SmartMode.DND, SmartMode.DRIVING, SmartMode.EXAM, SmartMode.SLEEPING)
                modes.forEach { mode ->
                    val selected = smartMode == mode
                    val bg = if (selected) ObsidianColors.PrimaryContainer else ObsidianColors.SurfaceContainerLow
                    val contentColor = if (selected) ObsidianColors.OnPrimaryContainer else ObsidianColors.OnSurfaceVariant
                    val dotColor = if (selected) ObsidianColors.OnPrimaryContainer else ObsidianColors.OutlineVariant.copy(alpha = 0.6f)
                    Surface(
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(ObsidianRounded.Pill),
                        color = bg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ObsidianColors.PrimaryContainer.copy(alpha = 0.4f) else ObsidianColors.Hairline),
                        onClick = {
                            scope.launch {
                                try {
                                    prefs.setStatus(mode.status)
                                    com.shlok.jarvis.storage.JarvisLogger.log(ctx, "MODE_SET", mode.name)
                                    TtsManager.speak(mode.status.spokenAck)
                                } catch (_: Exception) {}
                            }
                        }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                            Text(mode.displayName, color = contentColor, style = ObsidianTypography.LabelMd)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // JARVIS STATUS card
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Memory, null, tint = ObsidianColors.Primary, modifier = Modifier.size(16.dp))
                    Text("JARVIS STATUS", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerLowest).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                    Text("OPERATIONAL", color = ObsidianColors.Primary, style = ObsidianTypography.LabelSm)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Voice Assistant
            StatusRow(
                icon = Icons.Default.Mic,
                title = "Voice Assistant",
                subtitle = if (micGranted && isVoiceAvailable) "Neural NLP engine active" else "Microphone permission required",
                badge = if (micGranted && isVoiceAvailable) "ON" else "OFF",
                badgeActive = micGranted && isVoiceAvailable
            )
            Spacer(Modifier.height(8.dp))
            StatusRow(
                icon = Icons.Default.Phone,
                title = "Call Assistant",
                subtitle = if (screeningGranted) "Screening & Busy intercept" else "Setup required",
                badge = if (screeningGranted) "ON" else "OFF",
                badgeActive = screeningGranted
            )
            Spacer(Modifier.height(8.dp))
            StatusRow(
                icon = Icons.Default.RecordVoiceOver,
                title = "Wake Word",
                subtitle = "“Hey JARVIS” primed",
                badge = if (wakeState.contains("READY") || wakeState.contains("WAKE")) "READY" else "OFF",
                badgeActive = wakeState.contains("READY") || wakeState.contains("WAKE")
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerLowest.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Verified, null, tint = ObsidianColors.Primary, modifier = Modifier.size(16.dp))
                    Text("Everything is ready and synchronized", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                }
                Text("42ms LATENCY", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Upcoming Timeline
        val hasUpcoming = upcoming != null
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Upcoming Timeline", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                    Text("AUTOMATION READY", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Memory, null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (hasUpcoming) upcoming!!.mode.displayName else "Board Meeting",
                            color = ObsidianColors.OnSurface,
                            style = ObsidianTypography.BodyMd,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (hasUpcoming) {
                                val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(upcoming!!.startTime))
                                fmt
                            } else "10:30 PM",
                            color = ObsidianColors.OnSurfaceVariant,
                            style = ObsidianTypography.LabelSm
                        )
                    }
                    Text(
                        if (hasUpcoming) "Auto-${upcoming!!.mode.displayName} will engage • Call guardian intercepts active"
                        else "Auto-Busy will engage • Call guardian intercepts active",
                        color = ObsidianColors.OnSurfaceVariant,
                        style = ObsidianTypography.BodySm,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, badge: String, badgeActive: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(ObsidianColors.SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (badgeActive) ObsidianColors.Primary else ObsidianColors.Secondary, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
            }
        }
        Box(
            Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(if (badgeActive) ObsidianColors.PrimaryContainer.copy(alpha = 0.15f) else ObsidianColors.SurfaceContainerHigh).padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(badge, color = if (badgeActive) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
        }
    }
}
