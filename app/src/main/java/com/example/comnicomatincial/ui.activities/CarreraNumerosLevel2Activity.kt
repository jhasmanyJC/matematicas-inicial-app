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
import kotlin.math.min
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

private const val TAG = "CARRERA_NUMEROS_N2"

// ------------------------------------------------------
// 🏁 INTRODUCCIÓN — NIVEL 2
// ------------------------------------------------------
@Composable
fun CarreraNumerosIntroDialogN2(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }

    LaunchedEffect(Unit) {
        delay(1500)
        feedback.speak(
            "¡Hola $kidName! Bienvenido al nivel dos del juego Carrera de Números. " +
                    "Ahora contaremos del uno al diez. Cada número correcto te hará avanzar hasta llegar a la meta. ¡Vamos a correr con los números grandes!"
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
            Text("🏁 Carrera de Números - Nivel 2",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(16.dp))
            Text(
                "Cuenta del uno al diez en voz alta para avanzar por la pista. ¡Llega a la meta y gana!",
                fontSize = 18.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Muy bien! Empecemos la carrera del nivel dos.")
                    Log.i(TAG, "🟢 Botón de inicio presionado.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) { Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp) }
        }
    }
}

// ------------------------------------------------------
// 🎮 PANTALLA PRINCIPAL DEL NIVEL 2
// ------------------------------------------------------
@Composable
fun CarreraNumerosLevel2ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        CarreraNumerosIntroDialogN2(kidName, { showIntro = false }, context)
    }

    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        CarreraNumerosGameN2(kidName, navController, context)
    }
}

