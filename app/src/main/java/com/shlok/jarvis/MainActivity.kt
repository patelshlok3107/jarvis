package com.shlok.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.shlok.jarvis.service.JarvisForegroundService
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.ui.screens.*
import com.shlok.jarvis.ui.theme.JarvisTheme
import com.shlok.jarvis.voice.TtsManager

class MainActivity : ComponentActivity() {

    private lateinit var prefs: JarvisPreferences
    private var currentTab by mutableStateOf(0) // 0 home, 1 history, 2 rules, 3 settings, 4 onboarding, 5 phone, 6 diagnostics

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ultra-minimal onCreate - must never crash
        // All heavy init is done async after UI is shown
        try {
            prefs = JarvisPreferences(this)
        } catch (_: Exception) {
            // Last resort - plain TextView
            try {
                val tv = android.widget.TextView(this)
                tv.text = "JARVIS\nPrefs init failed\nService may still be running"
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.setBackgroundColor(android.graphics.Color.parseColor("#05070A"))
                tv.setPadding(32, 32, 32, 32)
                setContentView(tv)
            } catch (_: Exception) {}
            return
        }

        // Make setContent robust - catch any Compose/theme initialization errors
        // Heavy init (TTS, service, permissions) is done async after UI is shown, so UI never waits
        try {
            setContent {
                JarvisTheme {
                    Scaffold(
                        containerColor = Color(0xFF05070A),
                        bottomBar = {
                            NavigationBar(containerColor = Color(0xFF0E1218)) {
                                NavigationBarItem(selected = currentTab==0, onClick = { currentTab=0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("JARVIS") })
                                NavigationBarItem(selected = currentTab==5, onClick = { currentTab=5 }, icon = { Icon(Icons.Default.Phone, null) }, label = { Text("Phone") })
                                NavigationBarItem(selected = currentTab==1, onClick = { currentTab=1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") })
                                NavigationBarItem(selected = currentTab==6, onClick = { currentTab=6 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Diag") })
                                NavigationBarItem(selected = currentTab==3, onClick = { currentTab=3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                            }
                        }
                ) { pad ->
                    Box(Modifier.padding(pad)) {
                        when (currentTab) {
                            0 -> HomeScreenSimple(prefs, onOpenSettings = { currentTab=3 }, onOpenHistory = { currentTab=1 }, onOpenRules = { currentTab=2 }, onOpenOnboarding = { currentTab=5 })
                            1 -> HistoryScreen()
                            2 -> CallRulesScreen(prefs)
                            3 -> SettingsScreen(prefs)
                            4 -> OnboardingScreen(onDone = { currentTab=0 })
                            5 -> PhoneConnectionScreen()
                            6 -> DiagnosticsScreen(prefs)
                        }
                    }
                }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "setContent failed", e)
            try {
                com.shlok.jarvis.storage.JarvisLogger.logSync(this, "UI_CRASH", e.message ?: "unknown")
            } catch (_: Exception) {}
            val tv = android.widget.TextView(this)
            tv.text = "JARVIS\n\nUI failed to start:\n${e.message}\n\nService is still running (check notification).\nOpen Diagnostics."
            tv.setTextColor(android.graphics.Color.WHITE)
            tv.setBackgroundColor(android.graphics.Color.parseColor("#05070A"))
            tv.setPadding(32, 32, 32, 32)
            setContentView(tv)
        }

        // Async init after UI is shown - never block UI
        try {
            // TTS init async
            Thread {
                try { com.shlok.jarvis.voice.TtsManager.init(this) } catch (_: Exception) {}
                try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "APP_STARTED", "MainActivity onCreate async") } catch (_: Exception) {}
                try { com.shlok.jarvis.service.JarvisForegroundService.start(this) } catch (_: Exception) {}
            }.start()
            // Permissions async
            try { requestIfNeeded() } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    private fun requestIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_CONTACTS)
        if (needed.isNotEmpty()) permLauncher.launch(needed.toTypedArray())
    }
}
