package com.example.comnicomatincial.ui.activities

import android.content.Context
import com.example.comnicomatincial.voice.FeedbackAgent
import com.example.comnicomatincial.voice.VoskEngine
import com.example.comnicomatincial.data.ResultsDatabase
import com.example.comnicomatincial.data.ActivityResult

class ActivityBase(
    private val context: Context,
    private val kidName: String,
    private val activityName: String
) {

    private val feedback = FeedbackAgent(context)
    private val vosk = VoskEngine(
        context = context,
        onSpeechRecognized = { result -> handleVoiceResult(result) },
        mode = VoskEngine.Mode.TURN_BASED // ✅ reemplazo correcto
    )
    private val db = ResultsDatabase(context)

    private var level = 1
    private var attempts = 0
    private var correctAnswer = ""
    private var score = 0

    fun startActivitySession(expectedAnswer: String, currentLevel: Int) {
        correctAnswer = expectedAnswer
        level = currentLevel
        attempts = 0
        vosk.startListening()
        feedback.speak("Muy bien, cariño. Dime la respuesta.")
    }

    private fun handleVoiceResult(result: String) {
        attempts++
        if (result.equals(correctAnswer, ignoreCase = true)) {
            score += 10
            feedback.encourageSuccess()
            saveResult()
        } else if (attempts < 3) {
            feedback.encourageRetry()
        } else {
            feedback.showCorrectAnswer(correctAnswer)
            saveResult()
        }
    }

    private fun saveResult() {
        val result = ActivityResult(
            kidName = kidName,
            activityName = activityName,
            level = level,
            attempts = attempts,
            score = score
        )
        db.insertResult(result)
    }

    fun stop() {
        vosk.stopListening()
        feedback.close()
    }
}
