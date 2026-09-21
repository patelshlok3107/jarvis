package com.shlok.jarvis.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shlok.jarvis.MainActivity
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Background-first heart of JARVIS.
 * - Persistent notification (required by OS for foreground service)
 * - Observes status changes, updates notification
 * - Keeps TTS ready, monitors health
 * - Battery-efficient: no polling; flow-based
 */
class JarvisForegroundService : LifecycleService() {

    private lateinit var prefs: JarvisPreferences
    private var currentStatus: JarvisStatus = JarvisStatus.AVAILABLE

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "jarvis_stop"
        const val ACTION_TOGGLE_LISTEN = "jarvis_toggle_listen"
        fun start(ctx: Context) {
            val i = Intent(ctx, JarvisForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, JarvisForegroundService::class.java)) }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = JarvisPreferences(this)
        TtsManager.init(this)

        lifecycleScope.launch {
            prefs.statusFlow.collectLatest { s ->
                currentStatus = s
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                // User requested stop via notification action
                try {
                    com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_STOPPED", "user requested via notification")
                } catch (_: Exception) {}
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_LISTEN -> { /* handled via MainActivity */ }
        }
        // Ensure notification is shown before work, required for foreground service
        startForeground(NOTIF_ID, buildNotification(currentStatus))
        // Log service start for diagnostics
        try {
            com.shlok.jarvis.storage.JarvisLogger.logSync(this, "SERVICE_STARTED", "status=$currentStatus")
        } catch (_: Exception) {}
        // START_STICKY: OS may restart us after kill where permitted. Handles Doze/App Standby.
        // We do not use while(true) or polling; we are event-driven via Telecom and DataStore flows.
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // OEMs may kill service when task removed (swiped away). We stay START_STICKY, but also ensure
        // we do not stop ourselves. The system may restart us if permitted.
        // We update notification to reflect that service is still active, not stopped.
        try {
            com.shlok.jarvis.storage.JarvisLogger.logSync(this, "TASK_REMOVED", "app swiped away, service remains START_STICKY")
        } catch (_: Exception) {}
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Handle low memory: we are lightweight, no heavy work, so we just log.
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            try {
                com.shlok.jarvis.storage.JarvisLogger.logSync(this, "TRIM_MEMORY", "level=$level")
            } catch (_: Exception) {}
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(currentStatus))
    }

    private fun buildNotification(status: JarvisStatus): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, JarvisForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dot = when (status) {
            JarvisStatus.AVAILABLE -> "● AVAILABLE"
            JarvisStatus.BUSY -> "● BUSY"
            JarvisStatus.DND -> "◐ DND"
            JarvisStatus.DRIVING -> "◑ DRIVING"
            JarvisStatus.SLEEPING -> "◒ SLEEPING"
            JarvisStatus.MEETING -> "◓ MEETING"
        }
        return NotificationCompat.Builder(this, "jarvis_service")
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle("JARVIS — $dot")
            .setContentText(status.subtitle + " • Tap to open")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
    override fun onDestroy() { TtsManager.shutdown(); super.onDestroy() }
}
