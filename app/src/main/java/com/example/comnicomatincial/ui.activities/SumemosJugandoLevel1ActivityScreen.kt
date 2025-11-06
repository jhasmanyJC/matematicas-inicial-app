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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.comnicomatincial.R
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


// 🧮 INTRODUCCIÓN
@Composable
fun SumemosJugandoIntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }

    LaunchedEffect(Unit) {
        delay(2000)
        feedback.speak("¡Hola $kidName! Bienvenido al primer nivel de Sumemos Jugando. Mira las imágenes y dime con tu voz cuántos hay en total. ¡Vamos a divertirnos sumando!")
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9C4)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🍎 Sumemos Jugando - Nivel 1", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
            Spacer(Modifier.height(16.dp))
            Text("Observa las imágenes, escucha al agente y responde con tu voz. ¡Tú puedes hacerlo!",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Muy bien! Empecemos la actividad.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
            ) { Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp) }
        }
    }
}


// 🧩 PANTALLA PRINCIPAL
@Composable
fun SumemosJugandoLevel1ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumemosJugandoIntroDialog(kidName, { showIntro = false }, context)
    }
    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        SumemosJugandoGame(kidName, navController, context)
    }
}


// 🎲 LÓGICA DEL JUEGO
@Composable
fun SumemosJugandoGame(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { SumemosJugandoData.level1.shuffled() }

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val correctAnswer by remember { derivedStateOf { items[index].second } }
    val resultImage by remember { derivedStateOf { items[index].third } }

    val requiredCorrectAnswers = 5

    var message by remember { mutableStateOf("¿Cuántos hay en total?") }
    var attempts by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    var success by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

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
        Log.d("SumemosL1", "🗣️ [AGENT] Hablando: \"$text\"")

        val job = CompletableDeferred<Unit>()
        feedback.speak(text) {

            job.complete(Unit)
        }

        // Espera a que finalice la voz o 10s máximo, lo que ocurra primero//
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
                    Log.w("SumemosL1", "⏰ Tiempo agotado sin respuesta.")
                    message = "⏰ Se acabó el tiempo. Intenta otra vez."
                    speakAndWait("Se acabó el tiempo, cariño. Intentemos otra vez.", 1200L)
                    delay(2000)
                    startListeningCycle()
                }
            }
            Log.i("SumemosL1", "🎙️ Ciclo de escucha iniciado.")
        } catch (e: Exception) {
            Log.e("SumemosL1", "❌ Error iniciando escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.i("SumemosL1", "🛑 Escucha detenida.")
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("SumemosL1", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }
        Log.d("SumemosL1", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extracted = match?.groups?.get(1)?.value ?: rawText
        val recognized = Normalizer.normalize(extracted.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()
        if (recognized.isBlank()) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return
        Log.d("SumemosL1", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-").filter { it.isNotBlank() }
        val acierto = palabras.any { similar(it, correctAnswer) }

        Log.d("SumemosL1", "✅ Comparando con esperado='$correctAnswer' → match=$acierto")

        if (acierto) {
            stopListeningCycle()
            success = true; correctCount++
            message = "¡Muy bien! 🎉 La respuesta es $correctAnswer."
            scope.launch {
                speakAndWait("Excelente trabajo, $kidName. La respuesta es $correctAnswer.", 1200L)

                    val score = calculateScore(attempts)
                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Sumemos Jugando",
                        level = 1,
                        attempts = attempts + 1,
                        score = score,
                        correctAnswers = correctCount,
                        totalQuestions = 5 // 🟢 según tu cantidad real de figuras en nivel 1
                    )

                    saveResult(result)

                Log.i("SumemosL1", "[DB] Resultado guardado correctamente (score=100)")

                if (correctCount < requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Felicidades, $kidName! Has completado el nivel de sumas.", 1500L)
                    message = "🎉 ¡Nivel 1 completado!"
                    Log.i("SumemosL1", "🏆 Nivel completado.")
                    delay(3000)
                    navController.navigate("sumemosNivel2/$kidName") {
                        popUpTo("sumemosNivel1/{kidName}") { inclusive = true }
                    }
                } else {
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "¿Cuántos hay en total?"
                    speakAndWait("Muy bien, ahora dime cuántos hay en total.", 1000L)
                    startListeningCycle()
                }
            }
        } else {
            attempts++
            Log.d("SumemosL1", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("No del todo, cariño. Intenta otra vez.", 1000L)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("La respuesta correcta era $correctAnswer. Vamos con otro.", 2000L)
                    message = "Era $correctAnswer."

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Sumemos Jugando",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 5
                    )

                    saveResult(result)

                    Log.i("SumemosL1", "[DB] Resultado guardado correctamente (score=0)")
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "¿Cuántos hay en total?"
                    speakAndWait("Muy bien, ahora dime cuántos hay en total.", 1500L)
                    startListeningCycle()
                }
            }
        }
    }

    // 🟢 Inicialización del motor de voz
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(
            context = context,
            onSpeechRecognized = { handleVoiceResult(it) },
            mode = VoskEngine.Mode.HYBRID,
            onSilenceDetected = {
                if (isListening) {
                    Log.w("SumemosL1", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta hablar más fuerte.")
                    message = "🤫 No te escucho, intenta hablar más fuerte."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(2500)
        speakAndWait("Muy bien $kidName, observa la imagen y dime cuántos hay en total.", 1500L)
        startListeningCycle()
    }

    // 🎨 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9C4)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🍎 Sumemos Jugando - Nivel 1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.size(280.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = currentImage), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 5", fontSize = 16.sp, color = Color(0xFFD84315))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening && correctCount < 5) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))) {
                    Text("🛑 Detener", color = Color.White)
                }
            }

            // 🔹 Botón directo (test)
            if (!showConfetti) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = { navController.navigate("sumemosNivel2/$kidName") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
                    Text("➡ Ir al Nivel 2 (test)", color = Color.White, fontSize = 16.sp)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFFFFA726), trackColor = Color(0xFFFFF3E0))
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp, color = Color(0xFF6D4C41), modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (showConfetti) {
            KonfettiView(modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 10f, maxSpeed = 30f, spread = 360,
                        colors = listOf(0xFFFFA726.toInt(), 0xFFFFF176.toInt(), 0xFFFFCC80.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80))
                )
            )
        }
    }
}
