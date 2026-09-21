package com.shlok.jarvis.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val MIC_LEVEL = stringPreferencesKey("voice_mic_level")
private val WAKE_LAST = longPreferencesKey("voice_wake_last")
private val WAKE_COUNT = stringPreferencesKey("voice_wake_count")
private val VOICE_STATE = stringPreferencesKey("voice_state")
private val LAST_AUDIO = longPreferencesKey("voice_last_audio")

object VoiceDiagnostics {
    fun micLevelFlow(ctx: Context): Flow<String> = ctx.dataStore.data.map { it[MIC_LEVEL] ?: "0" }
    fun wakeLastFlow(ctx: Context): Flow<Long> = ctx.dataStore.data.map { it[WAKE_LAST] ?: 0L }
    fun stateFlow(ctx: Context): Flow<String> = ctx.dataStore.data.map { it[VOICE_STATE] ?: "STOPPED" }
    fun lastAudioFlow(ctx: Context): Flow<Long> = ctx.dataStore.data.map { it[LAST_AUDIO] ?: 0L }

    suspend fun setMicLevel(ctx: Context, level: String) { ctx.dataStore.edit { it[MIC_LEVEL] = level } }
    suspend fun setWakeLast(ctx: Context, time: Long) { ctx.dataStore.edit { it[WAKE_LAST] = time } }
    suspend fun setState(ctx: Context, state: String) { ctx.dataStore.edit { it[VOICE_STATE] = state } }
    suspend fun setLastAudio(ctx: Context, time: Long) { ctx.dataStore.edit { it[LAST_AUDIO] = time } }
    suspend fun setWakeCount(ctx: Context, count: String) { ctx.dataStore.edit { it[WAKE_COUNT] = count } }
}
