package com.shlok.jarvis.ui.screens

import android.content.Intent
import android.provider.Settings
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
import com.shlok.jarvis.permissions.PermissionManager

@Composable
fun CallsScreen() {
    val ctx = LocalContext.current
    var isScreening by remember { mutableStateOf(PermissionManager.isCallScreeningGranted(ctx)) }
    var isDialer by remember { mutableStateOf(PermissionManager.isDefaultDialer(ctx)) }
    var missing by remember { mutableStateOf(PermissionManager.missingPermissions(ctx)) }

    fun refresh() {
        isScreening = PermissionManager.isCallScreeningGranted(ctx)
        isDialer = PermissionManager.isDefaultDialer(ctx)
        missing = PermissionManager.missingPermissions(ctx)
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("JARVIS CALL ASSISTANT", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        // Main status card — spec §12
        if (!isScreening) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CALL ASSISTANT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Status:", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    Text("⚠ NOT CONFIGURED", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical=4.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("JARVIS needs the appropriate Android call role/permission to manage calls.", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) }
                            ?: ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("SET UP") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { refresh() }) { Text("Refresh", color = Color.White.copy(0.6f)) }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2A1A)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CALL ASSISTANT", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("● ACTIVE", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Incoming calls:", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    Text("READY", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("JARVIS will handle calls based on your current mode (BUSY/EXAM/etc) using Android CallScreeningService.", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Role details
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                StatusRow("Call Screening Role", if (isScreening) "Configured ✓" else "Not configured", isScreening,
                    actionLabel = if (!isScreening) "Fix" else null,
                    onAction = {
                        PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) }
                            ?: ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    })
                StatusRow("Default Dialer Role", if (isDialer) "Active — Real answering enabled" else "Not set", isDialer,
                    actionLabel = if (!isDialer) "Set" else null,
                    onAction = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) })
                StatusRow("Phone Permissions", if (missing.isEmpty()) "Granted" else "Missing: ${missing.joinToString()}", missing.isEmpty(),
                    actionLabel = if (missing.isNotEmpty()) "Grant" else null,
                    onAction = { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${ctx.packageName}") }) })
            }
        }

        Spacer(Modifier.height(16.dp))

        // Real incoming call test — spec §13
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("REAL INCOMING CALL TEST", color = Color(0xFF00E5FF), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("1. Enable BUSY MODE (Home tab)", color = Color.White.copy(0.7f), fontSize = 11.sp)
                Text("2. Lock phone", color = Color.White.copy(0.7f), fontSize = 11.sp)
                Text("3. Enable BUSY MODE if not already", color = Color.White.copy(0.7f), fontSize = 11.sp)
                Text("4. Call the JARVIS phone from another phone", color = Color.White.copy(0.7f), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text("JARVIS must react using the actual Android call APIs available to the application.", color = Color.White.copy(0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("• With Call Screening: call is silenced, logged, notification sent (isSimulated=false for screening)", color = Color.White.copy(0.5f), fontSize = 10.sp)
                Text("• With Default Dialer: call is answered and TTS plays into call (where carrier permits)", color = Color.White.copy(0.5f), fontSize = 10.sp)
                Text("• Do NOT create a simulation — real cellular call only", color = Color(0xFFFFC107).copy(0.8f), fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Automatic answering note — spec §14
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Automatic Call Answering", color = Color(0xFFFFC107), fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                if (isDialer) {
                    Text("● Dialer role active — JARVIS can answer via Telecom ConnectionService and play TTS (where carrier/OS permits).", color = Color(0xFF00E676), fontSize = 11.sp)
                } else {
                    Text("If automatic answering is not permitted without Default Dialer role, you need to set JARVIS as Default Dialer.", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) }, modifier = Modifier.fillMaxWidth()) { Text("Set Default Dialer to Enable Answering") }
                }
                Spacer(Modifier.height(6.dp))
                Text("We do not attempt to bypass the restriction — we guide you through the official Android flow.", color = Color.White.copy(0.4f), fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Call speech examples — spec §15
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Dynamic Call Responses", color = Color(0xFF00E5FF), fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "BUSY" to "Hello, I'm JARVIS, Shlok's AI assistant. He's currently busy and can't take the call. Please leave a message.",
                    "EXAM" to "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in an exam and can't take the call. Please leave a message.",
                    "DRIVING" to "Hello, I'm JARVIS, Shlok's AI assistant. He's currently driving and can't safely answer the call. Please leave a message.",
                    "SLEEPING" to "Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message.",
                    "MEETING" to "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in a meeting and can't take the call. Please leave a message."
                ).forEach { (mode, text) ->
                    Text("$mode:", color = Color.White.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=6.dp))
                    Text(text, color = Color.White.copy(0.5f), fontSize = 10.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("These are dynamically generated via Mode → ResponseGenerator → TTS, not hard-coded separate flows.", color = Color.White.copy(0.35f), fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh Status") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatusRow(title: String, value: String, ok: Boolean, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
