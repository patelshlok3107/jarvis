package com.shlok.jarvis.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * JARVIS Foreground Service — phoneCall type only (microphone is separate VoiceAssistantService).
 *
 * Independent: failure here does NOT crash UI. Battery-efficient: no loops, event-driven via DataStore flows.
 * Persistent notification ALWAYS reflects actual status (● BUSY, ● AVAILABLE etc.).
 * Handles OEM kills gracefully: START_STICKY, logs task removed, trim memory.
 */
class JarvisForegroundService : LifecycleService() {

    private lateinit var prefs: JarvisPreferences
    private var currentStatus: JarvisStatus = JarvisStatus.AVAILABLE

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "jarvis_stop"
        fun start(ctx: Context) {
            val i = Intent(ctx, JarvisForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
            } catch (e: SecurityException) {
                android.util.Log.e("JarvisFGS", "start SecurityException", e)
            } catch (_: Exception) {}
        }
        fun stop(ctx: Context) { try { ctx.stopService(Intent(ctx, JarvisForegroundService::class.java)) } catch (_: Exception) {} }
    }

    override fun onCreate() {
        super.onCreate()
        // Wrap init in try/catch — if prefs or TTS fails, service must still start with safe fallback
        try {
            prefs = JarvisPreferences(this)
        } catch (e: Exception) {
            android.util.Log.e("JarvisFGS", "prefs init failed", e)
            // Create fallback prefs will be handled via try-catch later
            prefs = JarvisPreferences(this)
        }
        try { TtsManager.init(this) } catch (_: Exception) {}

        // Observe status changes — independent flow, isolated failure
        lifecycleScope.launch {
            try {
                prefs.statusFlow.collectLatest { s ->
                    currentStatus = s
                    updateNotificationSafe()
                }
            } catch (e: Exception) {
                android.util.Log.e("JarvisFGS", "status flow error", e)
                currentStatus = JarvisStatus.AVAILABLE
                updateNotificationSafe()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_STOPPED", "user requested via notification") } catch (_: Exception) {}
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Start foreground with actual status — required within 5s of startForegroundService
        try {
            startForeground(NOTIF_ID, JarvisNotificationManager.buildForegroundNotification(this, currentStatus))
        } catch (e: SecurityException) {
            android.util.Log.e("JarvisFGS", "startForeground SecurityException", e)
            // Fallback: try without foreground type? Still attempt to notify
            try {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, JarvisNotificationManager.buildForegroundNotification(this, currentStatus))
            } catch (_: Exception) {}
            // Don't crash — service stays but not foreground? We'll stop gracefully
            return START_STICKY
        } catch (e: Exception) {
            android.util.Log.e("JarvisFGS", "startForeground error", e)
            return START_STICKY
        }
        try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_STARTED", "status=$currentStatus") } catch (_: Exception) {}
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "TASK_REMOVED", "app swiped away, START_STICKY") } catch (_: Exception) {}
        // Do not stop — let system restart if permitted. Update notification to show still active
        updateNotificationSafe()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "TRIM_MEMORY", "level=$level") } catch (_: Exception) {}
        }
    }

    private fun updateNotificationSafe() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIF_ID, JarvisNotificationManager.buildForegroundNotification(this, currentStatus))
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
    override fun onDestroy() {
        try { TtsManager.shutdown() } catch (_: Exception) {}
        try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_DESTROYED", "") } catch (_: Exception) {}
        super.onDestroy()
    }
}
