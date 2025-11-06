package com.example.comnicomatincial.ui.activities

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.comnicomatincial.data.ActivityResult
import com.example.comnicomatincial.data.ResultsDatabase
import com.example.comnicomatincial.voice.FeedbackAgent
import com.example.comnicomatincial.voice.VoskEngine
import kotlinx.coroutines.*
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random

// 🟩 INTRODUCCIÓN AUTOMÁTICA NIVEL 1
@Composable
fun SumaYSaltaIntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    suspend fun speakAndWait(text: String, delayAfter: Long = 800L) {
        Log.d("SumaYSalta", "🗣️ [AGENT] $text")
        val job = CompletableDeferred<Unit>()
        feedback.speak(text) { job.complete(Unit) }
        withTimeoutOrNull(9000L) { job.await() }
        delay(delayAfter)
    }

    LaunchedEffect(Unit) {
        Log.d("SumaYSalta", "🗣️ [AGENT] Iniciando introducción del nivel 1.")
        scope.launch {
            speakAndWait("¡Hola $kidName! Bienvenido al nivel uno de Suma y Salta.", 300L)
            speakAndWait("Vamos a sumar números pequeños del uno al cinco.", 500L)
            speakAndWait("Cada vez que aciertes, la rana avanzará una casilla en el tablero.", 500L)
            speakAndWait("¡Llega hasta la casilla cinco y gana!", 500L)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐸 Suma y Salta - Nivel 1", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            Spacer(Modifier.height(16.dp))
            Text(
                "Resuelve sumas hasta cinco con tu voz. Cada vez que aciertes, la rana avanzará una casilla. ¡Llega a la meta y gana!",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Excelente! Empecemos el nivel uno de Suma y Salta.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// 🎮 PANTALLA PRINCIPAL
@Composable
fun SumaYSaltaLevel1ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumaYSaltaIntroDialog(kidName, { showIntro = false }, context)
    }
    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumaYSaltaGameLevel1(kidName, navController, context)
    }
}

