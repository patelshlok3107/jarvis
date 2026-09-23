package com.shlok.jarvis.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    prefs: JarvisPreferences?,
    onComplete: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) } // 0 welcome, 1 notif, 2 mic, 3 voice, 4 call
    var notifGranted by remember { mutableStateOf(false) }
    var micGranted by remember { mutableStateOf(false) }
    var screeningGranted by remember { mutableStateOf(false) }

    fun refresh() {
        notifGranted = if (Build.VERSION.SDK_INT >= 33) ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
        micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        screeningGranted = PermissionManager.isCallScreeningGranted(ctx)
    }

    LaunchedEffect(Unit) { refresh() }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
        if (granted) step = 2
        refresh()
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (granted) step = 3
        refresh()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh()
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF05070A)).verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("JARVIS", color = Color(0xFF00E5FF), letterSpacing = 8.sp, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
        Text("Your personal Android assistant.", color = Color.White.copy(0.5f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        // Progress
        LinearProgressIndicator(progress = (step + 1) / 5f, modifier = Modifier.fillMaxWidth(), color = Color(0xFF00E5FF), trackColor = Color(0xFF1E2A3A))
        Spacer(Modifier.height(16.dp))

        when (step) {
            0 -> {
                Text("WELCOME TO JARVIS", color = Color(0xFF00E5FF), letterSpacing = 4.sp, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text("Your personal Android assistant.\n\nLet's set up JARVIS in a few quick steps. Each permission is explained and requested via the official Android system flow.", color = Color.White.copy(0.7f), fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Let's set up JARVIS") }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    scope.launch { prefs?.setBool(PrefKeys.ONBOARDING_DONE, true) }
                    onComplete()
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Skip for now", color = Color.White.copy(0.5f)) }
            }
            1 -> {
                Text("Step 1 — Notifications", color = Color(0xFF00E5FF), letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Notifications", color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Required for background status and call alerts. Android requires this for foreground services.", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        if (notifGranted) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E676))
                                Text("Granted ✓", color = Color(0xFF00E676), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                        } else {
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else step = 2
                            }, modifier = Modifier.fillMaxWidth()) { Text("Allow") }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Skip", color = Color.White.copy(0.5f)) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
            }
            2 -> {
                Text("Step 2 — Microphone", color = Color(0xFF00E5FF), letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Microphone", color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Required for voice commands and \"Hey JARVIS\" wake word. Processed locally, not uploaded.", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        if (micGranted) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E676))
                                Text("Granted ✓", color = Color(0xFF00E676), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                        } else {
                            Button(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) { Text("Allow") }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) { Text("Skip — Voice will be offline", color = Color.White.copy(0.5f)) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { step = 1 }) { Text("Back") }
                    TextButton(onClick = { refresh() }) { Text("Refresh") }
                }
            }
            3 -> {
                Text("Step 3 — Voice Assistant", color = Color(0xFF00E5FF), letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Voice Assistant", color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Android allows microphone access only when you enable it and grant permission. JARVIS will start a foreground service with a visible notification when you enable voice.", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("• Tap mic in Assistant tab to speak\n• Enable wake word \"Hey JARVIS\" for hands-free\n• Battery-efficient — continuous listening only if you enable it", color = Color.White.copy(0.5f), fontSize = 11.sp)
                        Spacer(Modifier.height(12.dp))
                        if (!micGranted) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), modifier = Modifier.fillMaxWidth()) {
                                Text("Microphone not granted — voice will show offline. You can enable it later in Settings.", color = Color(0xFFFF8A80), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Button(onClick = { step = 4 }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { step = 2 }) { Text("Back") }
                    TextButton(onClick = { step = 4 }) { Text("Next") }
                }
            }
            4 -> {
                Text("Step 4 — Call Assistant", color = Color(0xFF00E5FF), letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1218)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("CALL ASSISTANT", color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("To manage calls, Android may require a supported system role/permission.", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        if (screeningGranted) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E676))
                                Text("Call screening role granted ✓", color = Color(0xFF00E676), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("JARVIS can screen calls (silence/handling). For real answering with TTS, you'll also need Default Dialer role (optional, in next screen).", color = Color.White.copy(0.5f), fontSize = 11.sp)
                        } else {
                            Text("JARVIS needs Call Screening role to detect and handle incoming calls.", color = Color.White.copy(0.6f), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                val intent = PermissionManager.intentRequestCallScreeningRole(ctx)
                                if (intent != null) roleLauncher.launch(intent)
                                else ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                            }, modifier = Modifier.fillMaxWidth()) { Text("SET UP CALL ASSISTANT") }
                            Spacer(Modifier.height(8.dp))
                            Text("This opens the official Android system role request — JARVIS does not fake permissions.", color = Color.White.copy(0.4f), fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Default Dialer (optional, for real answering)", color = Color.White.copy(0.7f), fontSize = 12.sp)
                        Text("Without Default Dialer, Android blocks answering + injecting TTS audio into the call. JARVIS will honestly show [SIMULATED] and use silence + notification. Tap below to enable real answering if you want it.", color = Color.White.copy(0.5f), fontSize = 11.sp, modifier = Modifier.padding(top=6.dp))
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { ctx.startActivity(PermissionManager.intentDefaultDialer(ctx)) }, modifier = Modifier.fillMaxWidth()) { Text("Set as Default Dialer") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch { prefs?.setBool(PrefKeys.ONBOARDING_DONE, true) }
                    onComplete()
                }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Finish — Open JARVIS") }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { step = 3 }) { Text("Back") }
                    TextButton(onClick = { refresh() }) { Text("Refresh status") }
                }
                if (!screeningGranted) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        scope.launch { prefs?.setBool(PrefKeys.ONBOARDING_DONE, true) }
                        onComplete()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Skip — set up later", color = Color.White.copy(0.5f)) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (step > 0) {
            Text("You can change these anytime in Settings → Setup Wizard", color = Color.White.copy(0.3f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
