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
import com.example.comnicomatincial.R
import com.example.comnicomatincial.data.ActivityResult
import com.example.comnicomatincial.data.ResultsDatabase
import com.example.comnicomatincial.data.ShapeColorLevelsData
import com.example.comnicomatincial.voice.FeedbackAgent
import com.example.comnicomatincial.voice.VoskEngine
import kotlinx.coroutines.*
import java.text.Normalizer
import kotlin.math.min
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun ShapeColorMagicLevel2Screen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { ShapeColorLevelsData.level2.shuffled() }

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val currentAnswer by remember { derivedStateOf { items[index].second } }

    var message by remember { mutableStateOf("Dime la figura y el color.") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    val maxAttempts = 3
    var correctCount by remember { mutableStateOf(0) }

    // 🟢 Nuevo: cantidad de aciertos necesarios para pasar el nivel
    val requiredCorrectAnswers = 5

    var isListening by remember { mutableStateOf(false) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var listenStartTime by remember { mutableStateOf(0L) }

    var showConfetti by remember { mutableStateOf(false) }

    // Contador de guardados para depuración
    var saveCount by remember { mutableStateOf(0) }

    // 🟢 Nuevo: cálculo de puntaje según intentos
    fun calculateScore(attempts: Int, maxAttempts: Int = 3): Int {
        return when (attempts) {
            0 -> 100
            1 -> 80
            2 -> 60
            else -> 0
        }
    }

    suspend fun speakAndWait(text: String, delayAfter: Long = 800L) {
        feedback.speak(text)
        val estimated = text.length * 60L + delayAfter
        delay(estimated)
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
        return 1 - (distance / maxLen) >= 0.7
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

    // 🔹 Maneja un ciclo completo de escucha + temporizador
    fun startListeningCycle() {
        listeningJob?.cancel()
        voskEngine?.stopListening()
        progress = 0f
        isListening = true
        listenStartTime = System.currentTimeMillis()

        try {
            voskEngine?.startListening()
            listeningJob = scope.launch {
                val totalTime = 20000L
                val interval = 100L
                var elapsed = 0L
                while (elapsed < totalTime && isListening) {
                    delay(interval)
                    elapsed += interval
                    progress = elapsed / totalTime.toFloat()
                }
                if (isListening) {
                    voskEngine?.stopListening()
                    isListening = false
                    feedback.speak("Se acabó el tiempo. Intentemos otra vez.")
                    message = "⏰ Se acabó el tiempo. Intenta otra vez."
                }
            }
        } catch (e: Exception) {
            Log.e("ShapeColorMagic", "❌ Error iniciando ciclo de escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("ShapeColorMagic", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }

        Log.d("ShapeColorMagic", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(rawText)
        val extractedText = match?.groups?.get(1)?.value ?: rawText
        val recognized = Normalizer.normalize(extractedText.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .trim()

        if (recognized.isBlank() || recognized.length < 3) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return

        val ignorePhrases = listOf(
            "dime la figura", "muy bien", "era", "sigamos", "intenta", "no te preocupes"
        )
        if (ignorePhrases.any { recognized.contains(it, true) }) {
            Log.i("ShapeColorMagic", "[GUARD] Ignorando texto del agente: '$recognized'")
            return
        }

        Log.d("ShapeColorMagic", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-")
            .filter { it.isNotBlank() }

        val partes = currentAnswer.split(" ")
        val formaCorrecta = partes.getOrNull(0) ?: ""
        val colorCorrecto = partes.getOrNull(1) ?: ""

        val formaDicha = palabras.any { p -> similar(p, formaCorrecta) }
        val colorDicho = palabras.any { p -> similar(p, colorCorrecto) }

        Log.d("ShapeColorMagic", "✅ Comparando: forma='$formaCorrecta' color='$colorCorrecto' → forma=$formaDicha / color=$colorDicho")

        if (formaDicha && colorDicho) {
            stopListeningCycle()
            success = true
            correctCount++
            message = "¡Excelente! 🎉 Dijiste ${currentAnswer.uppercase()}."

            scope.launch {
                speakAndWait("¡Muy bien! Has dicho ${currentAnswer}.", 1200L)

                // 🟢 Guarda puntaje real + aciertos actuales
                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Formas y colores",
                    level = 2,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 5 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                Log.d("ShapeColorMagic", "✅ Resultado guardado localmente (modo offline).")

                if (correctCount < requiredCorrectAnswers) {
                    success = false
                    attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "Dime la figura y el color."
                    speakAndWait("Muy bien. Ahora dime la figura y el color de este.", 1500L)
                    startListeningCycle()
                } else {
                    showConfetti = true
                    speakAndWait("¡Felicidades! Has completado el Nivel 2. Eres un campeón de las figuras y colores.", 2500L)
                    message = "🎉 ¡Nivel 2 completado!"
                    Log.i("ShapeColorMagic", "🏆 Nivel completado → Navegando al nivel 3.")
                    delay(4000)
                    navController.navigate("formasMagicasNivel3/$kidName") {
                        popUpTo("formasMagicasNivel2/{kidName}") { inclusive = true }
                    }
                }
            }

        } else {
            attempts++
            Log.d("ShapeColorMagic", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("No es correcto, cariño. Intenta otra vez.", 1000L)
                    message = "Intenta otra vez, dime la figura y el color."
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("Era ${currentAnswer}. No te preocupes, sigamos con la siguiente figura.", 1800L)
                    message = "Era ${currentAnswer.uppercase()}."

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Formas y colores",
                        level = 2,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 5
                    )

                    saveResult(result)

                    Log.d("ShapeColorMagic", "✅ Resultado guardado localmente (fallo).")

                    success = false
                    attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "Dime la figura y el color."
                    speakAndWait("Muy bien. Ahora dime la figura y el color de este.", 1500L)
                    startListeningCycle()
                }
            }
        }
    }

    // 🔹 Inicialización automática del motor y primera escucha
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(
            context = context,
            onSpeechRecognized = { handleVoiceResult(it) },
            mode = VoskEngine.Mode.HYBRID,
            onSilenceDetected = {
                if (isListening) {
                    Log.w("ShapeColorMagic", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta hablar más fuerte.")
                    message = "🤫 No te escucho, intenta hablar más fuerte."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(1500)
        feedback.speak("Muy bien $kidName, observa y dime la figura y el color.")
        delay(2000)
        startListeningCycle()
    }

    // 🔹 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF3E0))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🟩 Formas y Colores - Nivel 2", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = currentImage),
                    contentDescription = currentAnswer,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 5", fontSize = 16.sp, color = Color(0xFFD84315))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { if (!isListening && correctCount < 5) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))
                ) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }

                Button(
                    onClick = { stopListeningCycle() },
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
                    color = Color(0xFFFF9800),
                    trackColor = Color(0xFFFFE0B2)
                )
                Text(
                    text = "⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp,
                    color = Color(0xFFEF6C00),
                    modifier = Modifier.padding(top = 8.dp)
                )
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
                        colors = listOf(0xFFFFA726.toInt(), 0xFFFFC107.toInt(), 0xFF66BB6A.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80)
                    )
                )
            )
        }
    }
}
