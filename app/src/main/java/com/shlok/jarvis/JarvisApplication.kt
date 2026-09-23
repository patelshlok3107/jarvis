package com.shlok.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * JARVIS Application — must never crash. All init is wrapped.
 * Creates notification channels required for foreground services and call handling.
 */
class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Each channel creation isolated — one failure must not block others
        try { createChannels() } catch (e: Exception) { android.util.Log.e("JarvisApp", "channel error", e) }
        // Async lightweight init — don't block main thread
        try {
            Thread {
                try { com.shlok.jarvis.voice.TtsManager.init(this) } catch (_: Exception) {}
                try { com.shlok.jarvis.storage.JarvisLogger.logSync(this, "APP_CREATED", "Application onCreate") } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }
    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = try { getSystemService(NotificationManager::class.java) } catch (_: Exception) { return }
            try {
                nm.createNotificationChannel(NotificationChannel("jarvis_service", "JARVIS Service", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps JARVIS running in background. Shows current mode and listening status."
                    setShowBadge(false)
                    enableVibration(false)
                })
            } catch (_: Exception) {}
            try {
                nm.createNotificationChannel(NotificationChannel("jarvis_calls", "JARVIS Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Incoming call handling — JARVIS screened or handled notifications"
                    enableVibration(true)
                })
            } catch (_: Exception) {}
            try {
                nm.createNotificationChannel(NotificationChannel("jarvis_messages", "JARVIS Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Messages left for you when JARVIS handled a call"
                })
            } catch (_: Exception) {}
        }
    }
}
