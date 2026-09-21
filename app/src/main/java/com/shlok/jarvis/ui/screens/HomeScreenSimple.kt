package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.ui.components.JarvisCore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreenSimple(
    prefs: JarvisPreferences,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenOnboarding: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(JarvisStatus.AVAILABLE) }
    var historyPreview by remember { mutableStateOf(emptyList<com.shlok.jarvis.data.CallHistoryEntry>()) }

    // Safe collect - never crash, independent from service
    LaunchedEffect(Unit) {
        try {
            prefs.statusFlow.collectLatest { status = it }
        } catch (_: Exception) {
            // If DataStore fails, stay on default AVAILABLE
        }
    }
    LaunchedEffect(Unit) {
        try {
            com.shlok.jarvis.storage.HistoryRepository.historyFlow(ctx).collectLatest { historyPreview = it.take(3) }
        } catch (_: Exception) {
            // If history fails, show empty
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(16.dp)) {
        // Top bar - minimal
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("JARVIS", color = Color.White.copy(0.5f), fontSize = 11.sp, letterSpacing = 3.sp)
            TextButton(onClick = onOpenSettings) { Text("Settings", color = Color.White.copy(0.6f), fontSize = 11.sp) }
        }

        Spacer(Modifier.height(8.dp))
        JarvisCore(status, false, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(12.dp))
        Text(status.displayName, color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(status.subtitle, color = Color.White.copy(0.5f), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Text("Call Assistant: READY", color = Color(0xFF00E676).copy(0.7f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(16.dp))
        // Simple mode buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            listOf(JarvisStatus.AVAILABLE, JarvisStatus.BUSY, JarvisStatus.DND).forEach { s ->
                FilterChip(
                    selected = status == s,
                    onClick = { scope.launch { try { prefs.setStatus(s) } catch (_: Exception) {} } },
                    label = { Text(s.displayName.take(6), fontSize = 10.sp) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        // Recent activity - safe
        Text("Recent Activity", color = Color.White.copy(0.6f), fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        if (historyPreview.isEmpty()) {
            Text("No recent calls", color = Color.White.copy(0.3f), fontSize = 11.sp)
        } else {
            historyPreview.forEach { e ->
                val fmt = try {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(e.timestampMillis))
                } catch (_: Exception) { "" }
                Text("${e.callerName ?: e.callerNumber} • ${e.disposition} • $fmt", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(vertical=2.dp))
            }
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) { Text("History", fontSize = 11.sp) }
            OutlinedButton(onClick = onOpenRules, modifier = Modifier.weight(1f)) { Text("Rules", fontSize = 11.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Voice: Say \"Hey JARVIS, I'm busy\" (tap mic in full app) • Service runs in background", color = Color.White.copy(0.3f), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
