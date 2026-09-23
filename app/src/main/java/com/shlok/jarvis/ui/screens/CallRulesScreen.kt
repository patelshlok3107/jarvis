package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.data.ContactRuleAction
import com.shlok.jarvis.data.UnknownCallerAction
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun CallRulesScreen(prefs: JarvisPreferences, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var unknown by remember { mutableStateOf(UnknownCallerAction.JARVIS_HANDLES) }

    LaunchedEffect(Unit) {
        val v = prefs.dataFlow.first()[PrefKeys.UNKNOWN_ACTION] ?: "JARVIS_HANDLES"
        unknown = try { UnknownCallerAction.valueOf(v) } catch(_:Exception){ UnknownCallerAction.JARVIS_HANDLES }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(16.dp)) {
        if (onBack != null) {
            Row {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Text("CALL RULES", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp, modifier = Modifier.padding(top=12.dp))
            }
        } else {
            Text("CALL RULES", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        RuleCard("Mom", "Always Allow — demo", ContactRuleAction.ALWAYS_ALLOW, {})
        RuleCard("Rahul", "Always Allow — demo", ContactRuleAction.ALWAYS_ALLOW, {})
        Card(Modifier.fillMaxWidth().padding(vertical=6.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218))) {
            Column(Modifier.padding(12.dp)) {
                Text("Unknown Numbers", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnknownCallerAction.entries.forEach { a ->
                        FilterChip(selected = unknown==a, onClick = {
                            unknown=a; scope.launch { prefs.setString(PrefKeys.UNKNOWN_ACTION, a.name) }
                        }, label = { Text(a.name, fontSize = 10.sp) })
                    }
                }
                Text("JARVIS Handles = screen when BUSY/DND/etc. Allow = ring normally.", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(top=6.dp))
            }
        }
        Card(Modifier.fillMaxWidth().padding(vertical=6.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218))) {
            Column(Modifier.padding(12.dp)) {
                Text("Spam / Suspicious", color = Color.White, fontSize = 14.sp)
                Text("Uses Android's spam screening where available. Block/Screen.", color = Color.White.copy(0.5f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Contact-specific rules: grant Contacts permission, then use the system contact picker (coming next — current build uses the two demo rules + Unknown handling). Full CRUD with EncryptedSharedPreferences ships in v1.1.", color = Color.White.copy(0.35f), fontSize = 11.sp)
    }
}

@Composable
private fun RuleCard(name: String, subtitle: String, action: ContactRuleAction, onChange: (ContactRuleAction)->Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical=6.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218))) {
        Column(Modifier.padding(12.dp)) {
            Text(name, color = Color.White, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(0.5f), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text("[ ${action.name.replace('_',' ')} ]", color = Color(0xFF00E5FF), fontSize = 11.sp)
        }
    }
}
