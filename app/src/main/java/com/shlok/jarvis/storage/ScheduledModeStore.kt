package com.shlok.jarvis.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.shlok.jarvis.engine.ScheduledMode
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.engine.PrivacyLevel
import org.json.JSONArray
import org.json.JSONObject

private val SCHEDULED_KEY = stringPreferencesKey("scheduled_modes_json")

object ScheduledModeStore {
    fun flow(ctx: Context): Flow<List<ScheduledMode>> = ctx.dataStore.data.map { prefs ->
        try {
            val raw = prefs[SCHEDULED_KEY] ?: "[]"
            val arr = JSONArray(raw)
            val list = mutableListOf<ScheduledMode>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(ScheduledMode(
                    id = o.getString("id"),
                    mode = SmartMode.fromString(o.getString("mode")),
                    startTime = o.getLong("startTime"),
                    endTime = o.getLong("endTime"),
                    privacy = try { PrivacyLevel.valueOf(o.getString("privacy")) } catch (_: Exception) { PrivacyLevel.MEDIUM },
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                ))
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    suspend fun add(ctx: Context, mode: ScheduledMode) {
        val current = flow(ctx).first().toMutableList()
        current.add(mode)
        save(ctx, current)
    }

    suspend fun remove(ctx: Context, id: String) {
        val filtered = flow(ctx).first().filter { it.id != id }
        save(ctx, filtered)
    }

    suspend fun getActive(ctx: Context, now: Long = System.currentTimeMillis()): ScheduledMode? {
        return flow(ctx).first().filter { now in it.startTime..it.endTime }
            .maxByOrNull { it.mode.priority }
    }

    suspend fun clearExpired(ctx: Context) {
        val now = System.currentTimeMillis()
        val filtered = flow(ctx).first().filter { it.endTime > now }
        save(ctx, filtered)
    }

    private suspend fun save(ctx: Context, list: List<ScheduledMode>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("mode", m.mode.name)
                put("startTime", m.startTime)
                put("endTime", m.endTime)
                put("privacy", m.privacy.name)
                put("createdAt", m.createdAt)
            })
        }
        ctx.dataStore.edit { it[SCHEDULED_KEY] = arr.toString() }
    }
}
