package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var missing by remember { mutableStateOf(PermissionManager.missingPermissions(ctx)) }
    var isScreening by remember { mutableStateOf(PermissionManager.isCallScreeningGranted(ctx)) }
    var isDialer by remember { mutableStateOf(PermissionManager.isDefaultDialer(ctx)) }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("WELCOME TO JARVIS", color = Color(0xFF00E5FF), letterSpacing = 4.sp, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("To act as your mobile assistant,\nJARVIS needs certain permissions.", color = Color.White.copy(0.7f), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        PermissionRow("Phone / Call management", missing.none { it.contains("PHONE") && it.contains("READ") }, "Required to detect incoming calls.")
        PermissionRow("Microphone", missing.none { it.contains("RECORD_AUDIO") }, "For voice commands.")
        PermissionRow("Notifications", missing.none { it.contains("POST_NOTIFICATIONS") }, "For background presence (OS requirement).")
        PermissionRow("Contacts — optional", missing.none { it.contains("READ_CONTACTS") }, "To allow Mom / Rahul to always ring.")
        PermissionRow("Call Screening Role", isScreening, "Lets JARVIS silence/screen calls without being default dialer.")
        Card(Modifier.fillMaxWidth().padding(vertical=8.dp), colors = CardDefaults.cardColors(containerColor = if(isDialer) Color(0xFF0A2A1A) else Color(0xFF2A1A0A))) {
            Column(Modifier.padding(12.dp)) {
                Text(if(isDialer) "✓ Default Dialer — REAL call answering enabled" else "Default Dialer — not set", color = Color.White, fontSize = 13.sp)
                Text(
                    if(isDialer) "JARVIS can answer and play AI voice into the call (where carrier/OS permits)."
                    else "Without this, Android blocks answering + injecting audio into cellular calls. JARVIS will honestly show [SIMULATED] and use notification-based handling. Tap Fix to set JARVIS as default dialer.",
                    color = Color.White.copy(0.6f), fontSize = 11.sp
                )
                if (!isDialer) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) }) { Text("Fix — Open Default Dialer Settings") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
            Column(Modifier.padding(12.dp)) {
                Text("REAL vs SIMULATED", color = Color(0xFFFFC107), fontSize = 12.sp)
                Text(
                    "SIMULATED = JARVIS silences the call, logs it, notifies you, and would-have-said the response aloud (but did NOT inject audio into the phone line — Android forbids this without Default Dialer role). REAL = actually answered via ConnectionService (requires RoleManager.ROLE_DIALER + user consent + carrier support). History marks each call so you always know.",
                    color = Color.White.copy(0.6f), fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (missing.isNotEmpty()) {
            Button(onClick = {
                // Request via Activity launcher — here show manual instruction
                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${ctx.packageName}")
                })
            }, modifier = Modifier.fillMaxWidth()) { Text("Open App Permissions") }
            Text("After granting, return and tap Refresh.", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(top=6.dp))
            OutlinedButton(onClick = {
                missing = PermissionManager.missingPermissions(ctx)
                isScreening = PermissionManager.isCallScreeningGranted(ctx)
                isDialer = PermissionManager.isDefaultDialer(ctx)
            }, modifier = Modifier.fillMaxWidth()) { Text("Refresh status") }
        } else {
            Button(onClick = {
                JarvisForegroundService.start(ctx)
                onDone()
            }, modifier = Modifier.fillMaxWidth()) { Text("Continue — Enable JARVIS") }
        }

        Spacer(Modifier.height(8.dp))
        if (!isScreening) {
            OutlinedButton(onClick = {
                PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) }
                    ?: ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) { Text("Request Call Screening Role") }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Disable Battery Optimization (recommended)")
        }
        Text("Some OEMs (Xiaomi, Samsung, OnePlus) kill background services aggressively — disabling optimization + locking JARVIS in recents helps.", color = Color.White.copy(0.35f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, desc: String) {
    Card(Modifier.fillMaxWidth().padding(vertical=4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218))) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if(granted) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if(granted) Color(0xFF00E676) else Color(0xFFFFC107))
            Column {
                Text(title + if(granted) " ✓" else " — needed", color = Color.White, fontSize = 13.sp)
                Text(desc, color = Color.White.copy(0.5f), fontSize = 11.sp)
            }
        }
    }
}
