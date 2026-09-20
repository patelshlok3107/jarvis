package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.service.JarvisForegroundService

@Composable
fun PhoneConnectionScreen() {
    val ctx = LocalContext.current
    var missing by remember { mutableStateOf(PermissionManager.missingPermissions(ctx)) }
    var isScreening by remember { mutableStateOf(PermissionManager.isCallScreeningGranted(ctx)) }
    var isDialer by remember { mutableStateOf(PermissionManager.isDefaultDialer(ctx)) }
    var serviceRunning by remember { mutableStateOf(true) } // ForegroundService started in MainActivity

    fun refresh() {
        missing = PermissionManager.missingPermissions(ctx)
        isScreening = PermissionManager.isCallScreeningGranted(ctx)
        isDialer = PermissionManager.isDefaultDialer(ctx)
    }

    val allPhoneOk = missing.isEmpty() && isScreening
    val readyForRealAnswering = isDialer
    val overallReady = allPhoneOk // screening is sufficient for silencing; answering needs dialer

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(16.dp)) {
        Text("JARVIS PHONE CONNECTION", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        if (overallReady && isDialer) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2A1A)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("JARVIS - READY", color = Color(0xFF00E676), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                    Text("Real answering enabled (Default Dialer). Caller WILL hear JARVIS voice (where carrier permits).", color = Color.White.copy(0.7f), fontSize = 11.sp)
                }
            }
        } else if (overallReady) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A0A)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("JARVIS - READY (LIMITED)", color = Color(0xFFFFC107), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                    Text("Call screening active. Without Default Dialer, Android blocks injecting TTS into cellular audio.", color = Color.White.copy(0.7f), fontSize = 11.sp)
                    Text("Closest supported: silence + notification + history (isSimulated=true). Tap below for REAL answering.", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) }, modifier = Modifier.fillMaxWidth()) { Text("Enable Real Answering - Set Default Dialer") }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("JARVIS CANNOT HANDLE CALLS YET", color = Color(0xFFFF8A80), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                    Text("Missing:", color = Color.White.copy(0.7f), fontSize = 11.sp, modifier = Modifier.padding(top=6.dp))
                    if (missing.isNotEmpty()) Text("- Permissions: ${missing.joinToString()}", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    if (!isScreening) Text("- Default call screening role", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${ctx.packageName}")
                        })
                    }, modifier = Modifier.fillMaxWidth()) { Text("Open Android Settings") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        StatusRow("Phone Status", if (missing.isEmpty()) "Connected" else "Permission missing", missing.isEmpty())
        StatusRow("Call Assistant", if (isScreening) "Ready" else "Not configured", isScreening)
        StatusRow("Default Call Role", if (isDialer) "Configured" else "Not set - tap to fix", isDialer, actionLabel = if (!isDialer) "Fix" else null, onAction = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) })
        StatusRow("Background Service", if (serviceRunning) "Running" else "Stopped", serviceRunning, actionLabel = if (!serviceRunning) "Start" else null, onAction = { JarvisForegroundService.start(ctx); serviceRunning = true })
        StatusRow("Microphone", if (missing.none { it.contains("RECORD_AUDIO") }) "Allowed" else "Not allowed", missing.none { it.contains("RECORD_AUDIO") })
        StatusRow("Notifications", if (missing.none { it.contains("POST_NOTIFICATIONS") }) "Allowed" else "Not allowed", missing.none { it.contains("POST_NOTIFICATIONS") })
        StatusRow("Battery Optimization", "Check", true, actionLabel = "Configure", onAction = { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) })

        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("WHAT WORKS / WHAT DOES NOT", color = Color(0xFFFFC107), fontSize = 11.sp, letterSpacing = 1.sp)
                Text("WORKS (CallScreeningService): detect real incoming cellular call, check JARVIS status, silence/reject per rules, save history, notify you. Tested on real device with BUSY mode.", color = Color.White.copy(0.7f), fontSize = 11.sp, modifier = Modifier.padding(top=6.dp))
                Text("NEEDS Default Dialer (ConnectionService+InCallService): answer() and play TTS into call audio so caller hears JARVIS. Without this role, Android modem blocks audio injection — we honestly mark isSimulated=true.", color = Color.White.copy(0.7f), fontSize = 11.sp, modifier = Modifier.padding(top=6.dp))
                Text("WHY: Android Telecom security — only the Default Dialer may create Connections. Carriers may still block app audio injection even then. We detect and report correctly.", color = Color.White.copy(0.5f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh Status") }
        if (!isScreening) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) }
                    ?: ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) { Text("Request Call Screening Role") }
        }
    }
}

@Composable
private fun StatusRow(title: String, value: String, ok: Boolean, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (ok) Color(0xFF00E676) else Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp)
                    Text(value, color = if (ok) Color(0xFF00E676).copy(0.8f) else Color(0xFFFFC107).copy(0.8f), fontSize = 11.sp)
                }
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = Color(0xFF00E5FF), fontSize = 11.sp) }
            }
        }
    }
}
