package com.omnidot.automation

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object VoiceFeedbackManager {

    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)
    private var initDeferred = CompletableDeferred<Boolean>()

    @Synchronized
    private fun getOrInitTts(context: Context): CompletableDeferred<Boolean> {
        if (tts != null && isReady.get()) {
            val ready = CompletableDeferred<Boolean>()
            ready.complete(true)
            return ready
        }
        if (tts != null) {
            return initDeferred
        }

        initDeferred = CompletableDeferred()
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                try {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts?.setAudioAttributes(attributes)
                } catch (_: Throwable) {}
                isReady.set(true)
                initDeferred.complete(true)
            } else {
                isReady.set(false)
                initDeferred.complete(false)
            }
        }
        return initDeferred
    }

    suspend fun speak(
        context: Context,
        text: String,
        pitch: Float = 1.0f,
        rate: Float = 1.0f,
        waitForCompletion: Boolean = false,
    ) = withContext(Dispatchers.Main) {
        val deferred = getOrInitTts(context)
        val success = withTimeoutOrNull(2500L) { deferred.await() } ?: false
        if (!success) return@withContext

        val engine = tts ?: return@withContext
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))

        val utteranceId = "omnidot_voice_${System.currentTimeMillis()}"
        val completionDeferred = CompletableDeferred<Unit>()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                completionDeferred.complete(Unit)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                completionDeferred.complete(Unit)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                completionDeferred.complete(Unit)
            }
        })

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

        if (waitForCompletion) {
            withTimeoutOrNull(4000L) {
                completionDeferred.await()
            }
        }
    }
}
