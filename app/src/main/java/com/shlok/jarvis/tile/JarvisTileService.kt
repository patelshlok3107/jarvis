package com.shlok.jarvis.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class JarvisTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val prefs = JarvisPreferences(this@JarvisTileService)
            val status = try { prefs.statusFlow.first() } catch (_:Exception){ JarvisStatus.AVAILABLE }
            qsTile?.apply {
                label = "JARVIS — ${status.displayName}"
                subtitle = status.subtitle
                state = if (status == JarvisStatus.AVAILABLE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val prefs = JarvisPreferences(this@JarvisTileService)
            val current = try { prefs.statusFlow.first() } catch(_:Exception){ JarvisStatus.AVAILABLE }
            // Cycle: AVAILABLE -> BUSY -> DND -> AVAILABLE (quick toggle)
            val next = when(current){
                JarvisStatus.AVAILABLE -> JarvisStatus.BUSY
                JarvisStatus.BUSY -> JarvisStatus.DND
                else -> JarvisStatus.AVAILABLE
            }
            prefs.setStatus(next)
            qsTile?.apply {
                label = "JARVIS — ${next.displayName}"
                state = if (next==JarvisStatus.AVAILABLE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                updateTile()
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
