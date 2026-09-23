package com.shlok.jarvis.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.manager.ModeManager
import com.shlok.jarvis.service.VoiceAssistantService
import com.shlok.jarvis.storage.JarvisPreferences
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
    var lastReply by remember { mutableStateOf<String?>(null) }
    var errorBanner by remember { mutableStateOf<String?>(null) }
    var micLevel by remember { mutableStateOf("0") }
    var voiceState by remember { mutableStateOf("STOPPED") }
    var isAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { prefs.statusFlow.collectLatest { status = it } } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try { com.shlok.jarvis.storage.VoiceDiagnostics.micLevelFlow(ctx).collectLatest { micLevel = it } } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try { com.shlok.jarvis.storage.VoiceDiagnostics.stateFlow(ctx).collectLatest { voiceState = it } } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try { isAvailable = SpeechRecognizerWakeWordEngine().isAvailable(ctx) } catch (_: Exception) {}
    }

    val micGranted = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    Column(
        Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("JARVIS ASSISTANT", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("Voice control centre", color = Color.White.copy(0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))

        errorBanner?.let {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), modifier = Modifier.fillMaxWidth().padding(bottom=12.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF8A80))
                    Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { errorBanner = null }) { Text("Dismiss", color = Color.White) }
                }
            }
        }

        // Voice status
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Voice Assistant", color = Color.White, fontSize = 14.sp)
                        Text(
                            when {
                                !micGranted -> "⚠ Microphone permission required"
                                !isAvailable -> "⚠ Speech recognition unavailable on this device"
                                voiceState.contains("ERROR") -> "⚠ $voiceState"
                                wakeEnabled -> "● Listening for \"Hey JARVIS\""
                                listening -> "● Listening..."
                                else -> "Ready — tap to speak"
                            },
                            color = when {
                                !micGranted || !isAvailable -> Color(0xFFFFC107)
                                wakeEnabled || listening -> Color(0xFF00E676)
                                else -> Color.White.copy(0.6f)
                            },
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        if (wakeEnabled || listening) Icons.Default.Mic else Icons.Default.MicNone,
                        null,
                        tint = if (wakeEnabled || listening) Color(0xFF00E676) else Color.White.copy(0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Wake word toggle
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Wake Word — \"Hey JARVIS\"", color = Color.White, fontSize = 13.sp)
                        Text("Continuous listening (battery-intensive). Works on home screen too.", color = Color.White.copy(0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = wakeEnabled,
                        onCheckedChange = { enabled ->
                            if (!micGranted) {
                                errorBanner = "Microphone permission required. Grant it in Setup Wizard."
                                return@Switch
                            }
                            if (!isAvailable) {
                                errorBanner = "Speech recognition not available on this device."
                                return@Switch
                            }
                            wakeEnabled = enabled
                            if (enabled) {
                                val ok = VoiceAssistantService.tryStart(ctx, wakeWord = true)
                                if (!ok) {
                                    errorBanner = "Android blocked microphone background access. Open app to enable voice, or check battery optimization."
                                    wakeEnabled = false
                                } else {
                                    lastReply = "Wake word enabled — try: \"Hey JARVIS\" (return to home screen and say it)"
                                }
                            } else {
                                VoiceAssistantService.stop(ctx)
                                lastReply = "Wake word disabled"
                            }
                        }
                    )
                }

                if (wakeEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Mic level:", color = Color.White.copy(0.5f), fontSize = 10.sp)
                        LinearProgressIndicator(
                            progress = (micLevel.toIntOrNull() ?: 0) / 10f,
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = Color(0xFF00E676),
                            trackColor = Color(0xFF1E2A3A)
                        )
                        Text(micLevel, color = Color.White.copy(0.6f), fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("State: $voiceState", color = Color.White.copy(0.4f), fontSize = 10.sp)
                }

                Spacer(Modifier.height(12.dp))
                // Tap to speak (one-shot) — reliable default
                Button(
                    onClick = {
                        if (listening) return@Button
                        if (!micGranted) {
                            errorBanner = "Microphone permission denied — voice offline. Enable in Setup Wizard."
                            return@Button
                        }
                        listening = true
                        errorBanner = null
                        scope.launch {
                            try {
                                // Use one-shot via service or direct STT
                                // Prefer service one-shot for consistent TTS handling
                                val ok = VoiceAssistantService.tryStart(ctx, wakeWord = false)
                                if (!ok) {
                                    // Fallback to direct STT
                                    val stt = com.shlok.jarvis.voice.SttManager(ctx)
                                    if (!stt.isAvailable()) {
                                        errorBanner = "Speech recognition not available."
                                        listening = false
                                        return@launch
                                    }
                                    var got = false
                                    stt.listenFlow().collect { utterance ->
                                        if (got) return@collect
                                        got = true
                                        val modeManager = ModeManager(prefs)
                                        val res = modeManager.setModeByUtterance(ctx, utterance)
                                        val reply = res.getOrNull()?.ack ?: "Sorry, didn't catch that. Try \"I'm busy\"."
                                        lastReply = "\"$utterance\" → $reply"
                                        TtsManager.speak(reply)
                                        listening = false
                                    }
                                    kotlinx.coroutines.delay(7500)
                                    if (!got) {
                                        listening = false
                                        lastReply = "Didn't catch that — try again."
                                    }
                                } else {
                                    // Service will handle, we just show feedback
                                    lastReply = "Listening... (service)"
                                    kotlinx.coroutines.delay(6000)
                                    listening = false
                                }
                            } catch (e: Exception) {
                                listening = false
                                errorBanner = e.message ?: "Voice error"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (listening) Color(0xFFFF3B30) else Color(0xFF00E5FF), contentColor = if (listening) Color.White else Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(if (listening) Icons.Default.Mic else Icons.Default.MicNone, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (listening) "Listening…" else "Tap to speak  •  \"Hey JARVIS, I'm busy\"")
                }

                lastReply?.let {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
                        Text(it, color = Color.White.copy(0.7f), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Without wake word: tap the button above. With wake word: return to Home Screen and say \"Hey JARVIS\" — JARVIS replies \"Yes?\" then listen for command.", color = Color.White.copy(0.3f), fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick mode test (without voice)
        Text("Quick mode test (no voice needed)", color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Current: ${status.displayName} • ${status.subtitle}", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SmartMode.BUSY, SmartMode.EXAM, SmartMode.DRIVING, SmartMode.MEETING).forEach { m ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                scope.launch {
                                    val mm = ModeManager(prefs)
                                    val r = mm.setMode(ctx, m)
                                    r.onSuccess { lastReply = "Set to ${m.displayName}: ${mm.generateCallResponse(m)}" }
                                    TtsManager.speak(r.getOrNull()?.let { mm.generateCallResponse(m) } ?: "Mode set")
                                }
                            },
                            label = { Text(m.displayName.take(5), fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Voice examples
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Try these:", color = Color(0xFF00E5FF), fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "\"Hey JARVIS, I'm busy.\" → BUSY",
                    "\"Hey JARVIS, I'm in an exam.\" → EXAM",
                    "\"Hey JARVIS, I'm going to sleep.\" → SLEEPING",
                    "\"Hey JARVIS, I'm in a meeting.\" → MEETING",
                    "\"Hey JARVIS, I'm available now.\" → AVAILABLE",
                    "\"Hey JARVIS, I have an exam in 10 minutes.\" → schedules EXAM"
                ).forEach { Text("• $it", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(vertical=2.dp)) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Offline behavior note
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Offline behavior", color = Color.White.copy(0.7f), fontSize = 11.sp)
                Text("Without internet: wake word ✓, basic commands ✓, mode switching ✓, scheduling ✓, TTS ✓. Cloud AI shows \"Offline\" but basic JARVIS still works. App never crashes offline.", color = Color.White.copy(0.5f), fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
            }
        }
    }
}