var lastSum: Pair<Int, Int>? = null
// 🎲 LÓGICA DEL JUEGO
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SumaYSaltaGameLevel1(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    var currentPosition by remember { mutableStateOf(0) }
    var currentSum by remember { mutableStateOf(Pair(1, 1)) }
    var correctAnswer by remember { mutableStateOf(2) }
    var message by remember { mutableStateOf("¿Cuánto es uno más uno?") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var showAnswer by remember { mutableStateOf(false) }

    val maxAttempts = 3
    val requiredCorrectAnswers = 5
    var correctCount by remember { mutableStateOf(0) }

    // 🎙️ Control Vosk
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }

    // 📊 Puntaje igual que nivel 2
    fun calculateScore(attempts: Int): Int = when (attempts) {
        0 -> 100
        1 -> 80
        2 -> 60
        else -> 0
    }

    // 💾 Guardado igual que nivel 2
    var saveCount by remember { mutableStateOf(0) }
    fun saveResult(result: ActivityResult) {
        saveCount++
        db.insertResult(result)
        Log.i("DB_TRACK", """
            💾 [#${saveCount}] GUARDADO LOCAL
            ├ kidName: ${result.kidName}
            ├ activityName: ${result.activityName}
            ├ level: ${result.level}
            ├ attempts: ${result.attempts}
            ├ score: ${result.score}
            ├ correctAnswers: ${result.correctAnswers}
            └ timestamp: ${result.timestamp}
        """.trimIndent())
    }

    // 🔢 Generador de sumas ≤5
    fun generateRandomSum(): Pair<Pair<Int, Int>, Int> {
        var a: Int
        var b: Int
        var result: Int
        do {
            a = Random.nextInt(1, 5)
            b = Random.nextInt(1, 5)
            result = a + b
        } while (result > 5 || lastSum == Pair(a, b))
        lastSum = Pair(a, b)
        return Pair(Pair(a, b), result)
    }


    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
    }

    suspend fun  speakAndWait(text: String, delayAfter: Long = 800L) {
        stopListeningCycle() // 🔇 Evita que el micrófono escuche al asistente
        Log.d("SumaYSalta", "🗣️ [AGENT] $text")
        val job = CompletableDeferred<Unit>()
        feedback.speak(text) { job.complete(Unit) }
        withTimeoutOrNull(10000L) { job.await() }
        delay(delayAfter)
    }

    fun normalizeText(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), "")
            .trim()

    fun numberWordToDigit(text: String): String = when (text.trim().lowercase()) {
        "uno" -> "1"
        "dos" -> "2"
        "tres" -> "3"
        "cuatro" -> "4"
        "cinco" -> "5"
        else -> text
    }

    fun similar(a: String, b: String): Boolean {
        val cleanA = normalizeText(a)
        val cleanB = normalizeText(b)
        if (cleanA.isBlank() || cleanB.isBlank()) return false
        val m = cleanA.length; val n = cleanB.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            val cost = if (cleanA[i - 1] == cleanB[j - 1]) 0 else 1
            dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
        }
        val distance = dp[m][n]
        val similarity = 1 - (distance / maxOf(m, n).toFloat())
        return similarity >= 0.7
    }

    // 🎧 Ciclo de escucha
    fun startListeningCycle() {
        listeningJob?.cancel()
        voskEngine?.stopListening()
        if (isListening) return // ⛔ evita duplicados
        isListening = true
        progress = 0f

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
                    message = "⏰ Se acabó el tiempo. Intenta otra vez."
                    scope.launch {
                        speakAndWait("Se acabó el tiempo, intenta otra vez.")
                        message = "¿Cuánto es ${currentSum.first} más ${currentSum.second}?"
                        speakAndWait(message)
                        startListeningCycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SumaYSalta", "❌ Error iniciando escucha", e)
        }
    }


    // 🎙️ Procesar voz
    fun handleVoiceResult(rawText: String) {
        if (!isListening) return
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return
        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extracted = match?.groups?.get(1)?.value ?: rawText
        var recognized = normalizeText(extracted)
        if (recognized == "text" || recognized.isBlank()) return
        recognized = numberWordToDigit(recognized)

        // 🔇 Ignorar frases del asistente
        val ignoredPhrases = listOf(
            "casi intenta otra vez",
            "la respuesta correcta era",
            "vamos a intentarlo otra vez",
            "cuanto es",
            "se acabo el tiempo",
            "no te escucho",
            "excelente",
            "felicidades",
            "hola",
            "bienvenido"
        )
        if (ignoredPhrases.any { recognized.contains(it) }) {
            Log.d("SumaYSalta", "🛑 Ignorado: frase del asistente detectada → '$recognized'")
            return
        }

        val acierto = recognized.contains(correctAnswer.toString())
        Log.d("SumaYSalta", "🎧 Reconocido: '$recognized' → esperado: '$correctAnswer'")

        if (acierto) {
            stopListeningCycle()
            success = true
            correctCount++
            val score = calculateScore(attempts)

            scope.launch {
                showAnswer = true
                speakAndWait("¡Excelente $kidName! La respuesta $correctAnswer es correcta. Avanzas una casilla.")
                delay(2000)
                showAnswer = false
                currentPosition++

                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Suma y salta",
                    level = 1,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 5
                )
                saveResult(result)

                if (correctCount >= requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Felicidades $kidName! La rana llegó a la meta. ¡Ganaste!")
                    delay(3500)
                    navController.navigate("sumaYsaltaNivel2/$kidName") {
                        popUpTo("sumaYsaltaNivel1/{kidName}") { inclusive = true }
                    }
                } else {
                    val newSum = generateRandomSum()
                    currentSum = newSum.first
                    correctAnswer = newSum.second
                    attempts = 0
                    success = false
                    message = "¿Cuánto es ${currentSum.first} más ${currentSum.second}?"
                    speakAndWait(message)
                    startListeningCycle()
                }
            }
        } else {
            attempts++
            if (attempts < maxAttempts) {
                scope.launch {
                    speakAndWait("Casi, intenta otra vez.")
                    message = "¿Cuánto es ${currentSum.first} más ${currentSum.second}?"
                    speakAndWait(message)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("La respuesta correcta era $correctAnswer. Vamos a intentarlo otra vez.")
                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Suma y salta",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 5
                    )
                    saveResult(result)
                    val newSum = generateRandomSum()
                    currentSum = newSum.first
                    correctAnswer = newSum.second
                    attempts = 0
                    message = "¿Cuánto es ${currentSum.first} más ${currentSum.second}?"
                    speakAndWait(message)
                    startListeningCycle()
                }
            }
        }
    }

    // 🧠 Inicialización
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(context, { handleVoiceResult(it) }, VoskEngine.Mode.HYBRID, {
            if (isListening) {
                Log.w("SumaYSalta", "🤫 Silencio detectado.")
                feedback.speak("No te escucho, intenta otra vez.")
            }
        }, 6000L)
        delay(2500)
        val startSum = generateRandomSum()
        currentSum = startSum.first
        correctAnswer = startSum.second
        message = "¿Cuánto es ${currentSum.first} más ${currentSum.second}?"
        speakAndWait("Muy bien $kidName, empecemos. $message")
        startListeningCycle()
    }

    // 🎨 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🐸 Suma y Salta - Nivel 1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            Spacer(Modifier.height(20.dp))

            val ranaImageId = when (currentPosition) {
                0 -> com.example.comnicomatincial.R.drawable.rana_pos_0
                1 -> com.example.comnicomatincial.R.drawable.rana_pos_1
                2 -> com.example.comnicomatincial.R.drawable.rana_pos_2
                3 -> com.example.comnicomatincial.R.drawable.rana_pos_3
                4 -> com.example.comnicomatincial.R.drawable.rana_pos_4
                else -> com.example.comnicomatincial.R.drawable.rana_pos_5
            }

            AnimatedContent(targetState = ranaImageId, transitionSpec = { fadeIn() togetherWith fadeOut() }) { img ->
                Image(painter = painterResource(id = img), contentDescription = null, modifier = Modifier.size(250.dp))
            }

            Spacer(Modifier.height(20.dp))
            if (showAnswer) {
                Text("✅ $correctAnswer", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            } else {
                Text(message, fontSize = 20.sp, color = Color(0xFF0D47A1), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Casilla actual: $currentPosition / 5", fontSize = 16.sp, color = Color(0xFF1976D2))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90A4AE))) {
                    Text("🛑 Detener", color = Color.White)
                }
            }

            // 🔹 Botón directo (test)
            if (!showConfetti) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = { navController.navigate("sumaYsaltaNivel2/$kidName") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
                    Text("➡ Ir al Nivel 2 (test)", color = Color.White, fontSize = 16.sp)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF64B5F6), trackColor = Color(0xFFE3F2FD))
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s", fontSize = 16.sp, color = Color(0xFF0D47A1))
            }
        }

        if (showConfetti) {
            KonfettiView(modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 12f, spread = 360, maxSpeed = 30f,
                        colors = listOf(0xFF64B5F6.toInt(), 0xFF81C784.toInt(), 0xFFFFF176.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 5, TimeUnit.SECONDS).perSecond(100))
                )
            )
        }
    }
}
