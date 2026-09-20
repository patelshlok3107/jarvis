package com.shlok.jarvis.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.shlok.jarvis.data.*

internal val Context.dataStore by preferencesDataStore(name = "jarvis_prefs")

object PrefKeys {
    val STATUS = stringPreferencesKey("status")
    val USER_NAME = stringPreferencesKey("user_name")
    val JARVIS_NAME = stringPreferencesKey("jarvis_name")
    val UNKNOWN_ACTION = stringPreferencesKey("unknown_action")
    val TTS_SPEED = floatPreferencesKey("tts_speed")
    val TTS_PITCH = floatPreferencesKey("tts_pitch")
    val TTS_LANG = stringPreferencesKey("tts_lang") // en / hi / gu
    val VOICE_GENDER = stringPreferencesKey("voice_gender")
    val AI_PROVIDER = stringPreferencesKey("ai_provider") // local / openai / gemini / custom
    val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
    val RECORDING_ENABLED = booleanPreferencesKey("recording_enabled")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    // templates
    val TPL_BUSY = stringPreferencesKey("tpl_busy")
    val TPL_DND = stringPreferencesKey("tpl_dnd")
    val TPL_DRIVING = stringPreferencesKey("tpl_driving")
    val TPL_SLEEPING = stringPreferencesKey("tpl_sleeping")
    val TPL_MEETING = stringPreferencesKey("tpl_meeting")
    // encrypted transcript store key prefix handled separately
}

class JarvisPreferences(private val context: Context) {
    val statusFlow: Flow<JarvisStatus> = context.dataStore.data.map {
        JarvisStatus.fromString(it[PrefKeys.STATUS] ?: JarvisStatus.AVAILABLE.name)
    }
    suspend fun setStatus(s: JarvisStatus) {
        context.dataStore.edit { it[PrefKeys.STATUS] = s.name }
    }
    suspend fun setString(key: Preferences.Key<String>, v: String) { context.dataStore.edit { it[key]=v } }
    suspend fun setFloat(key: Preferences.Key<Float>, v: Float) { context.dataStore.edit { it[key]=v } }
    suspend fun setBool(key: Preferences.Key<Boolean>, v: Boolean) { context.dataStore.edit { it[key]=v } }

    fun templatesFlow(): Flow<ResponseTemplates> = context.dataStore.data.map {
        ResponseTemplates(
            busy = it[PrefKeys.TPL_BUSY] ?: ResponseTemplates().busy,
            dnd = it[PrefKeys.TPL_DND] ?: ResponseTemplates().dnd,
            driving = it[PrefKeys.TPL_DRIVING] ?: ResponseTemplates().driving,
            sleeping = it[PrefKeys.TPL_SLEEPING] ?: ResponseTemplates().sleeping,
            meeting = it[PrefKeys.TPL_MEETING] ?: ResponseTemplates().meeting,
        )
    }
    suspend fun saveTemplates(t: ResponseTemplates) {
        context.dataStore.edit {
            it[PrefKeys.TPL_BUSY]=t.busy; it[PrefKeys.TPL_DND]=t.dnd
            it[PrefKeys.TPL_DRIVING]=t.driving; it[PrefKeys.TPL_SLEEPING]=t.sleeping; it[PrefKeys.TPL_MEETING]=t.meeting
        }
    }
    val dataFlow get() = context.dataStore.data
}

/**
 * Encrypted storage for transcripts / sensitive data.
 * Uses AndroidX Security EncryptedSharedPreferences (AES256-GCM).
 */
class EncryptedStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "jarvis_encrypted", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    fun get(key: String): String? = prefs.getString(key, null)
    fun remove(key: String) { prefs.edit().remove(key).apply() }
    fun clear() { prefs.edit().clear().apply() }
}
