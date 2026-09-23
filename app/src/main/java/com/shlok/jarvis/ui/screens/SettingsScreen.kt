package com.shlok.jarvis.ui.screens

import android.content.Intent
import android.provider.Settings
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
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.EncryptedStore
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    prefs: JarvisPreferences,
    onOpenDiagnostics: (() -> Unit)? = null,
    onOpenSetup: (() -> Unit)? = null
) {
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
    var showDiagnostics by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val d = prefs.dataFlow.first()
            userName = d[PrefKeys.USER_NAME] ?: "Shlok"
            jarvisName = d[PrefKeys.JARVIS_NAME] ?: "JARVIS"
            ttsSpeed = d[PrefKeys.TTS_SPEED] ?: 1f
            ttsPitch = d[PrefKeys.TTS_PITCH] ?: 1f
            ttsLang = d[PrefKeys.TTS_LANG] ?: "en"
            aiProvider = d[PrefKeys.AI_PROVIDER] ?: "template"
            recording = d[PrefKeys.RECORDING_ENABLED] ?: false
            busyTpl = d[PrefKeys.TPL_BUSY] ?: ""
        } catch (_: Exception) {}
    }

    if (showDiagnostics) {
        DiagnosticsScreen(prefs, onBack = { showDiagnostics = false })
        return
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("SETTINGS", color = Color(0xFF00E5FF), letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("JARVIS • v2.0.0 (8)", color = Color.White.copy(0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))

        // Setup wizard shortcut
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Setup Wizard", color = Color.White, fontSize = 13.sp)
                Text("Re-run the 4-step setup: Notifications, Microphone, Voice, Calls", color = Color.White.copy(0.5f), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpenSetup?.invoke() }) { Text("Open Setup Wizard") }
                    OutlinedButton(onClick = { showDiagnostics = true }) { Text("Diagnostics") }
                }
            }
        }

        Section("General") {
            OutlinedTextField(value = userName, onValueChange = { userName=it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = jarvisName, onValueChange = { jarvisName=it }, label = { Text("Assistant name") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { try { prefs.setString(PrefKeys.USER_NAME, userName); prefs.setString(PrefKeys.JARVIS_NAME, jarvisName) } catch (_: Exception) {} } }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }

        Section("Call Assistant — Responses") {
            OutlinedTextField(value = busyTpl, onValueChange = { busyTpl=it }, label = { Text("Busy response") }, placeholder = { Text("Hello. I am JARVIS…") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = { scope.launch { try { prefs.setString(PrefKeys.TPL_BUSY, busyTpl) } catch (_: Exception) {} } }, modifier = Modifier.fillMaxWidth()) { Text("Save Busy Response") }
            Text("Also configurable via code: Meeting / Sleeping / Driving / DND use dynamic ResponseGenerator. Custom: \"Hey JARVIS, tell callers I'm studying.\"", color = Color.White.copy(0.4f), fontSize = 11.sp)
        }

        Section("Voice") {
            Text("Language", color = Color.White.copy(0.7f), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English", "hi" to "Hindi", "gu" to "Gujarati").forEach { (code,label) ->
                    FilterChip(selected = ttsLang==code, onClick = {
                        ttsLang=code; TtsManager.setLanguage(code); scope.launch { try { prefs.setString(PrefKeys.TTS_LANG, code) } catch (_: Exception) {} }
                    }, label = { Text(label, fontSize = 11.sp) })
                }
            }
            Text("Speed ${String.format("%.1f", ttsSpeed)}", color = Color.White.copy(0.7f), fontSize = 12.sp, modifier = Modifier.padding(top=8.dp))
            Slider(value = ttsSpeed, onValueChange = { ttsSpeed=it; TtsManager.setSpeedPitch(ttsSpeed, ttsPitch) }, valueRange = 0.5f..2f, onValueChangeFinished = { scope.launch { try { prefs.setFloat(PrefKeys.TTS_SPEED, ttsSpeed) } catch (_: Exception) {} } })
            Text("Pitch ${String.format("%.1f", ttsPitch)}", color = Color.White.copy(0.7f), fontSize = 12.sp)
            Slider(value = ttsPitch, onValueChange = { ttsPitch=it; TtsManager.setSpeedPitch(ttsSpeed, ttsPitch) }, valueRange = 0.5f..2f, onValueChangeFinished = { scope.launch { try { prefs.setFloat(PrefKeys.TTS_PITCH, ttsPitch) } catch (_: Exception) {} } })
            Button(onClick = { scope.launch { try { TtsManager.speak("Hello Shlok. I am $jarvisName. This is my voice.") } catch (_: Exception) {} } }, modifier = Modifier.fillMaxWidth()) { Text("Preview Voice") }
            Spacer(Modifier.height(8.dp))
            Text("TTS is Android-native, configurable, interruptible, low-latency.", color = Color.White.copy(0.35f), fontSize = 10.sp)
        }

        Section("AI Provider") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("template" to "Template", "cloud" to "Cloud LLM").forEach { (k,l) ->
                    FilterChip(selected = aiProvider==k, onClick = { aiProvider=k; scope.launch { try { prefs.setString(PrefKeys.AI_PROVIDER, k) } catch (_: Exception) {} } }, label = { Text(l) })
                }
            }
            Text("Cloud provider requires endpoint + API key in EncryptedSharedPreferences (never hardcoded). Template works fully offline. Without internet, Cloud shows \"Offline\" but app stays functional.", color = Color.White.copy(0.4f), fontSize = 11.sp)
        }

        Section("Privacy") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable call recording", color = Color.White, fontSize = 13.sp)
                Switch(checked = recording, onCheckedChange = { recording=it; scope.launch { try { prefs.setBool(PrefKeys.RECORDING_ENABLED, it) } catch (_: Exception) {} } })
            }
            Text("Recording is OFF by default and requires user consent + local legal compliance. Transcripts are encrypted locally. Never uploaded without explicit permission. If OS blocks call audio, feature shows \"Not supported\".", color = Color.White.copy(0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { EncryptedStore(ctx).clear() }, modifier = Modifier.fillMaxWidth()) { Text("Clear Encrypted Transcripts") }
        }

        Section("Background & Battery") {
            Text("JARVIS uses supported background architecture (FGS + AlarmManager). No infinite loops, no constant restarts, no battery abuse.", color = Color.White.copy(0.5f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { try { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) } catch (_: Exception) {} }, modifier = Modifier.fillMaxWidth()) { Text("Open Battery Settings") }
            Text("If manufacturer kills background apps (Xiaomi, Samsung, OnePlus): allow Auto-start, set Battery to Unrestricted, lock JARVIS in Recents.", color = Color.White.copy(0.4f), fontSize = 10.sp, modifier = Modifier.padding(top=6.dp))
        }

        Section("About JARVIS") {
            Text("Version 2.0.0 (8) — rebuilt from ground up for Android 14+ security compliance.", color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text("Target SDK 34 • Min SDK 26 • Compose • DataStore • WorkManager • Telecom APIs", color = Color.White.copy(0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text("Cloud backend: Vercel + Render (optional, for AI features only). Core functions run natively offline.", color = Color.White.copy(0.35f), fontSize = 10.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { try { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${ctx.packageName}") }) } catch (_: Exception) {} }, modifier = Modifier.fillMaxWidth()) { Text("Open App Info") }
        }

        Section("Diagnostics") {
            Text("Real device state, not faked — taps open detailed diagnostics.", color = Color.White.copy(0.5f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showDiagnostics = true }, modifier = Modifier.fillMaxWidth()) { Text("Open Diagnostics") }
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
