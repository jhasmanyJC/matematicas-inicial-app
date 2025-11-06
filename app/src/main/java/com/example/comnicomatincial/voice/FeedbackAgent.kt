package com.example.comnicomatincial.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.util.*

class FeedbackAgent(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeak: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setPitch(1.2f)
                tts?.setSpeechRate(1.0f)
                isReady = true
                pendingSpeak?.let {
                    speak(it)
                    pendingSpeak = null
                }
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) {
            pendingSpeak = text
            return
        }

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        val params = hashMapOf<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "UTT_ID"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onDone?.invoke()
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTT_ID")
    }

    fun encourageSuccess() {
        val messages = listOf(
            "¡Excelente trabajo, mi amor!",
            "¡Lo hiciste perfecto, campeón!",
            "¡Muy bien, cariño, sigue así!"
        )
        speak(messages.random())
    }

    fun encourageRetry() {
        val messages = listOf(
            "Casi, corazón, inténtalo otra vez.",
            "Uy, estuviste muy cerca, cariño.",
            "No pasa nada, vuelve a intentarlo, tú puedes."
        )
        speak(messages.random())
    }

    fun showCorrectAnswer(correctAnswer: String) {
        speak("La respuesta correcta era $correctAnswer, mi cielo. ¡Vamos a seguir aprendiendo juntos!")
    }

    fun close() {
        tts?.stop()
        tts?.shutdown()
        isReady = false
    }
}
