package com.example.comnicomatincial.ui.activities

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.comnicomatincial.R
import com.example.comnicomatincial.data.ActivityResult
import com.example.comnicomatincial.data.ResultsDatabase
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

private const val TAG = "CARRERA_NUMEROS" // 🔹 Un solo TAG para todo el Logcat

// ------------------------------------------------------
// 🏁 INTRODUCCIÓN
// ------------------------------------------------------
@Composable
fun CarreraNumerosIntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }

    LaunchedEffect(Unit) {
        delay(1500)
        feedback.speak(
            "¡Hola $kidName! Bienvenido al juego Carrera de Números. " +
                    "Vamos a contar del uno al cinco. Por cada número correcto, " +
                    "tu personaje avanzará una casilla hasta llegar a la meta. ¡Prepárate para correr con los números!"
        )
        Log.i(TAG, "🎬 Introducción reproducida para $kidName")
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
            Text("🏁 Carrera de Números - Nivel 1",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(16.dp))
            Text(
                "Cuenta del uno al cinco en voz alta para avanzar por la pista. ¡Llega a la meta y gana!",
                fontSize = 18.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Muy bien! Empecemos la carrera de números.")
                    Log.i(TAG, "🟢 Botón de inicio presionado.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) { Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp) }
        }
    }
}

// ------------------------------------------------------
// 🎮 PANTALLA PRINCIPAL DEL NIVEL 1
// ------------------------------------------------------
@Composable
fun CarreraNumerosLevel1ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        CarreraNumerosIntroDialog(kidName, { showIntro = false }, context)
    }

    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        CarreraNumerosGame(kidName, navController, context)
    }
}

