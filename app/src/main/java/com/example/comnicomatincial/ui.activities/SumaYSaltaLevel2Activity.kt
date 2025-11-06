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
import kotlin.random.Random

// 🟩 INTRODUCCIÓN AUTOMÁTICA NIVEL 2
@Composable
fun SumaYSaltaLevel2IntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    suspend fun speakAndWait(text: String, delayAfter: Long = 500L) {
        Log.d("SumaYSalta2", "🗣️ [AGENT] $text")
        val job = CompletableDeferred<Unit>()
        feedback.speak(text) { job.complete(Unit) }
        withTimeoutOrNull(9000L) { job.await() }
        delay(delayAfter)
    }

    LaunchedEffect(Unit) {
        Log.d("SumaYSalta2", "🗣️ [AGENT] Iniciando introducción del nivel 2.")
        scope.launch {
            speakAndWait("¡Hola $kidName! Bienvenido al nivel dos de Suma y Salta.")
            speakAndWait("Ahora vamos a sumar números más grandes, hasta el diez.")
            speakAndWait("Cada vez que aciertes, la rana avanzará dos casillas en el tablero.")
            speakAndWait("Debes lograr cinco aciertos para ganar el juego. ¡Buena suerte!")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE1F5FE)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐸 Suma y Salta - Nivel 2", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
            Spacer(Modifier.height(16.dp))
            Text(
                "Resuelve sumas hasta diez con tu voz. Tienes tres intentos por suma y necesitas cinco aciertos para ganar.",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Excelente! Empecemos el nivel dos de Suma y Salta.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
            ) {
                Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// 🎮 PANTALLA PRINCIPAL
@Composable
fun SumaYSaltaLevel2ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumaYSaltaLevel2IntroDialog(kidName, { showIntro = false }, context)
    }
    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumaYSaltaGameLevel2(kidName, navController, context)
    }
}

// 🎲 LÓGICA DEL JUEGO NIVEL 2
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SumaYSaltaGameLevel2(
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
    var showAnswer by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    val maxAttempts = 3
    val requiredCorrectAnswers = 5
    var correctCount by remember { mutableStateOf(0) }

    // 🎙️ Control de voz
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }



    // 🔁 CICLO DE ESCUCHA — ahora seguro
    var startListeningCycleJob by remember { mutableStateOf<Job?>(null) }

    // 📊 Puntaje
    fun calculateScore(attempts: Int): Int = when (attempts) {
        0 -> 100; 1 -> 80; 2 -> 60; else -> 0
    }

    // 💾 Guardado
    var saveCount by remember { mutableStateOf(0) }
    fun saveResult(result: ActivityResult) {
        saveCount++
        db.insertResult(result)
        Log.i("DB_TRACK_L2", """
            💾 [#${saveCount}] GUARDADO NIVEL 2
            ├ kidName: ${result.kidName}
            ├ activityName: ${result.activityName}
            ├ level: ${result.level}
            ├ attempts: ${result.attempts}
            ├ score: ${result.score}
            ├ correctAnswers: ${result.correctAnswers}
            └ timestamp: ${result.timestamp}
        """.trimIndent())
    }

    // 🔢 Generador aleatorio hasta 10
    fun generateRandomSum(): Pair<Pair<Int, Int>, Int> {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        val result = a + b
        return if (result <= 10) Pair(Pair(a, b), result) else generateRandomSum()
    }

    // 🔇 Detiene el micrófono
    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        startListeningCycleJob?.cancel()
        isListening = false
        progress = 0f
    }

    // 🗣️ Habla y espera
    suspend fun speakAndWait(text: String, delayAfter: Long = 700L) {
        stopListeningCycle()
        Log.d("SumaYSalta2", "🗣️ [AGENT] $text")
        val job = CompletableDeferred<Unit>()
        feedback.speak(text) { job.complete(Unit) }
        withTimeoutOrNull(12000L) { job.await() }
        delay(delayAfter)
    }

    // ⚙️ Supervisión de coroutines para evitar cierre total del scope
    val supervisor = remember { SupervisorJob() }
    val supervisedScope = remember { CoroutineScope(Dispatchers.Main + supervisor) }

