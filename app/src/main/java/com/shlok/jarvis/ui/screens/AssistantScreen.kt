package com.shlok.jarvis.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.manager.ModeManager
import com.shlok.jarvis.service.VoiceAssistantService
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.ui.theme.ObsidianColors
import com.shlok.jarvis.ui.theme.ObsidianRounded
import com.shlok.jarvis.ui.theme.ObsidianSpacing
import com.shlok.jarvis.ui.theme.ObsidianTypography
import com.shlok.jarvis.voice.SpeechRecognizerWakeWordEngine
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(prefs: JarvisPreferences) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(JarvisStatus.AVAILABLE) }
    var listening by remember { mutableStateOf(false) }
    var wakeEnabled by remember { mutableStateOf(false) }
    var lastUtterance by remember { mutableStateOf<String?>(null) }
    var lastReply by remember { mutableStateOf<String?>(null) }
    var errorBanner by remember { mutableStateOf<String?>(null) }
    var micLevel by remember { mutableStateOf("0") }
    var voiceState by remember { mutableStateOf("READY") }
    var isAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { try { prefs.statusFlow.collectLatest { status = it } } catch (_: Exception) {} }
    LaunchedEffect(Unit) { try { com.shlok.jarvis.storage.VoiceDiagnostics.micLevelFlow(ctx).collectLatest { micLevel = it } } catch (_: Exception) {} }
    LaunchedEffect(Unit) { try { com.shlok.jarvis.storage.VoiceDiagnostics.stateFlow(ctx).collectLatest { voiceState = it } } catch (_: Exception) {} }
    LaunchedEffect(Unit) { try { isAvailable = SpeechRecognizerWakeWordEngine().isAvailable(ctx) } catch (_: Exception) {} }

    val micGranted = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val isListening = listening || wakeEnabled

    // Waveform animation
    val infinite = rememberInfiniteTransition(label = "wave")
    val waveHeights = listOf(
        infinite.animateFloat(2f, 8f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1"),
        infinite.animateFloat(4f, 12f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2"),
        infinite.animateFloat(6f, 18f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3"),
        infinite.animateFloat(8f, 22f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h4"),
        infinite.animateFloat(5f, 14f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h5"),
        infinite.animateFloat(7f, 16f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h6"),
        infinite.animateFloat(3f, 9f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h7"),
        infinite.animateFloat(1f, 5f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h8"),
    )

    Column(
        Modifier.fillMaxSize().background(ObsidianColors.Background).verticalScroll(rememberScrollState()).padding(horizontal = ObsidianSpacing.Margin).padding(top = 12.dp, bottom = 12.dp)
    ) {
        // Header neural core
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Mic, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(20.dp))
                Column {
                    Text("JARVIS NEURAL CORE", color = ObsidianColors.OnSurface, style = ObsidianTypography.LabelMd, letterSpacing = 1.sp)
                    Text("SUB-HERTZ v4.9 ACTIVE", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                }
            }
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text(if (isListening) "LISTENING" else "READY", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Waveform orb area
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(140.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer.copy(alpha = 0.05f)))
                Box(Modifier.size(112.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer.copy(alpha = 0.08f)))
                Box(Modifier.size(80.dp).clip(CircleShape).background(ObsidianColors.SecondaryContainer.copy(alpha = 0.2f)))
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(ObsidianColors.SurfaceContainerHigh)
                        .border(1.dp, ObsidianColors.PrimaryContainer.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            // Waveform nodes
            Row(
                Modifier.height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                waveHeights.forEachIndexed { idx, h ->
                    val height = if (isListening) h.value.dp else (2 + idx % 3).dp
                    Box(
                        Modifier.width(4.dp).height(height).clip(RoundedCornerShape(2.dp)).background(
                            when (idx) {
                                2, 3, 5 -> ObsidianColors.PrimaryContainer
                                1, 4 -> ObsidianColors.PrimaryContainer.copy(alpha = 0.7f)
                                else -> ObsidianColors.PrimaryContainer.copy(alpha = 0.4f)
                            }
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.TipsAndUpdates, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(14.dp))
                Text("“Hey JARVIS, what's on my schedule?”", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
            }
        }

        Spacer(Modifier.height(16.dp))

        errorBanner?.let {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ObsidianColors.ErrorContainer).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(it, color = ObsidianColors.OnErrorContainer, style = ObsidianTypography.BodySm, modifier = Modifier.weight(1f))
                TextButton(onClick = { errorBanner = null }) { Text("Dismiss", color = ObsidianColors.OnErrorContainer) }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Synapse log
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SYNAPSE LOG // RECENT", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                Text("LATENCY: 42ms", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
            }
            Spacer(Modifier.height(8.dp))

            // YOU bubble
            if (lastUtterance != null) {
                Column(Modifier.fillMaxWidth().padding(start = 32.dp), horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("YOU", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                        Text("now", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl).copy(topEnd = androidx.compose.foundation.shape.CornerSize(4.dp))).background(ObsidianColors.SurfaceContainerLow).padding(16.dp)
                    ) {
                        Text(lastUtterance!!, color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd)
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                // Demo YOU
                Column(Modifier.fillMaxWidth().padding(start = 32.dp), horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("YOU", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                        Text("21:30", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl).copy(topEnd = androidx.compose.foundation.shape.CornerSize(4.dp))).background(ObsidianColors.SurfaceContainerLow).padding(16.dp)
                    ) {
                        Text("I'm busy for the next hour with product review.", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // JARVIS bubble
            Column(Modifier.fillMaxWidth().padding(end = 32.dp), horizontalAlignment = Alignment.Start) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                    Text("JARVIS", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
                    Text(if (lastReply != null) "now" else "21:30", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl).copy(topStart = androidx.compose.foundation.shape.CornerSize(4.dp))).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl).copy(topStart = androidx.compose.foundation.shape.CornerSize(4.dp))).padding(16.dp)
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(ObsidianColors.PrimaryContainer))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            lastReply ?: "Understood. Busy Mode has been activated until 10:30 PM. Incoming phone calls will be gracefully screened, and emergency VIP contacts have been alerted.",
                            color = ObsidianColors.OnSurface,
                            style = ObsidianTypography.BodyMd
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(ObsidianRounded.Pill),
                                color = ObsidianColors.SurfaceContainerHigh,
                                onClick = {}
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Timelapse, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(14.dp))
                                    Text("Adjust Duration", color = ObsidianColors.OnSurface, style = ObsidianTypography.LabelSm)
                                }
                            }
                            Surface(modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(ObsidianRounded.Pill),
                                color = ObsidianColors.SurfaceContainerHigh,
                                onClick = {}
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Description, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(14.dp))
                                    Text("View Screening Script", color = ObsidianColors.OnSurface, style = ObsidianTypography.LabelSm)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mic controls
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerHigh,
                onClick = {}
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Keyboard, null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer.copy(alpha = 0.2f)))
                Surface(modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = ObsidianColors.PrimaryContainer,
                    shadowElevation = 8.dp,
                    onClick = {
                        if (!micGranted) {
                            errorBanner = "Microphone permission required."
                            return@Surface
                        }
                        if (!isAvailable) {
                            errorBanner = "Speech recognition unavailable."
                            return@Surface
                        }
                        if (listening) return@Surface
                        listening = true
                        errorBanner = null
                        scope.launch {
                            try {
                                val ok = VoiceAssistantService.tryStart(ctx, wakeWord = false)
                                if (!ok) {
                                    val engine = SpeechRecognizerWakeWordEngine()
                                    val utterance = engine.listenForCommand(ctx)
                                    if (utterance != null) {
                                        lastUtterance = utterance
                                        val mm = ModeManager(prefs)
                                        val res = mm.setModeByUtterance(ctx, utterance)
                                        val reply = res.getOrNull()?.ack ?: "Sorry, didn't catch that."
                                        lastReply = reply
                                        TtsManager.speak(reply)
                                    } else {
                                        errorBanner = "Didn't catch that — try again."
                                    }
                                    listening = false
                                } else {
                                    lastUtterance = "Listening..."
                                    kotlinx.coroutines.delay(6000)
                                    listening = false
                                }
                            } catch (e: Exception) {
                                listening = false
                                errorBanner = e.message
                            }
                        }
                    }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, null, tint = ObsidianColors.OnPrimary, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Surface(modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerHigh,
                onClick = { wakeEnabled = !wakeEnabled; if (wakeEnabled) VoiceAssistantService.tryStart(ctx, true) else VoiceAssistantService.stop(ctx) }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, null, tint = if (wakeEnabled) ObsidianColors.PrimaryContainer else ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
        Text("Tap to speak • Hold for continuous dialogue", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))

        Spacer(Modifier.height(16.dp))

        // Quick mode test without voice
        Text("Quick mode test", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(com.shlok.jarvis.engine.SmartMode.BUSY, com.shlok.jarvis.engine.SmartMode.EXAM, com.shlok.jarvis.engine.SmartMode.DRIVING, com.shlok.jarvis.engine.SmartMode.MEETING).forEach { m ->
                Surface(
                    shape = RoundedCornerShape(ObsidianRounded.Pill),
                    color = ObsidianColors.SurfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianColors.Hairline),
                    onClick = {
                        scope.launch {
                            val mm = ModeManager(prefs)
                            val r = mm.setMode(ctx, m)
                            r.onSuccess { lastReply = mm.generateCallResponse(m) }
                            TtsManager.speak(mm.generateCallResponse(m))
                        }
                    }
                ) {
                    Text(m.displayName.take(5), color = ObsidianColors.OnSurface, style = ObsidianTypography.LabelSm, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
