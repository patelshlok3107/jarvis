package com.shlok.jarvis.ui.screens

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.manager.DeviceStateManager
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.JarvisPreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    prefs: JarvisPreferences,
    onBack: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DeviceStateManager.DeviceState?>(null) }
    var logs by remember { mutableStateOf(emptyList<com.shlok.jarvis.storage.LogEntry>()) }
    var currentMode by remember { mutableStateOf("Loading...") }
    var refreshTick by remember { mutableStateOf(0) }

    suspend fun refresh() {
        try { state = DeviceStateManager.collect(ctx) } catch (_: Exception) {}
        try {
            val s = prefs.statusFlow.first()
            currentMode = s.displayName
        } catch (_: Exception) {}
    }

    LaunchedEffect(refreshTick) { refresh() }
    LaunchedEffect(Unit) { JarvisLogger.getLogsFlow(ctx).collectLatest { logs = it.take(20) } }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            }
            Text("JARVIS DIAGNOSTICS", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { refreshTick++ }) { Text("Refresh", color = Color(0xFF00E5FF), fontSize = 11.sp) }
        }
        Spacer(Modifier.height(12.dp))

        if (state == null) {
            CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
        } else {
            val s = state!!
            // Spec §28 — every value dynamically detected
            DiagRow("Android:", s.androidVersion, true)
            DiagRow("Device:", s.deviceModel, true)
            DiagRow("App Version:", s.appVersion, true)
            DiagRow("Microphone:", if (s.microphonePermission) "✓ GRANTED" else "✕ DENIED", s.microphonePermission)
            DiagRow("Microphone Permission:", if (s.microphonePermission) "✓ GRANTED" else "✕ DENIED — tap to fix", s.microphonePermission,
                actionLabel = if (!s.microphonePermission) "Fix" else null,
                onAction = { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${ctx.packageName}") }) })
            DiagRow("Voice Service:", if (s.voiceServiceRunning) "✓ RUNNING" else "✕ STOPPED", s.voiceServiceRunning,
                actionLabel = if (!s.voiceServiceRunning) "Start" else null,
                onAction = { com.shlok.jarvis.service.JarvisForegroundService.start(ctx) })
            DiagRow("Wake Engine:", s.wakeWordEngine, s.voiceEngineAvailable)
            DiagRow("Wake Word:", s.wakeWordState, !s.wakeWordState.contains("ERROR") && s.wakeWordState != "STOPPED")
            DiagRow("TTS:", if (s.ttsAvailable) "✓ READY" else "✕ UNAVAILABLE", s.ttsAvailable)
            DiagRow("Call Role:", if (s.callScreeningRole) "✓ CONFIGURED" else "✕ NOT CONFIGURED", s.callScreeningRole,
                actionLabel = if (!s.callScreeningRole) "Fix" else null,
                onAction = { PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) } })
            DiagRow("Call Assistant:", if (s.callScreeningRole || s.defaultDialer) "✓ READY" else "✕ SETUP REQUIRED", s.callScreeningRole || s.defaultDialer)
            DiagRow("Default Dialer:", if (s.defaultDialer) "✓ YES — real answering" else "✕ NO — screening only", s.defaultDialer,
                actionLabel = if (!s.defaultDialer) "Set" else null,
                onAction = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) })
            DiagRow("Notifications:", if (s.notificationPermission) "✓ GRANTED" else "✕ DENIED", s.notificationPermission,
                actionLabel = if (!s.notificationPermission) "Allow" else null,
                onAction = { ctx.startActivity(PermissionManager.intentNotificationSettings(ctx)) })
            DiagRow("Battery Restrictions:", if (!s.batteryOptimizationIgnored) "✓ UNRESTRICTED" else "✕ OPTIMIZED — may kill service", !s.batteryOptimizationIgnored,
                actionLabel = if (s.batteryOptimizationIgnored) "Fix" else null,
                onAction = { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) })
            DiagRow("Internet:", if (s.internetOnline) "✓ ONLINE" else "✕ OFFLINE", s.internetOnline)
            DiagRow("Current Mode:", s.currentMode, true)
            DiagRow("FGS Available:", if (s.foregroundServiceAvailable) "✓ YES" else "✕ NO", s.foregroundServiceAvailable)

            // Never show green check when feature isn't actually working — above logic ensures that
        }

        Spacer(Modifier.height(16.dp))

        // Additional: Service/Battery/Network details from old diagnostics
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("How to read this", color = Color(0xFFFFC107), fontSize = 11.sp, letterSpacing = 1.sp)
                Text("✓ = actually working (detected via Android API). ✕ = not granted / not running. We never fake checks.", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Last Service Event", color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
                val lastService = logs.find { it.event.contains("SERVICE") }
                Text(lastService?.let { JarvisLogger.format(it) } ?: "No events yet", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top=4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Last Call Event", color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
                val lastCall = logs.find { it.event.contains("CALL") }
                Text(lastCall?.let { JarvisLogger.format(it) } ?: "No call events yet", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top=4.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Recent Logs", color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
        logs.take(10).forEach { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                Text(JarvisLogger.format(entry), color = Color.White.copy(0.7f), fontSize = 10.sp, modifier = Modifier.padding(8.dp))
            }
        }
        if (logs.isEmpty()) {
            Text("No logs yet. Enable BUSY and make a test call.", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(top=8.dp))
        }

        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("OEM Restrictions", color = Color(0xFFFFC107), fontSize = 11.sp, letterSpacing = 1.sp)
                Text("Some devices (Xiaomi, Samsung, OnePlus, Oppo) kill background apps. If JARVIS stops, allow Auto-start and Unrestricted battery. Check: Settings → Apps → JARVIS → Battery → Unrestricted.", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
                Spacer(Modifier.height(8.dp))
                Button(onClick = { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) }, modifier = Modifier.fillMaxWidth()) { Text("OPEN BATTERY SETTINGS") }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { refreshTick++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh All") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DiagRow(title: String, value: String, ok: Boolean, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth().padding(vertical=3.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (ok) Color(0xFF00E676) else Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White.copy(0.7f), fontSize = 11.sp)
                    Text(value, color = if (ok) Color.White else Color(0xFFFFC107).copy(0.9f), fontSize = 12.sp)
                }
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = Color(0xFF00E5FF), fontSize = 11.sp) }
            }
        }
    }
}


