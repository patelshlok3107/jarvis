package com.shlok.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.storage.EncryptedStore
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(prefs: JarvisPreferences) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("Shlok") }
    var jarvisName by remember { mutableStateOf("JARVIS") }
    var ttsSpeed by remember { mutableStateOf(1f) }
    var ttsPitch by remember { mutableStateOf(1f) }
    var ttsLang by remember { mutableStateOf("en") }
    var aiProvider by remember { mutableStateOf("template") }
    var busyTpl by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val d = prefs.dataFlow.first()
        userName = d[PrefKeys.USER_NAME] ?: "Shlok"
        jarvisName = d[PrefKeys.JARVIS_NAME] ?: "JARVIS"
        ttsSpeed = d[PrefKeys.TTS_SPEED] ?: 1f
        ttsPitch = d[PrefKeys.TTS_PITCH] ?: 1f
        ttsLang = d[PrefKeys.TTS_LANG] ?: "en"
        aiProvider = d[PrefKeys.AI_PROVIDER] ?: "template"
        recording = d[PrefKeys.RECORDING_ENABLED] ?: false
        busyTpl = d[PrefKeys.TPL_BUSY] ?: ""
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("SETTINGS", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        Section("General") {
            OutlinedTextField(value = userName, onValueChange = { userName=it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = jarvisName, onValueChange = { jarvisName=it }, label = { Text("Assistant name") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { prefs.setString(PrefKeys.USER_NAME, userName); prefs.setString(PrefKeys.JARVIS_NAME, jarvisName) } }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }

        Section("Call Assistant — Responses") {
            OutlinedTextField(value = busyTpl, onValueChange = { busyTpl=it }, label = { Text("Busy response") }, placeholder = { Text("Hello. I am JARVIS…") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = { scope.launch { prefs.setString(PrefKeys.TPL_BUSY, busyTpl) } }, modifier = Modifier.fillMaxWidth()) { Text("Save Busy Response") }
            Text("Also configurable: Meeting / Sleeping / Driving / DND — tap each template in full build.", color = Color.White.copy(0.4f), fontSize = 11.sp)
        }

        Section("Voice") {
            Text("Language", color = Color.White.copy(0.7f), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English", "hi" to "Hindi", "gu" to "Gujarati").forEach { (code,label) ->
                    FilterChip(selected = ttsLang==code, onClick = {
                        ttsLang=code; TtsManager.setLanguage(code); scope.launch { prefs.setString(PrefKeys.TTS_LANG, code) }
                    }, label = { Text(label, fontSize = 11.sp) })
                }
            }
            Text("Speed ${String.format("%.1f", ttsSpeed)}", color = Color.White.copy(0.7f), fontSize = 12.sp, modifier = Modifier.padding(top=8.dp))
            Slider(value = ttsSpeed, onValueChange = { ttsSpeed=it; TtsManager.setSpeedPitch(ttsSpeed, ttsPitch) }, valueRange = 0.5f..2f, onValueChangeFinished = { scope.launch { prefs.setFloat(PrefKeys.TTS_SPEED, ttsSpeed) } })
            Text("Pitch ${String.format("%.1f", ttsPitch)}", color = Color.White.copy(0.7f), fontSize = 12.sp)
            Slider(value = ttsPitch, onValueChange = { ttsPitch=it; TtsManager.setSpeedPitch(ttsSpeed, ttsPitch) }, valueRange = 0.5f..2f, onValueChangeFinished = { scope.launch { prefs.setFloat(PrefKeys.TTS_PITCH, ttsPitch) } })
            Button(onClick = { scope.launch { TtsManager.speak("Hello Shlok. I am $jarvisName. This is my voice.") } }, modifier = Modifier.fillMaxWidth()) { Text("Preview Voice") }
        }

        Section("AI Provider") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("template" to "Template", "cloud" to "Cloud LLM").forEach { (k,l) ->
                    FilterChip(selected = aiProvider==k, onClick = { aiProvider=k; scope.launch { prefs.setString(PrefKeys.AI_PROVIDER, k) } }, label = { Text(l) })
                }
            }
            Text("Cloud provider requires endpoint + API key stored in EncryptedSharedPreferences (never hardcoded). Template works fully offline.", color = Color.White.copy(0.4f), fontSize = 11.sp)
        }

        Section("Privacy") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable call recording", color = Color.White, fontSize = 13.sp)
                Switch(checked = recording, onCheckedChange = { recording=it; scope.launch { prefs.setBool(PrefKeys.RECORDING_ENABLED, it) } })
            }
            Text("Recording is OFF by default and requires user consent + local legal compliance. Transcripts are encrypted locally. Never uploaded without explicit permission.", color = Color.White.copy(0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { EncryptedStore(ctx).clear() }, modifier = Modifier.fillMaxWidth()) { Text("Clear Encrypted Transcripts") }
        }

        Section("Background") {
            Text("If JARVIS stops after a while: disable battery optimization, lock JARVIS in Recents, and allow Autostart (Xiaomi/Oppo). Use the Onboarding screen's battery button.", color = Color.White.copy(0.5f), fontSize = 11.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Color.White.copy(0.6f), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top=16.dp, bottom=8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
