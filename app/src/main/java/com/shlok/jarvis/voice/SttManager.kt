package com.shlok.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * Speech-to-text via Android SpeechRecognizer.
 * Note: continuous wake-word "Hey Jarvis" is NOT reliable without foreground mic + battery cost
 * and is restricted on many OEMs. We use tap-to-talk + notification shortcut pattern
 * and document the limitation. Future: integrate on-device wake-word (e.g. openWakeWord) if granted.
 */
class SttManager(private val context: Context) {

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listenFlow(languageCode: String = "en-IN") = callbackFlow<String> {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when(languageCode){ "hi"->"hi-IN"; "gu"->"gu-IN"; else->"en-IN" })
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer.setRecognitionListener(object: RecognitionListener{
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(e: Int) { close() }
            override fun onResults(r: Bundle?) {
                val list = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                list?.firstOrNull()?.let { trySend(it) }
                close()
            }
            override fun onPartialResults(p: Bundle?) {
                val list = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                list?.firstOrNull()?.let { trySend(it) }
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        recognizer.startListening(intent)
        awaitClose { recognizer.destroy() }
    }
}
