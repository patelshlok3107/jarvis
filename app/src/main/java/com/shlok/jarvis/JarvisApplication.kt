package com.shlok.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createChannels()
    }
    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("jarvis_service", "JARVIS Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps JARVIS running in background"
                setShowBadge(false)
            })
            nm.createNotificationChannel(NotificationChannel("jarvis_calls", "JARVIS Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Call handling notifications"
            })
            nm.createNotificationChannel(NotificationChannel("jarvis_messages", "JARVIS Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Messages left for you"
            })
        }
    }
}
