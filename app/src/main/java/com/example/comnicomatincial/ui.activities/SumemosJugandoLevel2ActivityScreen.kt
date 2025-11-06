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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.comnicomatincial.data.ActivityResult
import com.example.comnicomatincial.data.ResultsDatabase
import com.example.comnicomatincial.data.SumemosJugandoData
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

// 🧸 INTRODUCCIÓN AUTOMÁTICA NIVEL 2
@Composable
fun SumemosJugandoIntroDialog2(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Log.d("SumemosL2", "🗣️ [AGENT] Iniciando introducción automática del nivel 2.")
        scope.launch {
            feedback.speak("¡Hola $kidName! Bienvenido al nivel dos de Sumemos Jugando.")
            delay(5000)
            feedback.speak("Ahora sumaremos cantidades más grandes, hasta diez. Observa las imágenes y dime cuántos hay en total.")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🤖 Sumemos Jugando - Nivel 2", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(16.dp))
            Text(
                "Observa las dos imágenes, escucha al agente y responde con tu voz. ¡Suma hasta diez!",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Excelente! Empecemos el nivel dos.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// 🧮 PANTALLA PRINCIPAL NIVEL 2
@Composable
fun SumemosJugandoLevel2ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumemosJugandoIntroDialog2(kidName, { showIntro = false }, context)
    }
    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumemosJugandoGameLevel2(kidName, navController, context)
    }
}

