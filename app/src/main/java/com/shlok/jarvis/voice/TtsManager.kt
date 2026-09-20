package com.shlok.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object TtsManager {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    fun setLanguage(langCode: String) {
        val locale = when(langCode){
            "hi" -> Locale("hi","IN")
            "gu" -> Locale("gu","IN")
            else -> Locale.US
        }
        tts?.language = locale
    }

    fun setSpeedPitch(speed: Float, pitch: Float) {
        tts?.setSpeechRate(speed.coerceIn(0.5f, 2f))
        tts?.setPitch(pitch.coerceIn(0.5f, 2f))
    }

    suspend fun speak(text: String): Boolean {
        val engine = tts ?: return false
        if (!ready) return false
        return suspendCancellableCoroutine { cont ->
            engine.setOnUtteranceProgressListener(object: UtteranceProgressListener(){
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { if (cont.isActive) cont.resume(true) }
                @Deprecated("Deprecated") override fun onError(id: String?) { if(cont.isActive) cont.resume(false) }
                override fun onError(id: String?, e: Int) { if(cont.isActive) cont.resume(false) }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_${System.currentTimeMillis()}")
        }
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.shutdown(); tts=null; ready=false }
    fun isReady() = ready
}