// 🔁 Declaración para funciones mutuas
    lateinit var startListeningCycle: () -> Unit

    // 🔊 Reanuda luego de hablar
    fun resumeListeningAfterSpeech(messageToAsk: String? = null) {
        supervisedScope.launch {
            try {
                delay(500)
                messageToAsk?.let {
                    message = it
                    speakAndWait(it)
                }
                if (!isListening) {
                    startListeningCycle()
                }
            } catch (e: CancellationException) {
                Log.w("SumaYSalta2", "🟡 Coroutine cancelada al reanudar: ${e.message}")
            } catch (e: Exception) {
                Log.e("SumaYSalta2", "⚠️ Error en resumeListeningAfterSpeech: ${e.message}", e)
                isListening = false
            }
        }
    }

// 🎧 Escucha
    startListeningCycle = {
        try {
            listeningJob?.cancel()
            voskEngine?.stopListening()
            isListening = true
            progress = 0f
            Log.d("SumaYSalta2", "🎧 [LISTEN] Iniciando escucha...")

            voskEngine?.startListening()
            listeningJob = supervisedScope.launch {
                try {
                    val total = 20000L
                    val step = 100L
                    var elapsed = 0L
                    while (elapsed < total && isListening) {
                        delay(step)
                        elapsed += step
                        progress = elapsed / total.toFloat()
                    }
                    if (isListening) {
                        Log.d("SumaYSalta2", "⏰ [TIMEOUT] Tiempo agotado sin respuesta.")
                        voskEngine?.stopListening()
                        isListening = false
                        speakAndWait("Se acabó el tiempo, intenta otra vez.")
                        resumeListeningAfterSpeech("¿Cuánto es ${currentSum.first} más ${currentSum.second}?")
                    }
                } catch (e: CancellationException) {
                    Log.d("SumaYSalta2", "🎧 Coroutine cancelada normalmente.")
                } catch (e: Exception) {
                    Log.e("SumaYSalta2", "⚠️ Error en listeningJob: ${e.message}", e)
                    isListening = false
                }
            }
        } catch (e: Exception) {
            Log.e("SumaYSalta2", "❌ Error en startListeningCycle: ${e.message}", e)
        }
    }



    // 🧹 Normalización
    fun normalizeText(t: String): String =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), "")
            .trim()

    fun numberWordToDigit(text: String): String = when (text.trim().lowercase()) {
        "uno" -> "1"; "dos" -> "2"; "tres" -> "3"; "cuatro" -> "4"; "cinco" -> "5"
        "seis" -> "6"; "siete" -> "7"; "ocho" -> "8"; "nueve" -> "9"; "diez" -> "10"; else -> text
    }

    // 🐸 Avanza la rana paso a paso (animación visual)
    fun avanzarCasillas(pasos: Int) {
        scope.launch {
            for (i in 1..pasos) {
                currentPosition += 1
                delay(9000L) // ⏳ controla la velocidad (700 ms por casilla)
            }
        }
    }

    // 🎙️ Procesar reconocimiento
    fun handleVoiceResult(raw: String) {
        if (!isListening) return
        if (raw.contains("\"partial\"") || !raw.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        var rec = match?.groups?.get(1)?.value ?: raw
        rec = numberWordToDigit(normalizeText(rec))
        if (rec == "text" || rec.isBlank()) return

        Log.d("SumaYSalta2", "🎧 Reconocido: '$rec' → esperado: '$correctAnswer'")

        val correct = rec.contains(correctAnswer.toString())
        if (correct) {
            stopListeningCycle()
            correctCount++
            val score = calculateScore(attempts)
            Log.d("SumaYSalta2", "✅ [CORRECTO] $rec es correcto. Aciertos: $correctCount")

            scope.launch {
                showAnswer = true
                speakAndWait("¡Excelente $kidName! ${currentSum.first} más ${currentSum.second} es $correctAnswer. ¡Avanzas dos casillas!")
                delay(1500)
                showAnswer = false

                avanzarCasillas(2)

                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Suma y Salta",
                    level = 2,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 5
                )
                saveResult(result)

                if (correctCount >= requiredCorrectAnswers) {
                    showConfetti = true
                    Log.d("SumaYSalta2", "🏁 [FIN] Completó el nivel 2.")
                    speakAndWait("¡Felicidades $kidName! Completaste cinco aciertos. ¡Ganaste el juego!")
                    delay(4000)
                    navController.popBackStack()
                } else {
                    val new = generateRandomSum()
                    currentSum = new.first
                    correctAnswer = new.second
                    attempts = 0
                    resumeListeningAfterSpeech("¿Cuánto es ${currentSum.first} más ${currentSum.second}?")
                }
            }
        } else {
            attempts++
            Log.d("SumaYSalta2", "❌ [INCORRECTO] '$rec' no coincide. Intentos: $attempts")

            if (attempts < maxAttempts) {
                scope.launch {
                    speakAndWait("Casi, intenta otra vez.")
                    resumeListeningAfterSpeech("¿Cuánto es ${currentSum.first} más ${currentSum.second}?")
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("La respuesta correcta era $correctAnswer. Vamos a intentarlo otra vez.")
                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Suma y Salta",
                        level = 2,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 5
                    )
                    saveResult(result)
                    val new = generateRandomSum()
                    currentSum = new.first
                    correctAnswer = new.second
                    attempts = 0
                    resumeListeningAfterSpeech("¿Cuánto es ${currentSum.first} más ${currentSum.second}?")
                }
            }
        }
    }

    // 🧠 Inicialización
    LaunchedEffect(Unit) {
        Log.d("SumaYSalta2", "🚀 Iniciando juego del nivel 2")
        voskEngine = VoskEngine(context, { handleVoiceResult(it) }, VoskEngine.Mode.HYBRID)
        delay(2000)
        val start = generateRandomSum()
        currentSum = start.first; correctAnswer = start.second
        message = "¿Cuánto es ${currentSum.first} más ${currentSum.second}?"
        speakAndWait("Muy bien $kidName, empecemos. $message")
        startListeningCycle()
    }

    // 🎨 UI
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFE1F5FE)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text("🐸 Suma y Salta - Nivel 2", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))
            Spacer(Modifier.height(20.dp))

            val ranaImageId = when (currentPosition) {
                0 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_0
                1 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_1
                2 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_2
                3 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_3
                4 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_4
                5 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_5
                6 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_6
                7 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_7
                8 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_8
                9 -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_9
                else -> com.example.comnicomatincial.R.drawable.rana_lvl2_pos_10
            }

            AnimatedContent(targetState = ranaImageId, transitionSpec = { fadeIn() togetherWith fadeOut() }) { img ->
                Image(painter = painterResource(id = img), contentDescription = null, modifier = Modifier.size(260.dp))
            }

            Spacer(Modifier.height(20.dp))
            if (showAnswer) Text("✅ $correctAnswer", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            else Text(message, fontSize = 22.sp, color = Color(0xFF01579B), textAlign = TextAlign.Center)

            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / $requiredCorrectAnswers", fontSize = 16.sp, color = Color(0xFF0277BD))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90A4AE))) {
                    Text("🛑 Detener", color = Color.White)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF4FC3F7), trackColor = Color(0xFFE1F5FE))
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s", fontSize = 16.sp, color = Color(0xFF01579B))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 12f, spread = 360,
                        colors = listOf(0xFF4FC3F7.toInt(), 0xFF81C784.toInt(), 0xFFFFF176.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(100))
                )
            )
        }
    }
}
