package com.nexus.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Natural-voice output for ULTRON. Uses the Android-installed voice engine;
 * the selected voice may be downloaded by Android and is not guaranteed offline.
 */
class CompanionVoice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(0.96f)
            tts.setPitch(1.0f)
        }
    }

    fun speak(text: String) {
        if (ready && text.isNotBlank()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ultron-${System.currentTimeMillis()}")
        }
    }

    fun shutdown() {
        ready = false
        tts.stop()
        tts.shutdown()
    }
}
