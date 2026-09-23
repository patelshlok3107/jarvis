package com.shlok.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.ui.screens.*
import com.shlok.jarvis.ui.theme.JarvisTheme
import com.shlok.jarvis.ui.theme.ObsidianColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: JarvisPreferences
    private var currentTab by mutableStateOf(0) // 0 HOME, 1 ASSISTANT, 2 CALLS, 3 HISTORY, 4 SETTINGS
    private var showSetup by mutableStateOf<Boolean?>(null) // null = loading, true = wizard, false = main
    private var useFallback by mutableStateOf(false)

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge — respect status/navigation bars, display cutouts
        try {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (_: Exception) {}

        // --- CRASH-SAFE PREFS INIT ---
        try {
            prefs = JarvisPreferences(this)
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "prefs init failed", e)
            useFallback = true
            try {
                val tv = android.widget.TextView(this)
                tv.text = "JARVIS\nPrefs init failed\nService may still be running"
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.setBackgroundColor(android.graphics.Color.parseColor("#05070A"))
                tv.setPadding(32, 32, 32, 32)
                setContentView(tv)
            } catch (_: Exception) {}
            // Still try to show fallback UI via Compose after
        }

        // If prefs init failed, create a dummy prefs (will fail gracefully inside screens)
        if (!::prefs.isInitialized) {
            try { prefs = JarvisPreferences(this) } catch (_: Exception) {}
        }

        // Check onboarding state async — don't block UI
        if (::prefs.isInitialized) {
            lifecycleScope.launch {
                try {
                    // Small delay to let DataStore load without blocking
                    delay(200)
                    val data = try { prefs.dataFlow.first() } catch (_: Exception) { null }
                    val done = data?.get(com.shlok.jarvis.storage.PrefKeys.ONBOARDING_DONE) ?: false
                    showSetup = !done
                } catch (_: Exception) {
                    showSetup = false
                }
            }
        } else {
            showSetup = false
        }

        // --- MAIN UI — must never crash ---
        try {
            setContent {
                JarvisTheme {
                    if (showSetup == null) {
                        // Loading splash — Obsidian void, never crashes
                        Box(Modifier.fillMaxSize().background(ObsidianColors.Background), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text("J.A.R.V.I.S", color = ObsidianColors.PrimaryContainer, letterSpacing = 6.sp, fontSize = 18.sp, style = com.shlok.jarvis.ui.theme.ObsidianTypography.LabelMd)
                                Spacer(Modifier.height(8.dp))
                                CircularProgressIndicator(color = ObsidianColors.PrimaryContainer, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    } else if (showSetup == true) {
                        SetupWizardScreen(
                            prefs = if (::prefs.isInitialized) prefs else null,
                            onComplete = {
                                showSetup = false
                                currentTab = 0
                            }
                        )
                    } else {
                        Scaffold(
                            containerColor = ObsidianColors.Background,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = {
                                // STITCH bottom nav — obsidian surface 85% + hairline, 64dp height
                                NavigationBar(
                                    containerColor = ObsidianColors.Surface.copy(alpha = 0.85f),
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                        label = { Text("HOME", fontSize = 10.sp, letterSpacing = 1.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ObsidianColors.PrimaryContainer,
                                            selectedTextColor = ObsidianColors.PrimaryContainer,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = ObsidianColors.OnSurfaceVariant,
                                            unselectedTextColor = ObsidianColors.OnSurfaceVariant
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                                        label = { Text("AI CORE", fontSize = 10.sp, letterSpacing = 1.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ObsidianColors.PrimaryContainer,
                                            selectedTextColor = ObsidianColors.PrimaryContainer,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = ObsidianColors.OnSurfaceVariant,
                                            unselectedTextColor = ObsidianColors.OnSurfaceVariant
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 2,
                                        onClick = { currentTab = 2 },
                                        icon = { Icon(Icons.Default.Security, contentDescription = null) },
                                        label = { Text("SECURE", fontSize = 10.sp, letterSpacing = 1.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ObsidianColors.PrimaryContainer,
                                            selectedTextColor = ObsidianColors.PrimaryContainer,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = ObsidianColors.OnSurfaceVariant,
                                            unselectedTextColor = ObsidianColors.OnSurfaceVariant
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 3,
                                        onClick = { currentTab = 3 },
                                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                                        label = { Text("LOGS", fontSize = 10.sp, letterSpacing = 1.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ObsidianColors.PrimaryContainer,
                                            selectedTextColor = ObsidianColors.PrimaryContainer,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = ObsidianColors.OnSurfaceVariant,
                                            unselectedTextColor = ObsidianColors.OnSurfaceVariant
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 4,
                                        onClick = { currentTab = 4 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        label = { Text("SYSTEM", fontSize = 10.sp, letterSpacing = 1.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ObsidianColors.PrimaryContainer,
                                            selectedTextColor = ObsidianColors.PrimaryContainer,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = ObsidianColors.OnSurfaceVariant,
                                            unselectedTextColor = ObsidianColors.OnSurfaceVariant
                                        )
                                    )
                                }
                            }
                        ) { pad ->
                            Box(Modifier.padding(pad)) {
                                when (currentTab) {
                                    0 -> SafeScreen { HomeScreenPremium(prefs, onOpenSettings = { currentTab = 4 }, onOpenAssistant = { currentTab = 1 }) }
                                    1 -> SafeScreen { AssistantScreen(prefs) }
                                    2 -> SafeScreen { CallsScreen() }
                                    3 -> SafeScreen { HistoryScreen() }
                                    4 -> SafeScreen { SettingsScreen(prefs, onOpenDiagnostics = { /* push diagnostics */ }, onOpenSetup = { showSetup = true }) }
                                    else -> SafeScreen { HomeScreenPremium(prefs, onOpenSettings = { currentTab = 4 }, onOpenAssistant = { currentTab = 1 }) }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisMain", "setContent failed", e)
            try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "UI_CRASH", e.message ?: "unknown") } catch (_: Exception) {}
            try {
                val tv = android.widget.TextView(this)
                tv.text = "JARVIS\n\nUI failed to start:\n${e.message}\n\nService is still running (check notification).\nRestart app."
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.setBackgroundColor(android.graphics.Color.parseColor("#05070A"))
                tv.setPadding(32, 32, 32, 32)
                setContentView(tv)
            } catch (_: Exception) {}
        }

        // Async init after UI is shown — never block UI, never crash UI
        try {
            Thread {
                try { com.shlok.jarvis.voice.TtsManager.init(this) } catch (_: Exception) {}
                try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "APP_STARTED", "MainActivity onCreate") } catch (_: Exception) {}
                // Start foreground service — phoneCall type, safe, no mic
                try { com.shlok.jarvis.service.JarvisForegroundService.start(this) } catch (_: Exception) {}
            }.start()
            // Request permissions gently — don't force
            try { requestIfNeeded() } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    private fun requestIfNeeded() {
        val needed = mutableListOf<String>()
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) {}
        if (needed.isNotEmpty()) {
            try { permLauncher.launch(needed.toTypedArray()) } catch (_: Exception) {}
        }
    }
}

@Composable
private fun SafeScreen(content: @Composable () -> Unit) {
    // Compose does not support try/catch around composable invocations directly.
    // Each screen is already isolated with its own error handling for data flows.
    // This wrapper simply calls content; outer setContent try/catch handles fatal init errors.
    content()
}
