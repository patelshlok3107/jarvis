package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.storage.HistoryRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(emptyList<com.shlok.jarvis.data.CallHistoryEntry>()) }
    LaunchedEffect(Unit) {
        HistoryRepository.historyFlow(ctx).collect { list = it }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(16.dp)) {
        Text("JARVIS ACTIVITY", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("${list.size} entries • tap Clear to delete (encrypted locally)", color = Color.White.copy(0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            Text("No activity yet.", color = Color.White.copy(0.4f), modifier = Modifier.padding(top=24.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(list, key = { it.id }) { e ->
                    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(e.timestampMillis))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218))) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(e.callerName ?: e.callerNumber, color = Color.White, fontSize = 14.sp)
                                Text(fmt, color = Color.White.copy(0.45f), fontSize = 11.sp)
                            }
                            Text("${e.disposition} • ${e.status.displayName}" + if(e.isSimulated) " • SIMULATED" else " • REAL", color = Color(0xFF00E5FF).copy(alpha=0.8f), fontSize = 11.sp)
                            e.jarvisResponse?.let { Text(it, color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(top=4.dp)) }
                            e.transcript?.let { Text("Transcript: $it", color = Color.White.copy(0.5f), fontSize = 11.sp) }
                            if (e.isSimulated) Text("ℹ Simulated — grant Default Dialer for real answering (see Onboarding).", color = Color(0xFFFFC107).copy(alpha=0.8f), fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val scope = rememberCoroutineScope()
            OutlinedButton(onClick = { scope.launch { HistoryRepository.clear(ctx) } }, modifier = Modifier.fillMaxWidth()) { Text("Clear History") }
        }
    }
}
