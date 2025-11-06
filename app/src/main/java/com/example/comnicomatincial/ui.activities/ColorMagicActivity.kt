package com.example.comnicomatincial.ui.activities

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import com.example.comnicomatincial.data.ActivityResult
import com.example.comnicomatincial.data.ResultsDatabase
import com.example.comnicomatincial.voice.FeedbackAgent
import com.example.comnicomatincial.voice.VoskEngine
import com.example.comnicomatincial.R
import kotlinx.coroutines.*
import java.text.Normalizer
import kotlin.math.min
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

object ShapeLevelsData {
    val level1 = listOf(
        Pair(R.drawable.circulo, "círculo"),
        Pair(R.drawable.cuadrado, "cuadrado"),
        Pair(R.drawable.triangulo, "triángulo"),
        Pair(R.drawable.rectangulo, "rectángulo"),
        Pair(R.drawable.estrella, "estrella"),
        Pair(R.drawable.corazon, "corazón")
    )
}

@Composable
fun ShapeMagicActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { ShapeLevelsData.level1.shuffled() }

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val currentAnswer by remember { derivedStateOf { items[index].second } }

    var message by remember { mutableStateOf("¿Qué figura es esta?") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    val maxAttempts = 3
    var correctCount by remember { mutableStateOf(0) }

    val requiredCorrectAnswers = 3
    var isListening by remember { mutableStateOf(false) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var listenStartTime by remember { mutableStateOf(0L) }

    var showConfetti by remember { mutableStateOf(false) }

    var isAssistantSpeaking by remember { mutableStateOf(false) }
    var lastTtsDuration by remember { mutableStateOf(0L) }

    // Contador de guardados para depuración
    var saveCount by remember { mutableStateOf(0) }

    fun calculateScore(attempts: Int, maxAttempts: Int = 3): Int {
        return when (attempts) {
            0 -> 100
            1 -> 80
            2 -> 60
            else -> 0
        }
    }

    suspend fun speakAndWait(text: String, delayAfter: Long = 800L) {
        isAssistantSpeaking = true
        feedback.speak(text)
        val estimated = text.length * 75L + delayAfter
        lastTtsDuration = estimated
        delay(estimated)
        isAssistantSpeaking = false
    }

    fun similar(a: String, b: String): Boolean {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        val distance = dp[m][n]
        val maxLen = maxOf(m, n).toFloat()
        return 1 - (distance / maxLen) >= 0.8
    }

    fun saveResult(result: ActivityResult) {
        saveCount++
        db.insertResult(result)
        Log.i(
            "DB_TRACK",
            """
            💾 [#${saveCount}] GUARDADO LOCAL
            ├ kidName: ${result.kidName}
            ├ activityName: ${result.activityName}
            ├ level: ${result.level}
            ├ attempts: ${result.attempts}
            ├ score: ${result.score}
            ├ correctAnswers: ${result.correctAnswers}
            └ timestamp: ${result.timestamp}
            """.trimIndent()
        )
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening) return
        if (isAssistantSpeaking) {
            Log.w("ShapeMagic", "🛑 Ignorado: asistente aún hablando.")
            return
        }

        Log.d("ShapeMagic", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") && !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(rawText)
        val extractedText = match?.groups?.get(1)?.value?.trim() ?: rawText.trim()
        if (extractedText.isBlank()) return

        val recognized = Normalizer.normalize(extractedText.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .trim()

        Log.d("ShapeMagic", "🎙️ Texto reconocido limpio: '$recognized'")

        val frasesSistema = listOf(
            "no es esa figura", "no es esta figura", "intenta otra vez", "era", "muy bien",
            "sigamos", "no te preocupes", "dime que figura", "ahora dime", "bien ahora dime",
            "muy bien ahora dime", "la respuesta era", "excelente trabajo", "felicidades",
            "ahora dime que figura es esta"
        )

        if (frasesSistema.any { recognized.contains(it) }) {
            Log.w("ShapeMagic", "🛑 Ignorado: frase del asistente detectada → '$recognized'")
            return
        }

        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < lastTtsDuration + 300L) {
            Log.w("ShapeMagic", "⚠️ Ignorado por ventana muerta dinámica (${elapsed}ms).")
            return
        }

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-").filter { it.isNotBlank() }
        val acierto = palabras.any { palabra ->
            similar(palabra, currentAnswer) || palabra.contains(currentAnswer)
        }

        Log.d("ShapeMagic", "✅ Comparando '$recognized' con '$currentAnswer' → $acierto")

        if (acierto) {
            Log.i("ShapeMagic", "[STATE] Respuesta correcta detectada → Deteniendo escucha.")
            voskEngine?.stopListening()
            listeningJob?.cancel()
            isListening = false
            progress = 0f
            success = true
            correctCount++
            message = "¡Muy bien! 🎉 Es un ${currentAnswer.uppercase()}."

            scope.launch {
                speakAndWait("¡Excelente trabajo! Es un ${currentAnswer}.", 600L)

                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Formas y colores",
                    level = 1,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 3 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                if (correctCount < requiredCorrectAnswers) {
                    delay(1200)
                    attempts = 0
                    if (index < items.size - 1) index++ else index = 0
                    message = "¿Qué figura es esta?"
                    delay(700)
                    feedback.speak("Muy bien. Ahora dime qué figura es esta.")
                    delay(2200)

                    voskEngine?.startListening()
                    delay(300)
                    listenStartTime = System.currentTimeMillis()
                    isListening = true

                    listeningJob = launch {
                        val totalTime = 20000L
                        val interval = 100L
                        var elapsed = 0L
                        Log.i("ShapeMagic", "[TIMER] (AutoNext) Iniciado tras acierto rápido.")
                        while (elapsed < totalTime && isListening) {
                            delay(interval)
                            elapsed += interval
                            progress = elapsed / totalTime.toFloat()
                        }
                    }
                } else {
                    showConfetti = true
                    speakAndWait("¡Felicidades $kidName! Has aprendido las figuras. Vamos al Nivel 2.", 1000L)
                    message = "🎉 ¡Nivel 1 completado!"
                    delay(5000)
                    navController.navigate("formasMagicasNivel2/$kidName") {
                        popUpTo("formasMagicas/{kidName}") { inclusive = true }
                    }
                }
            }
        } else {
            attempts++
            voskEngine?.stopListening()
            listeningJob?.cancel()
            isListening = false
            progress = 0f

            if (attempts < maxAttempts) {
                scope.launch {
                    speakAndWait("No es esa figura, cariño. Intenta otra vez.")
                    message = "Intenta otra vez, ¿qué figura es?"
                    delay(1000)
                    voskEngine?.startListening()
                    delay(300)
                    listenStartTime = System.currentTimeMillis()
                    isListening = true
                    listeningJob = launch {
                        val totalTime = 20000L
                        val interval = 100L
                        var elapsed = 0L
                        Log.i("ShapeMagic", "[TIMER] (Retry) Iniciado.")
                        while (elapsed < totalTime && isListening) {
                            delay(interval)
                            elapsed += interval
                            progress = elapsed / totalTime.toFloat()
                        }
                    }
                }
            } else {
                scope.launch {
                    speakAndWait("Era ${currentAnswer}. No te preocupes, sigamos con la siguiente figura.", 800L)
                    message = "La respuesta era ${currentAnswer.uppercase()}."
                    voskEngine?.stopListening()
                    listeningJob?.cancel()
                    isListening = false
                    progress = 0f

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Formas y colores",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 3
                    )

                    saveResult(result)

                    delay(1500)
                    success = false
                    attempts = 0
                    if (index < items.size - 1) index++ else index = 0
                    message = "¿Qué figura es esta?"

                    delay(700)
                    feedback.speak("Muy bien. Ahora dime qué figura es esta.")
                    delay(2200)
                    voskEngine?.startListening()
                    delay(300)
                    listenStartTime = System.currentTimeMillis()
                    isListening = true

                    listeningJob = launch {
                        val totalTime = 20000L
                        val interval = 100L
                        var elapsed = 0L
                        Log.i("ShapeMagic", "[TIMER] (AutoNextAfterFail) Iniciado.")
                        while (elapsed < totalTime && isListening) {
                            delay(interval)
                            elapsed += interval
                            progress = elapsed / totalTime.toFloat()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            voskEngine = VoskEngine(context, { handleVoiceResult(it) },
                VoskEngine.Mode.HYBRID, {
                    if (isListening && !isAssistantSpeaking) {
                        scope.launch { speakAndWait("No te escucho, intenta hablar más fuerte.") }
                        message = "🤫 No te escucho, intenta hablar más fuerte."
                    }
                }, 6000L
            )
            voskEngine?.initModel {
                scope.launch { speakAndWait("Muy bien $kidName, dime qué figura es esta.") }
            }
        } catch (e: Exception) {
            scope.launch { speakAndWait("Ocurrió un error al iniciar el motor de voz.") }
            message = "❌ Error al cargar el motor de voz."
        }
    }

    // 🎨 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3F2FD))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔷 Formas Mágicas - Nivel 1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = currentImage), contentDescription = currentAnswer, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 3", fontSize = 16.sp, color = Color(0xFF1E88E5))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (!isListening && correctCount < 3) {
                            listeningJob?.cancel()
                            progress = 0f
                            isListening = false
                            if (voskEngine == null) {
                                feedback.speak("Preparando el micrófono.")
                                return@Button
                            }
                            isListening = true
                            feedback.speak("¿Qué figura es esta?")
                            scope.launch {
                                delay(400)
                                voskEngine?.startListening()
                                delay(300)
                                listenStartTime = System.currentTimeMillis()
                                listeningJob = launch {
                                    val totalTime = 20000L
                                    val interval = 100L
                                    var elapsed = 0L
                                    while (elapsed < totalTime && isListening) {
                                        delay(interval)
                                        elapsed += interval
                                        progress = elapsed / totalTime.toFloat()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White) }

                Button(
                    onClick = {
                        if (isListening) {
                            voskEngine?.stopListening()
                            listeningJob?.cancel()
                            progress = 0f
                            isListening = false
                            feedback.speak("Muy bien, detuve la escucha.")
                        } else feedback.speak("Aún no estoy escuchando.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))
                ) { Text("🛑 Detener", color = Color.White) }

            }

            if (isListening) {
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF64B5F6),
                    trackColor = Color(0xFFBBDEFB)
                )
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s", fontSize = 16.sp, color = Color(0xFF1565C0), modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        angle = 270,
                        speed = 10f,
                        maxSpeed = 30f,
                        spread = 360,
                        colors = listOf(0xFF42A5F5.toInt(), 0xFFFFC107.toInt(), 0xFF66BB6A.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80)
                    )
                )
            )
        }
    }
}
