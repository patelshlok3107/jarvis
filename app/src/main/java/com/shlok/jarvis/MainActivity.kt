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
        // Critical: Make MainActivity robust - it must open even if service, TTS, or other components fail
        // The UI and background service are separate; one crashing must not take down the other
        try {
            prefs = JarvisPreferences(this)
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "Failed to init prefs", e)
            // Create a fallback prefs to allow UI to open
            prefs = JarvisPreferences(this)
        }
        try {
            TtsManager.init(this)
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "TTS init failed, continuing without TTS", e)
        }
        // Log app start for diagnostics
        try {
            com.shlok.jarvis.storage.JarvisLogger.logSync(this, "APP_STARTED", "MainActivity onCreate")
        } catch (_: Exception) {}
        // Auto-start foreground service (will show notification) - critical for background call handling
        // This is independent of browser/Vercel; native services are the source of truth
        // Do not let service start failure crash the UI
        try {
            JarvisForegroundService.start(this)
            com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_START_REQUESTED", "from MainActivity")
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "Service start failed", e)
            try {
                com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_START_FAILED", e.message ?: "unknown")
            } catch (_: Exception) {}
        }

        // Request base permissions on first launch - do not let this crash the UI
        try {
            requestIfNeeded()
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "Permission request failed", e)
        }

        val openHistory = try {
            intent.getBooleanExtra("open_history", false)
        } catch (_: Exception) { false }
        if (openHistory) currentTab = 1

        // Make setContent robust - catch any Compose/theme initialization errors
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
                            // Each screen is wrapped to catch its own errors
                            try {
                                when (currentTab) {
                                    0 -> HomeScreen(prefs, onOpenSettings = { currentTab=3 }, onOpenHistory = { currentTab=1 }, onOpenRules = { currentTab=2 }, onOpenOnboarding = { currentTab=5 })
                                    1 -> HistoryScreen()
                                    2 -> CallRulesScreen(prefs)
                                    3 -> SettingsScreen(prefs)
                                    4 -> OnboardingScreen(onDone = { currentTab=0 })
                                    5 -> PhoneConnectionScreen()
                                    6 -> DiagnosticsScreen(prefs)
                                    else -> HomeScreen(prefs, onOpenSettings = { currentTab=3 }, onOpenHistory = { currentTab=1 }, onOpenRules = { currentTab=2 }, onOpenOnboarding = { currentTab=5 })
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("JarvisMain", "Screen $currentTab failed", e)
                                // Show error screen instead of crashing
                                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Text("JARVIS", color = Color(0xFF00E5FF), letterSpacing = 8.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Something needs attention", color = Color.White, fontSize = 14.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Screen $currentTab failed: ${e.message}", color = Color(0xFFFFC107), fontSize = 11.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = { currentTab = 0 }) { Text("Go Home") }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { currentTab = 6 }) { Text("Diagnostics") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "setContent failed", e)
            // Fallback: show a minimal Activity without Compose if even theme fails
            try {
                com.shlok.jarvis.storage.JarvisLogger.logSync(this, "UI_CRASH", e.message ?: "unknown")
            } catch (_: Exception) {}
            // Try to show a simple TextView as fallback
            val tv = android.widget.TextView(this)
            tv.text = "JARVIS\n\nUI failed to start:\n${e.message}\n\nService is still running (check notification).\nOpen Diagnostics."
            tv.setTextColor(android.graphics.Color.WHITE)
            tv.setBackgroundColor(android.graphics.Color.parseColor("#05070A"))
            tv.setPadding(32, 32, 32, 32)
            setContentView(tv)
        }
    }

    private fun requestIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_CONTACTS)
        if (needed.isNotEmpty()) permLauncher.launch(needed.toTypedArray())
    }
}
