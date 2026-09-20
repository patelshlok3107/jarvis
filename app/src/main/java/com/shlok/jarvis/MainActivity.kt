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
    private var currentTab by mutableStateOf(0) // 0 home, 1 history, 2 rules, 3 settings, 4 onboarding

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = JarvisPreferences(this)
        TtsManager.init(this)
        // Auto-start foreground service (will show notification)
        try { JarvisForegroundService.start(this) } catch (_: Exception) {}

        // Request base permissions on first launch
        requestIfNeeded()

        val openHistory = intent.getBooleanExtra("open_history", false)
        if (openHistory) currentTab = 1

        setContent {
            JarvisTheme {
                Scaffold(
                    containerColor = Color(0xFF05070A),
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF0E1218)) {
                            NavigationBarItem(selected = currentTab==0, onClick = { currentTab=0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("JARVIS") })
                            NavigationBarItem(selected = currentTab==1, onClick = { currentTab=1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") })
                            NavigationBarItem(selected = currentTab==2, onClick = { currentTab=2 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Rules") })
                            NavigationBarItem(selected = currentTab==3, onClick = { currentTab=3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                        }
                    }
                ) { pad ->
                    Box(Modifier.padding(pad)) {
                        when (currentTab) {
                            0 -> HomeScreen(prefs, onOpenSettings = { currentTab=3 }, onOpenHistory = { currentTab=1 }, onOpenRules = { currentTab=2 }, onOpenOnboarding = { currentTab=4 })
                            1 -> HistoryScreen()
                            2 -> CallRulesScreen(prefs)
                            3 -> SettingsScreen(prefs)
                            4 -> OnboardingScreen(onDone = { currentTab=0 })
                        }
                    }
                }
            }
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
