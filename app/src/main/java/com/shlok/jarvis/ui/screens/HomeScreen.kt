package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.data.CallDisposition
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.service.JarvisForegroundService
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import com.shlok.jarvis.ui.components.JarvisCore
import com.shlok.jarvis.voice.SttManager
import com.shlok.jarvis.voice.TtsManager
import com.shlok.jarvis.voice.VoiceCommandProcessor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    prefs: JarvisPreferences,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenOnboarding: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(JarvisStatus.AVAILABLE) }
    var listening by remember { mutableStateOf(false) }
    var lastReply by remember { mutableStateOf<String?>(null) }
    var errorBanner by remember { mutableStateOf<String?>(null) }
    var serviceOn by remember { mutableStateOf(false) }
    var historyPreview by remember { mutableStateOf(emptyList<com.shlok.jarvis.data.CallHistoryEntry>()) }

    LaunchedEffect(Unit) {
        prefs.statusFlow.collectLatest { status = it }
    }
    LaunchedEffect(Unit) {
        // check service status via prefs or assume on after start
        HistoryRepository.historyFlow(ctx).collectLatest { historyPreview = it.take(3) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(16.dp)) {
        // Top bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("JARVIS", color = Color.White.copy(0.5f), fontSize = 11.sp, letterSpacing = 3.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onOpenOnboarding) { Icon(Icons.Default.Info, null, tint = Color.White.copy(0.6f)) }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = Color.White.copy(0.6f)) }
            }
        }

        errorBanner?.let {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), modifier = Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(10.dp), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        JarvisCore(status, listening, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(16.dp))

        // Quick status chips
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            listOf(JarvisStatus.AVAILABLE, JarvisStatus.BUSY, JarvisStatus.MEETING, JarvisStatus.SLEEPING).forEach { s ->
                FilterChip(
                    selected = status==s,
                    onClick = { scope.launch { prefs.setStatus(s); TtsManager.speak(s.spokenAck) } },
                    label = { Text(s.displayName, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF), selectedLabelColor = Color.Black)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Voice command button — lightweight, battery efficient (tap-to-talk)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218))) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Voice Command", color = Color.White.copy(0.7f), fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (listening) return@Button
                        listening = true
                        scope.launch {
                            try {
                                val stt = SttManager(ctx)
                                if (!stt.isAvailable()) {
                                    errorBanner = "Speech recognition not available on this device."
                                    listening = false; return@launch
                                }
                                // Use callbackFlow — collect first result
                                var got = false
                                stt.listenFlow().collect { utterance ->
                                    if (got) return@collect
                                    got = true
                                    val cmd = VoiceCommandProcessor.parse(utterance)
                                    val res = VoiceCommandProcessor.replyFor(cmd)
                                    lastReply = "\"$utterance\" → ${res.reply}"
                                    res.newStatus?.let { prefs.setStatus(it) }
                                    TtsManager.speak(res.reply)
                                    listening = false
                                }
                                // timeout fallback
                                kotlinx.coroutines.delay(7000)
                                if (!got) { listening = false; lastReply = "Didn't catch that — try again." }
                            } catch (e: Exception) {
                                listening = false; errorBanner = e.message
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(listening) Color(0xFFFF3B30) else Color(0xFF00E5FF), contentColor = if(listening) Color.White else Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(if(listening) Icons.Default.Mic else Icons.Default.MicNone, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if(listening) "Listening…" else "Tap to speak  •  \"Hey Jarvis, I'm busy\"")
                }
                lastReply?.let { Text(it, color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(top=8.dp)) }
                Text("Wake-word \"Hey Jarvis\" requires foreground mic and is battery-intensive — tap is the reliable default. Enable continuous listening in Settings if your device supports it.", color = Color.White.copy(0.25f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Service toggle
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Background Service", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("JARVIS runs when app is closed", color = Color.White.copy(0.5f), fontSize = 11.sp)
                }
                Switch(checked = true, onCheckedChange = { on ->
                    if (on) JarvisForegroundService.start(ctx) else JarvisForegroundService.stop(ctx)
                })
            }
        }

        Spacer(Modifier.height(12.dp))

        // Recent activity
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Activity", color = Color.White.copy(0.7f), fontSize = 11.sp, letterSpacing = 2.sp)
            TextButton(onClick = onOpenHistory) { Text("View all", color = Color(0xFF00E5FF), fontSize = 11.sp) }
        }
        if (historyPreview.isEmpty()) {
            Text("No calls handled yet.", color = Color.White.copy(0.3f), fontSize = 12.sp, modifier = Modifier.padding(vertical=8.dp))
        } else {
            historyPreview.forEach { e ->
                val fmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(e.timestampMillis))
                Row(Modifier.fillMaxWidth().padding(vertical=4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(e.callerName ?: e.callerNumber, color = Color.White, fontSize = 13.sp)
                        Text("${e.disposition} • $fmt" + if(e.isSimulated) " • SIMULATED" else "", color = Color.White.copy(0.45f), fontSize = 11.sp)
                    }
                    Icon(when(e.disposition){ CallDisposition.ALLOWED->Icons.Default.Call; else->Icons.Default.CallEnd }, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenRules, modifier = Modifier.weight(1f)) { Text("Call Rules") }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) { Text("History") }
        }
    }
}