// 🎲 LÓGICA DEL JUEGO NIVEL 2
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SumemosJugandoGameLevel2(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }
    val items = remember { SumemosJugandoData.level2.shuffled() }

    var index by remember { mutableStateOf(0) }
    var image1 by remember { mutableStateOf(items[index].firstImage) }
    var image2 by remember { mutableStateOf(items[index].secondImage) }
    var correctAnswer by remember { mutableStateOf(items[index].answerText) }
    var resultImage by remember { mutableStateOf(items[index].resultImage) }

    var message by remember { mutableStateOf("¿Cuántos hay en total?") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }

    val requiredCorrectAnswers = 4

    val maxAttempts = 3
    var isListening by remember { mutableStateOf(false) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var listenStartTime by remember { mutableStateOf(0L) }

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
        Log.d("SumemosL2", "🗣️ [AGENT] Hablando: \"$text\"")
        val job = CompletableDeferred<Unit>()
        feedback.speak(text) { job.complete(Unit) }
        withTimeoutOrNull(10000L) { job.await() }
        delay(delayAfter)
    }

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

    fun startListeningCycle() {
        listeningJob?.cancel()
        voskEngine?.stopListening()
        progress = 0f
        isListening = true
        listenStartTime = System.currentTimeMillis()
        try {
            voskEngine?.startListening()
            listeningJob = scope.launch {
                val totalTime = 20000L; val interval = 100L; var elapsed = 0L
                while (elapsed < totalTime && isListening) {
                    delay(interval); elapsed += interval
                    progress = elapsed / totalTime.toFloat()
                }
                if (isListening) {
                    voskEngine?.stopListening(); isListening = false
                    Log.w("SumemosL2", "⏰ Tiempo agotado sin respuesta.")
                    message = "⏰ Se acabó el tiempo. Intenta otra vez."
                    scope.launch {
                        speakAndWait("Se acabó el tiempo, cariño. Intentemos otra vez.", 1200L)
                        delay(2000)
                        message = "¿Cuántos hay en total?" // 🔹 actualiza el texto en pantalla
                        startListeningCycle()
                    }
                }

            }
            Log.i("SumemosL2", "🎙️ Ciclo de escucha iniciado.")
        } catch (e: Exception) {
            Log.e("SumemosL2", "❌ Error iniciando escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.i("SumemosL2", "🛑 Escucha detenida.")
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("SumemosL2", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }
        Log.d("SumemosL2", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extracted = match?.groups?.get(1)?.value ?: rawText
        val recognized = Normalizer.normalize(extracted.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()
        if (recognized.isBlank()) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return
        Log.d("SumemosL2", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-").filter { it.isNotBlank() }
        val acierto = palabras.any { similar(it, correctAnswer) }

        Log.d("SumemosL2", "✅ Comparando con esperado='$correctAnswer' → match=$acierto")

        if (acierto) {
            stopListeningCycle()
            success = true; correctCount++
            message = "¡Excelente! La respuesta es $correctAnswer."
            scope.launch {
                speakAndWait("Excelente trabajo, $kidName. La respuesta es $correctAnswer.", 1200L)

                // 🟢 Guarda puntaje real + aciertos actuales
                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Sumemos Jugando",
                    level = 2,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 4 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                Log.i("SumemosL2", "[DB] Resultado guardado correctamente (score=100)")

                if (correctCount < requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Maravilloso trabajo $kidName! Has completado el nivel dos de sumas hasta diez.", 1500L)
                    message = "🎉 ¡Nivel completado! 🚀"
                    Log.i("SumemosL2", "🏆 Nivel completado.")
                    delay(4000)
                    navController.popBackStack()
                } else {
                    success = false; attempts = 0
                    index = (index + 1) % items.size
                    image1 = items[index].firstImage
                    image2 = items[index].secondImage
                    correctAnswer = items[index].answerText
                    resultImage = items[index].resultImage
                    message = "¿Cuántos hay en total?"
                    speakAndWait("Muy bien, ahora dime cuántos hay en total.", 1000L)
                    startListeningCycle()
                }
            }
        } else {
            attempts++
            Log.d("SumemosL2", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("Casi, intenta otra vez.", 1000L)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("La respuesta correcta era $correctAnswer. Vamos a intentarlo otra vez.", 2000L)

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Sumemos Jugando",
                        level = 2,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 4
                    )

                    saveResult(result)

                    Log.i("SumemosL2", "[DB] Resultado guardado correctamente (score=0)")
                    success = false; attempts = 0
                    index = (index + 1) % items.size
                    image1 = items[index].firstImage
                    image2 = items[index].secondImage
                    correctAnswer = items[index].answerText
                    resultImage = items[index].resultImage
                    message = "¿Cuántos hay en total?"
                    speakAndWait("Muy bien, ahora dime cuántos hay en total.", 1500L)
                    startListeningCycle()
                }
            }
        }
    }

    // 🟢 Inicialización de Vosk
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(
            context = context,
            onSpeechRecognized = { handleVoiceResult(it) },
            mode = VoskEngine.Mode.HYBRID,
            onSilenceDetected = {
                if (isListening) {
                    Log.w("SumemosL2", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta otra vez.")
                    message = "🤫 No te escucho, intenta otra vez."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(2500)
        speakAndWait("Muy bien $kidName, observa las imágenes y dime cuántos hay en total.", 1500L)
        startListeningCycle()
    }

    // 🎨 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🤖 Sumemos Jugando - Nivel 2", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(20.dp))

            AnimatedContent(targetState = success, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "AnimatedImage") { successState ->
                if (!successState) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painter = painterResource(id = image1), contentDescription = null)
                        Spacer(Modifier.height(8.dp))
                        Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        Spacer(Modifier.height(8.dp))
                        Image(painter = painterResource(id = image2), contentDescription = null)
                    }
                } else {
                    Image(painter = painterResource(id = resultImage), contentDescription = null)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 4", fontSize = 16.sp, color = Color(0xFF1B5E20))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening && correctCount < 4) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))) {
                    Text("🛑 Detener", color = Color.White)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF81C784), trackColor = Color(0xFFE8F5E9))
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s", fontSize = 16.sp, color = Color(0xFF33691E))
            }
        }

        if (showConfetti) {
            KonfettiView(modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 10f, spread = 360,
                        colors = listOf(0xFF81C784.toInt(), 0xFFA5D6A7.toInt(), 0xFFC8E6C9.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80))
                )
            )
        }
    }
}
