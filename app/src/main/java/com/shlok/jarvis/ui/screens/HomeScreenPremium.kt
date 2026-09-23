package com.shlok.jarvis.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.ui.components.JarvisCore
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    var historyPreview by remember { mutableStateOf(emptyList<com.shlok.jarvis.data.CallHistoryEntry>()) }
    var micGranted by remember { mutableStateOf(false) }
    var screeningGranted by remember { mutableStateOf(false) }
    var batteryIgnored by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            prefs.statusFlow.collectLatest { s ->
                status = s
                smartMode = SmartMode.fromStatus(s)
            }
        } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        try { com.shlok.jarvis.storage.HistoryRepository.historyFlow(ctx).collectLatest { historyPreview = it.take(3) } } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) {
        // Refresh real states
        micGranted = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        screeningGranted = PermissionManager.isCallScreeningGranted(ctx)
        batteryIgnored = try {
            val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(ctx.packageName)
        } catch (_: Exception) { true }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // Top
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("JARVIS", color = Color.White.copy(0.5f), fontSize = 11.sp, letterSpacing = 3.sp)
            TextButton(onClick = onOpenSettings) { Text("Settings", color = Color.White.copy(0.6f), fontSize = 11.sp) }
        }

        Spacer(Modifier.height(8.dp))

        // Core animation — isolated: if it fails, show fallback text
        JarvisCore(status, false, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(12.dp))
        Text(status.displayName, color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(status.subtitle, color = Color.White.copy(0.5f), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
        Text("\"Hey JARVIS\"", color = Color.White.copy(0.35f), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(16.dp))

        // Status cards: Voice Assistant, Call Assistant
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (micGranted) Color(0xFF0A2A1A) else Color(0xFF2A1A0A)),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Voice Assistant", color = Color.White.copy(0.7f), fontSize = 10.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(if (micGranted) "ACTIVE" else "OFFLINE", color = if (micGranted) Color(0xFF00E676) else Color(0xFFFFC107), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (micGranted) "Listening for \"Hey JARVIS\"" else "Tap Assistant to enable", color = Color.White.copy(0.5f), fontSize = 9.sp)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = if (screeningGranted) Color(0xFF0A2A1A) else Color(0xFF2A1A0A)),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Call Assistant", color = Color.White.copy(0.7f), fontSize = 10.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(if (screeningGranted) "ACTIVE" else "SETUP REQUIRED", color = if (screeningGranted) Color(0xFF00E676) else Color(0xFFFFC107), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (screeningGranted) "Ready" else "Tap to fix", color = Color.White.copy(0.5f), fontSize = 9.sp)
                }
            }
        }

        if (!batteryIgnored) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Battery optimization may stop JARVIS", color = Color(0xFFFFC107), fontSize = 11.sp)
                        Text("Tap to allow background activity", color = Color.White.copy(0.6f), fontSize = 10.sp)
                    }
                    TextButton(onClick = { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) }) { Text("Fix", color = Color(0xFF00E5FF), fontSize = 11.sp) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mode chips — primary modes
        Text("Modes", color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        // Row 1: AVAILABLE, BUSY, DND
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SmartMode.AVAILABLE, SmartMode.BUSY, SmartMode.DND).forEach { mode ->
                val isSelected = smartMode == mode || (mode == SmartMode.BUSY && status == JarvisStatus.BUSY) // fallback
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        scope.launch {
                            try {
                                prefs.setStatus(mode.status)
                                TtsManager.speak(mode.status.spokenAck)
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text(mode.displayName.take(6), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF), selectedLabelColor = Color.Black),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Row 2: EXAM, MEETING, DRIVING
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SmartMode.EXAM, SmartMode.MEETING, SmartMode.DRIVING).forEach { mode ->
                val isSelected = prefs.let { smartMode == mode }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        scope.launch {
                            try {
                                prefs.setStatus(mode.status)
                                // For SLEEPING vs EXAM etc need to store mode via SmartModeEngine? Use prefs status + let SmartMode mapping handle?
                                // For exam we set MEETING status but also log
                                com.shlok.jarvis.storage.JarvisLogger.log(ctx, "MODE_SET", mode.name)
                                TtsManager.speak(
                                    when (mode) {
                                        SmartMode.EXAM -> "Understood. I'll activate Exam Mode."
                                        SmartMode.MEETING -> "Meeting mode on."
                                        SmartMode.DRIVING -> "Driving mode on."
                                        else -> mode.status.spokenAck
                                    }
                                )
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text(mode.displayName, fontSize = 9.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Row 3: SLEEPING, STUDYING, GYM
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SmartMode.SLEEPING, SmartMode.STUDYING, SmartMode.GYM).forEach { mode ->
                FilterChip(
                    selected = smartMode == mode,
                    onClick = {
                        scope.launch {
                            try {
                                prefs.setStatus(mode.status)
                                com.shlok.jarvis.storage.JarvisLogger.log(ctx, "MODE_SET", mode.name)
                                TtsManager.speak("${mode.displayName} mode on.")
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text(mode.displayName.take(7), fontSize = 9.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Recent activity
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Activity", color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 2.sp)
            TextButton(onClick = onOpenAssistant) { Text("Assistant", color = Color(0xFF00E5FF), fontSize = 11.sp) }
        }
        Spacer(Modifier.height(6.dp))
        if (historyPreview.isEmpty()) {
            Text("No recent calls. Enable a mode and call this phone to test.", color = Color.White.copy(0.3f), fontSize = 11.sp)
        } else {
            historyPreview.forEach { e ->
                val fmt = try { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(e.timestampMillis)) } catch (_: Exception) { "" }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                    Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(e.callerName ?: e.callerNumber, color = Color.White, fontSize = 13.sp)
                            Text("${e.disposition} • ${e.status.displayName}" + if (e.isSimulated) " • SIMULATED" else " • REAL", color = Color.White.copy(0.5f), fontSize = 10.sp)
                            e.jarvisResponse?.let { Text(it.take(80) + if (it.length > 80) "..." else "", color = Color.White.copy(0.4f), fontSize = 10.sp) }
                        }
                        Text(fmt, color = Color.White.copy(0.4f), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Voice hint
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Try:", color = Color.White.copy(0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
                Text("\"Hey JARVIS, I'm busy.\"", color = Color(0xFF00E5FF), fontSize = 12.sp)
                Text("\"Hey JARVIS, I have an exam in 10 minutes.\"", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(top=4.dp))
                Text("Tap Assistant tab to enable wake word", color = Color.White.copy(0.3f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}