// ------------------------------------------------------
// 🎲 LÓGICA DEL JUEGO
// ------------------------------------------------------
@Composable
fun CarreraNumerosGame(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    data class CarreraStep(val numerosImage: Int, val positionImage: Int, val expected: Int)

    val carreraSteps = listOf(
        CarreraStep(R.drawable.carrera_inicio, R.drawable.carrera_inicio, 0),
        CarreraStep(R.drawable.numeros_1, R.drawable.carrera_pos_1, 1),
        CarreraStep(R.drawable.numeros_1_2, R.drawable.carrera_pos_2, 2),
        CarreraStep(R.drawable.numeros_1_2_3, R.drawable.carrera_pos_3, 3),
        CarreraStep(R.drawable.numeros_1_2_3_4, R.drawable.carrera_pos_4, 4),
        CarreraStep(R.drawable.numeros_1_2_3_4_5, R.drawable.carrera_pos_5, 5),
        CarreraStep(R.drawable.carrera_meta, R.drawable.carrera_meta, 6)
    )

    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep by remember { derivedStateOf { carreraSteps[currentStepIndex] } }

    var showNumbers by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("¡Prepárate para comenzar la carrera!") }
    var attempts by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var saveCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    val totalNumbers = 5
    var showConfetti by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }


    // ---------------------------------------------------
    // 🔠 FUNCIONES AUXILIARES
    // ---------------------------------------------------
    fun similar(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        val cleanA = a.lowercase().replace("[^a-záéíóúñ]".toRegex(), "")
        val cleanB = b.lowercase().replace("[^a-záéíóúñ]".toRegex(), "")
        val m = cleanA.length; val n = cleanB.length
        if (m == 0 || n == 0) return false
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

    fun saveResult(result: ActivityResult) {
        saveCount++
        db.insertResult(result)
        Log.i(
            TAG,
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

    fun calculateScore(attempts: Int) = when (attempts) {
        0 -> 100; 1 -> 80; 2 -> 60; else -> 0
    }

    suspend fun speakAndWait(text: String, delayAfter: Long = 600L) {
        Log.d(TAG, "🗣️ $text")
        val job = CompletableDeferred<Unit>()
        feedback.speak(text) { job.complete(Unit) }
        withTimeoutOrNull(10000L) { job.await() }
        delay(delayAfter)
    }

    // ---------------------------------------------------
// 🎧 ESCUCHA DEL VOSK — control del ciclo de escucha
// ---------------------------------------------------
    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.d(TAG, "🛑 Escucha detenida.")
    }

    // ---------------------------------------------------
// 🎧 ESCUCHA DEL VOSK — inicio con timeout y reinicio automático real
// ---------------------------------------------------
    fun startListeningCycle() {
        listeningJob?.cancel()
        voskEngine?.stopListening()
        progress = 0f
        isListening = true
        voskEngine?.startListening()
        Log.d(TAG, "🎧 Escucha iniciada (paso ${currentStep.expected})")

        listeningJob = scope.launch {
            val totalTime = 20000L // 20 segundos por intento
            val step = 100L
            var elapsed = 0L

            while (elapsed < totalTime && isListening) {
                delay(step)
                elapsed += step
                progress = elapsed / totalTime.toFloat()
            }

            // 🕒 Si se acaba el tiempo sin respuesta:
            if (isListening) {
                stopListeningCycle()
                attempts += 1

                if (attempts >= maxAttempts) {
                    // 🔁 Se acabaron los intentos por tiempo
                    scope.launch {
                        speakAndWait("Pablo, se acabaron los intentos. Vamos a intentarlo desde el principio del número 1 y volvemos a empezar.")
                        attempts = 0
                        correctCount = 0
                        currentStepIndex = 1
                        delay(1200)
                        speakAndWait("Contemos del uno al cinco. Di el número uno para avanzar.")
                        startListeningCycle()
                    }
                } else {
                    val numberWords = mapOf(
                        1 to "uno", 2 to "dos", 3 to "tres", 4 to "cuatro", 5 to "cinco"
                    )
                    val esperado = numberWords[currentStep.expected] ?: currentStep.expected.toString()
                    scope.launch {
                        Log.d(TAG, "🕒 Tiempo agotado, mostrando feedback y reiniciando escucha...")
                        speakAndWait("Se acabó el tiempo, $kidName. Intenta contar del uno al $esperado.")
                        delay(1000)
                        Log.d(TAG, "🔄 Reiniciando escucha automática...")
                        startListeningCycle()
                    }
                }
            }
        }
    }


    // ---------------------------------------------------
// 🧠 PROCESAR RESPUESTA (versión final completísima)
// ---------------------------------------------------
    fun handleVoiceResult(raw: String) {
        if (currentStep.expected == 0) return
        if (!isListening || raw.contains("\"partial\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        val recognized = match?.groups?.get(1)?.value?.lowercase() ?: raw

        // 🔢 Palabras y equivalencias de números
        val numberWords = mapOf(
            1 to listOf("uno", "1"),
            2 to listOf("dos", "2"),
            3 to listOf("tres", "3"),
            4 to listOf("cuatro", "4"),
            5 to listOf("cinco", "5")
        )

        val recognizedClean = recognized.lowercase()
            .replace("[^a-záéíóúñ0-9]".toRegex(), "")
            .trim()

        Log.d(TAG, "🎙️ Reconocido: [$recognizedClean] — Esperado hasta: ${currentStep.expected}")

        // 🧠 Analizar los números reconocidos
        val allNumberWords = mapOf(
            "uno" to 1, "1" to 1,
            "dos" to 2, "2" to 2,
            "tres" to 3, "3" to 3,
            "cuatro" to 4, "4" to 4,
            "cinco" to 5, "5" to 5
        )

        val recognizedNumbers = allNumberWords
            .filter { (word, _) -> recognizedClean.contains(word) }
            .values
            .sorted()

        Log.d(TAG, "🔢 Números detectados: $recognizedNumbers — Esperado: ${(1..currentStep.expected).toList()}")

        val expectedSeq = (1..currentStep.expected).toList()
        val missingNumbers = expectedSeq.filterNot { it in recognizedNumbers }
        val inOrder = recognizedNumbers == expectedSeq

        if (missingNumbers.isNotEmpty()) {
            stopListeningCycle()
            attempts += 1
            val missingText = missingNumbers.joinToString(", ") {
                numberWords[it]?.first() ?: it.toString()
            }

            if (attempts >= maxAttempts) {
                scope.launch {
                    speakAndWait("se acabaron los intentos. Vamos a intentarlo desde el principio del número 1 y volvemos a empezar.")
                    attempts = 0
                    correctCount = 0
                    currentStepIndex = 1
                    delay(1200)
                    speakAndWait("Contemos del uno al cinco. Di el número uno para avanzar.")
                    startListeningCycle()
                }
            } else {
                scope.launch {
                    speakAndWait("Pablo, te saltaste el número $missingText. Intentemos contar de nuevo del uno al ${numberWords[currentStep.expected]?.first() ?: currentStep.expected}.")
                    delay(1200)
                    startListeningCycle()
                }
            }
            return
        }

        // 🟠 Caso: orden incorrecto (ej. “uno cinco tres”)
        if (!inOrder) {
            stopListeningCycle()
            attempts += 1

            if (attempts >= maxAttempts) {
                scope.launch {
                    speakAndWait(" se acabaron los intentos. Vamos a intentarlo desde el principio del número 1 y volvemos a empezar.")
                    attempts = 0
                    correctCount = 0
                    currentStepIndex = 1
                    delay(1200)
                    speakAndWait("Contemos del uno al cinco. Di el número uno para avanzar.")
                    startListeningCycle()
                }
            } else {
                scope.launch {
                    speakAndWait("Casi, intentemos contar de nuevo del uno al ${numberWords[currentStep.expected]?.first() ?: currentStep.expected}.")
                    delay(1200)
                    startListeningCycle()
                }
            }
            return
        }

        // ✅ Caso correcto (todos en orden y sin saltos)
        if (recognizedNumbers.contains(currentStep.expected)) {
            stopListeningCycle()
            scope.launch {
                correctCount++
                speakAndWait("¡Muy bien $kidName! Contaste muy bien. Avanzas una casilla.")
                showNumbers = false
                message = "Avanzaste a la casilla ${currentStep.expected}."

                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Carrera de Números",
                    level = 1,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = totalNumbers
                )
                saveResult(result)

                delay(1500)
                if (currentStepIndex < carreraSteps.size - 2) {
                    val siguiente = currentStep.expected + 1
                    currentStepIndex++
                    showNumbers = true
                    attempts = 0
                    speakAndWait("Ahora contemos del uno al ${numberWords[siguiente]?.first() ?: siguiente}.")
                    delay(1200)
                    startListeningCycle()
                } else {
                    currentStepIndex = carreraSteps.lastIndex
                    showNumbers = false
                    showConfetti = true
                    speakAndWait("¡Excelente $kidName! Llegaste a la meta. ¡Ganaste la carrera!")
                    delay(3000)
                    speakAndWait("¡Muy bien $kidName! Has terminado el nivel uno. Ahora pasemos al nivel dos.")
                    delay(2500)
                    navController.navigate("carreradenumerosNivel2/$kidName"){
                        popUpTo("carreradenumerosNivel1/{kidName}") { inclusive = true }
                    }

                }
            }
        } else {
            // ❌ Si no llegó al número esperado
            attempts++
            if (attempts < maxAttempts) {
                scope.launch {
                    stopListeningCycle()
                    val esperado = numberWords[currentStep.expected]?.first() ?: currentStep.expected.toString()
                    speakAndWait("Casi, intentemos contar de nuevo del uno al $esperado.")
                    delay(1200)
                    startListeningCycle()
                }
            } else {
                // 🔁 Si falló 3 veces, registrar el intento y reiniciar
                scope.launch {
                    stopListeningCycle()
                    val esperado = numberWords[currentStep.expected]?.first() ?: currentStep.expected.toString()
                    speakAndWait("La secuencia correcta era del uno al $esperado. Intentemos otra vez.")
                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Carrera de Números",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = totalNumbers
                    )
                    saveResult(result)
                    attempts = 0
                    delay(1200)
                    startListeningCycle()
                }
            }
        }
    }

    // ---------------------------------------------------
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(context, { handleVoiceResult(it) }, VoskEngine.Mode.HYBRID)
        // 🟢 Mostrar inicio
        showNumbers = false
        message = "Estás en la casilla de inicio."
        delay(2500)
        speakAndWait("¡Muy bien $kidName! Este es el punto de inicio. Cuando te diga contemos, comenzamos.")
        delay(2000)
        currentStepIndex = 1 // 🟢 saltamos al primer número (1)
        showNumbers = true
        speakAndWait("Contemos del uno al cinco. Di el número uno para avanzar.")
        startListeningCycle()
    }

    // ---------------------------------------------------
    // 🎨 INTERFAZ
    // ---------------------------------------------------
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFE3F2FD)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏁 Carrera de Números - Nivel 1",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
            Spacer(Modifier.height(16.dp))

            val imageRes = if (showNumbers)
                currentStep.numerosImage else currentStep.positionImage

            Image(painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(260.dp))
            Spacer(Modifier.height(20.dp))

            Text(message, fontSize = 20.sp, color = Color(0xFF0D47A1), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Progreso: $correctCount / $totalNumbers", fontSize = 16.sp, color = Color(0xFF1976D2))
            Spacer(Modifier.height(25.dp))

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
                Button(onClick = { navController.navigate("carreradenumerosNivel2/$kidName") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
                    Text("➡ Ir al Nivel 2 (test)", color = Color.White, fontSize = 16.sp)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF42A5F5), trackColor = Color(0xFFE3F2FD))
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp, color = Color(0xFF0D47A1))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 10f, spread = 360,
                        colors = listOf(0xFF42A5F5.toInt(), 0xFF81D4FA.toInt(), 0xFFFFF59D.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(100))
                )
            )
        }
    }
}