// ------------------------------------------------------
// 🎲 LÓGICA DEL JUEGO — NIVEL 2
// ------------------------------------------------------
@Composable
fun CarreraNumerosGameN2(
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
        CarreraStep(R.drawable.numeros_1_2_3_4_5_6, R.drawable.carrera_pos_6, 6),
        CarreraStep(R.drawable.numeros_1_2_3_4_5_6_7, R.drawable.carrera_pos_7, 7),
        CarreraStep(R.drawable.numeros_1_2_3_4_5_6_7_8, R.drawable.carrera_pos_8, 8),
        CarreraStep(R.drawable.numeros_1_2_3_4_5_6_7_8_9, R.drawable.carrera_pos_9, 9),
        CarreraStep(R.drawable.numeros_1_2_3_4_5_6_7_8_9_10, R.drawable.carrera_pos_10, 10),
        CarreraStep(R.drawable.carrera_meta, R.drawable.carrera_meta, 11)
    )

    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep by remember { derivedStateOf { carreraSteps[currentStepIndex] } }

    var showNumbers by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("¡Prepárate para comenzar la carrera del nivel dos!") }
    var attempts by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var saveCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    val totalNumbers = 10
    var showConfetti by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }

    // ---------------------------------------------------
    // 🔠 FUNCIONES AUXILIARES
    // ---------------------------------------------------
    fun saveResult(result: ActivityResult) {
        saveCount++
        db.insertResult(result)
        Log.i(
            TAG, """
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
    }

    fun startListeningCycle() {
        listeningJob?.cancel()
        voskEngine?.stopListening()
        progress = 0f
        isListening = true
        voskEngine?.startListening()
        Log.d(TAG, "🎧 Escucha iniciada (paso ${currentStep.expected})")

        listeningJob = scope.launch {
            val totalTime = 20000L
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
                        1 to "uno", 2 to "dos", 3 to "tres", 4 to "cuatro", 5 to "cinco", 6 to "seis", 7 to "siete", 8 to "ocho", 9 to "nueve", 10 to "diez"
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
    // 🧠 PROCESAR RESPUESTA (Nivel 2 completo hasta el 10)
    // ---------------------------------------------------
    fun handleVoiceResult(raw: String) {
        if (currentStep.expected == 0) return
        if (!isListening || raw.contains("\"partial\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        val recognized = match?.groups?.get(1)?.value?.lowercase() ?: raw

        val numberWords = mapOf(
            1 to listOf("uno", "1"), 2 to listOf("dos", "2"),
            3 to listOf("tres", "3"), 4 to listOf("cuatro", "4"),
            5 to listOf("cinco", "5"), 6 to listOf("seis", "6"),
            7 to listOf("siete", "7"), 8 to listOf("ocho", "8"),
            9 to listOf("nueve", "9"), 10 to listOf("diez", "10")
        )

        val recognizedClean = recognized.lowercase()
            .replace("[^a-záéíóúñ0-9]".toRegex(), "").trim()

        val allNumberWords = numberWords.flatMap { (num, words) ->
            words.map { it to num }
        }.toMap()

        val recognizedNumbers = allNumberWords
            .filter { (word, _) -> recognizedClean.contains(word) }
            .values.sorted()

        val expectedSeq = (1..currentStep.expected).toList()
        val missingNumbers = expectedSeq.filterNot { it in recognizedNumbers }
        val inOrder = recognizedNumbers == expectedSeq

        // 🟡 Faltan números
        if (missingNumbers.isNotEmpty()) {
            stopListeningCycle()
            attempts++
            val missingText = missingNumbers.joinToString(", ") {
                numberWords[it]?.first() ?: it.toString()
            }
            if (attempts >= maxAttempts) {
                scope.launch {
                    speakAndWait("Se acabaron los intentos. Vamos a intentarlo desde el principio.")
                    attempts = 0; correctCount = 0; currentStepIndex = 1
                    delay(1200)
                    speakAndWait("Contemos del uno al diez. Di el número uno para avanzar.")
                    startListeningCycle()
                }
            } else {
                scope.launch {
                    speakAndWait("Te saltaste el número $missingText. Intentemos contar de nuevo del uno al ${currentStep.expected}.")
                    delay(1200)
                    startListeningCycle()
                }
            }
            return
        }

        // 🟠 Orden incorrecto
        if (!inOrder) {
            stopListeningCycle()
            attempts++
            if (attempts >= maxAttempts) {
                scope.launch {
                    speakAndWait("Se acabaron los intentos. Empecemos desde el principio.")
                    attempts = 0; correctCount = 0; currentStepIndex = 1
                    delay(1200)
                    speakAndWait("Contemos del uno al diez. Di el número uno para avanzar.")
                    startListeningCycle()
                }
            } else {
                scope.launch {
                    speakAndWait("Casi, intentemos contar de nuevo del uno al ${currentStep.expected}.")
                    delay(1200)
                    startListeningCycle()
                }
            }
            return
        }

        // ✅ Correcto
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
                    level = 2,
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
                    speakAndWait("Ahora contemos del uno al ${siguiente}.")
                    delay(1200)
                    startListeningCycle()
                } else {
                    currentStepIndex = carreraSteps.lastIndex
                    showNumbers = false
                    showConfetti = true
                    speakAndWait("¡Excelente $kidName! Llegaste a la meta. ¡Ganaste la carrera del nivel dos!")
                    delay(3000)
                    navController.popBackStack()
                }
            }
        } else {
            // ❌ Falló 3 veces
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
                scope.launch {
                    stopListeningCycle()
                    val esperado = numberWords[currentStep.expected]?.first() ?: currentStep.expected.toString()
                    speakAndWait("La secuencia correcta era del uno al $esperado. Intentemos otra vez.")
                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Carrera de Números",
                        level = 2,
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
    // 🚀 INICIO AUTOMÁTICO DEL NIVEL 2
    // ---------------------------------------------------
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(context, { handleVoiceResult(it) }, VoskEngine.Mode.HYBRID)
        showNumbers = false
        message = "Estás en la casilla de inicio."
        delay(2500)
        speakAndWait("¡Muy bien $kidName! Este es el punto de inicio. Vamos a comenzar a contar del uno al diez.")
        delay(2000)
        currentStepIndex = 1
        showNumbers = true
        speakAndWait("Contemos del uno al diez. Di el número uno para avanzar.")
        startListeningCycle()
    }

    // ---------------------------------------------------
    // 🎨 INTERFAZ VISUAL
    // ---------------------------------------------------
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFE3F2FD)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏁 Carrera de Números - Nivel 2",
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

            if (isListening) {
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF42A5F5),
                    trackColor = Color(0xFFE3F2FD)
                )
                Text(
                    "⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp,
                    color = Color(0xFF0D47A1)
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
                        spread = 360,
                        colors = listOf(
                            0xFF42A5F5.toInt(),
                            0xFF81D4FA.toInt(),
                            0xFFFFF59D.toInt()
                        ),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(100)
                    )
                )
            )
        }
    }
}

