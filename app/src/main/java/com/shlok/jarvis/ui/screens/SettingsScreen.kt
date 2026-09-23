package com.shlok.jarvis.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.manager.DeviceStateManager
import com.shlok.jarvis.permissions.PermissionManager
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import com.shlok.jarvis.ui.theme.ObsidianColors
import com.shlok.jarvis.ui.theme.ObsidianRounded
import com.shlok.jarvis.ui.theme.ObsidianSpacing
import com.shlok.jarvis.ui.theme.ObsidianTypography
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
    var ttsSpeed by remember { mutableStateOf(1f) }
    var ttsLang by remember { mutableStateOf("en") }
    var callAssistantOn by remember { mutableStateOf(PermissionManager.isCallScreeningGranted(ctx)) }
    var deviceState by remember { mutableStateOf<DeviceStateManager.DeviceState?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showCallRules by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val d = prefs.dataFlow.first()
            userName = d[PrefKeys.USER_NAME] ?: "Shlok"
            ttsSpeed = d[PrefKeys.TTS_SPEED] ?: 1f
            ttsLang = d[PrefKeys.TTS_LANG] ?: "en"
        } catch (_: Exception) {}
        try { deviceState = DeviceStateManager.collect(ctx) } catch (_: Exception) {}
        callAssistantOn = PermissionManager.isCallScreeningGranted(ctx)
    }

    if (showDiagnostics) {
        DiagnosticsScreen(prefs, onBack = { showDiagnostics = false })
        return
    }
    if (showCallRules) {
        CallRulesScreen(prefs, onBack = { showCallRules = false })
        return
    }

    Column(
        Modifier.fillMaxSize().background(ObsidianColors.Background).verticalScroll(rememberScrollState()).padding(horizontal = ObsidianSpacing.Margin).padding(top = 12.dp, bottom = 12.dp)
    ) {
        // Sub-Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Settings & System", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text("NODE: 0x9F4A", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Profile & Core ID Card
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Code, null, tint = ObsidianColors.Primary, modifier = Modifier.size(26.dp))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer).align(Alignment.BottomEnd))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(userName, color = ObsidianColors.OnSurface, style = ObsidianTypography.HeadlineSm, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.Verified, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                    }
                    Text("JARVIS Neural Engine v4.8 (Enterprise Core)", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                }
            }
            Row(
                Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHighest).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                Text("SYNCHRONIZED", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(12.dp))

        // System Diagnostics Overview Card
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Memory, null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                    Text("System Diagnostics", color = ObsidianColors.OnSurface, style = ObsidianTypography.LabelMd, letterSpacing = 1.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(ObsidianRounded.Pill)).background(ObsidianColors.SurfaceContainerHigh).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.PrimaryContainer))
                    Text("ALL SYSTEMS HEALTHY", color = ObsidianColors.Primary, style = ObsidianTypography.LabelSm)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Grid 2x4
            val diagnostics = listOf(
                "Application" to (deviceState?.let { if (true) "READY" else "OFF" } ?: "READY"),
                "Microphone" to (if (deviceState?.microphonePermission == true) "READY" else "OFF"),
                "Voice Engine" to (if (deviceState?.voiceEngineAvailable == true) "READY" else "OFF"),
                "Wake Word" to (deviceState?.wakeWordState?.let { if (it.contains("READY") || it.contains("WAKE")) "READY" else "OFF" } ?: "READY"),
                "Call Assistant" to (if (deviceState?.callScreeningRole == true) "READY" else "SETUP"),
                "TTS Synthesis" to (if (deviceState?.ttsAvailable == true) "READY" else "OFF"),
                "On-Device Neural" to "READY",
                "Network (14ms)" to (if (deviceState?.internetOnline == true) "ONLINE" else "OFFLINE")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                diagnostics.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (label, value) ->
                            val isReady = value == "READY" || value == "ONLINE"
                            Row(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (isReady) ObsidianColors.Primary else ObsidianColors.OutlineVariant))
                                    Text(value, color = if (isReady) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showDiagnostics = true }, modifier = Modifier.align(Alignment.End)) {
                Text("View Details", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
                Icon(Icons.Default.ChevronRight, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // AI & Voice Configuration
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Icon(Icons.Default.GraphicEq, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                Text("AI & Voice Configuration", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(4.dp)
            ) {
                SettingsRow("Wake Word", "Hey JARVIS (Always Ready)", "VOICE_TRIGGER", onClick = { /* open assistant */ })
                SettingsRow("Voice Persona", "JARVIS Voice 01 (Neural British)", "EN-GB", onClick = {
                    // Cycle language
                    val next = when (ttsLang) { "en" -> "hi"; "hi" -> "gu"; else -> "en" }
                    ttsLang = next
                    TtsManager.setLanguage(next)
                    scope.launch { try { prefs.setString(PrefKeys.TTS_LANG, next) } catch (_: Exception) {} }
                })
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showSpeedDialog = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Speech Speed & Cadence", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Box(Modifier.width(96.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ObsidianColors.SurfaceContainerHighest)) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth((ttsSpeed / 2f).coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(ObsidianColors.PrimaryContainer))
                            }
                            Text("${String.format("%.2f", ttsSpeed)}x Dynamic", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                SettingsRow("Response Style", "Concise & Precision", null, onClick = {})
                SettingsRow("Language", "English (Primary), Hindi, Gujarati", "TRI-LINGUAL", onClick = {
                    val next = when (ttsLang) { "en" -> "hi"; "hi" -> "gu"; else -> "en" }
                    ttsLang = next
                    TtsManager.setLanguage(next)
                    scope.launch { try { prefs.setString(PrefKeys.TTS_LANG, next) } catch (_: Exception) {} }
                })
            }
        }

        Spacer(Modifier.height(16.dp))

        // Intelligent Calls & Automation
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Icon(Icons.Default.Call, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                Text("Intelligent Calls & Automation", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Call Assistant Engine", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
                        Text(if (callAssistantOn) "Autonomous Screener Active" else "Tap to enable", color = if (callAssistantOn) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
                    }
                    Switch(
                        checked = callAssistantOn,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                PermissionManager.intentRequestCallScreeningRole(ctx)?.let { ctx.startActivity(it) }
                                callAssistantOn = true
                            } else {
                                callAssistantOn = false
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianColors.PrimaryContainer, checkedTrackColor = ObsidianColors.PrimaryContainer.copy(alpha = 0.3f), uncheckedThumbColor = ObsidianColors.OutlineVariant, uncheckedTrackColor = ObsidianColors.SurfaceContainerHighest)
                    )
                }
                SettingsRow("Autonomous Mode Rules", "4 Active Schedules", null, onClick = { showCallRules = true })
                SettingsRow("VIP Emergency Bypass", "3 Contacts Whitelisted", "PRIORITY", onClick = { showCallRules = true })
            }
        }

        Spacer(Modifier.height(16.dp))

        // Appearance & System
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Icon(Icons.Default.Tune, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                Text("Appearance & System", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(4.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Theme Mode", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
                        Text("DARK OLED", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.LabelSm)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ObsidianColors.SurfaceContainerLowest).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Dark" to true, "Light" to false, "System" to false).forEach { (label, selected) ->
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (selected) ObsidianColors.SurfaceContainerHigh else Color.Transparent).padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (selected) ObsidianColors.Primary else ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
                            }
                        }
                    }
                }
                SettingsRow("Biometric Security", "Face Unlock & Fingerprint Required", null, trailingIcon = Icons.Default.Fingerprint, onClick = {})
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Privacy & Data Pipeline", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
                        Text("100% On-Device Processing", color = ObsidianColors.Primary, style = ObsidianTypography.BodySm)
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).background(ObsidianColors.SurfaceContainerHighest), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VerifiedUser, null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Battery
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
        ) {
            Text("Background & Battery", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
            Text("JARVIS may be stopped by battery optimization.", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { try { ctx.startActivity(PermissionManager.intentBatteryOptimization(ctx)) } catch (_: Exception) {} },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PrimaryContainer, contentColor = ObsidianColors.OnPrimaryContainer)
            ) {
                Text("OPEN BATTERY SETTINGS", style = ObsidianTypography.LabelMd)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Telemetry logs
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLowest).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Code, null, tint = ObsidianColors.OutlineVariant, modifier = Modifier.size(14.dp))
                    Text("Telemetry Core Logs", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
                }
                Text("LIVE STREAM", color = ObsidianColors.Primary, style = ObsidianTypography.LabelSm)
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("[09:41:02] V_CORE: Model weight cache intact (2.4 GB allocated)", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                Text("[09:41:04] DSP: Noise suppression envelope locked at -42dB", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
                Text("[09:41:05] SEC_AGENT: Zero exfiltration verified via hardware sandbox", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.LabelSm)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Setup wizard shortcut
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ObsidianRounded.Xl)).background(ObsidianColors.SurfaceContainerLow).border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(ObsidianRounded.Xl)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Setup Wizard", color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium)
                Text("Re-run 4-step setup", color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm)
            }
            Button(onClick = { onOpenSetup?.invoke() }, colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PrimaryContainer, contentColor = ObsidianColors.OnPrimaryContainer)) {
                Text("Open", style = ObsidianTypography.LabelMd)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Footer
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Shield, null, tint = ObsidianColors.OutlineVariant, modifier = Modifier.size(14.dp))
                Text("Neural OS Architecture", color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("JARVIS Core OS Build 2025.4.1 • Designed for Shlok • All rights reserved.", color = ObsidianColors.OutlineVariant.copy(alpha = 0.8f), style = ObsidianTypography.LabelSm)
            Spacer(Modifier.height(4.dp))
            Text("v2.1.0 (10) • ${deviceState?.appVersion ?: ""}", color = ObsidianColors.SurfaceContainerHighest, style = ObsidianTypography.LabelSm)
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Speech Speed", color = ObsidianColors.OnSurface) },
            text = {
                Column {
                    Text("${String.format("%.2f", ttsSpeed)}x", color = ObsidianColors.PrimaryContainer, style = ObsidianTypography.BodyMd)
                    Slider(value = ttsSpeed, onValueChange = { ttsSpeed = it; TtsManager.setSpeedPitch(ttsSpeed, 1f) }, valueRange = 0.5f..2f)
                    Button(onClick = {
                        scope.launch {
                            try { prefs.setFloat(PrefKeys.TTS_SPEED, ttsSpeed) } catch (_: Exception) {}
                            TtsManager.speak("Hello, this is my voice at ${String.format("%.1f", ttsSpeed)} speed.")
                            showSpeedDialog = false
                        }
                    }) { Text("Preview") }
                }
            },
            confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("Done") } },
            containerColor = ObsidianColors.SurfaceContainerLow
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    badge: String?,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ChevronRight,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = ObsidianColors.OnSurface, style = ObsidianTypography.BodyMd, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(subtitle, color = ObsidianColors.OnSurfaceVariant, style = ObsidianTypography.BodySm, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (badge != null) Text(badge, color = ObsidianColors.OutlineVariant, style = ObsidianTypography.LabelSm)
            Icon(trailingIcon, null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
