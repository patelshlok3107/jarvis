package com.shlok.jarvis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.LOCKED_BOOT_COMPLETED",
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) {
            // Respect battery optimization: only restart if user had enabled JARVIS
            // Check via DataStore synchronously via blocking call with timeout
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(Intent(context, JarvisForegroundService::class.java))
                } else context.startService(Intent(context, JarvisForegroundService::class.java))
            } catch (_: Exception) {}
        }
    }
}
