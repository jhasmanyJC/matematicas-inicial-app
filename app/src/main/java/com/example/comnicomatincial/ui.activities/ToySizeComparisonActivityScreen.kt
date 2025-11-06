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
import com.example.comnicomatincial.data.SizeComparisonData
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
fun SizeComparisonIntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            delay(1000)
            feedback.speak("¡Hola $kidName! 😊 Hoy aprenderemos a comparar tamaños.")
        }
        scope.launch {
            delay(6500)
            feedback.speak("Mira las imágenes con atención... y dime con tu voz cuál animal es más grande... o cuál es más pequeño.")
        }
        scope.launch {
            delay(14000)
            feedback.speak("Prepárate... ¡va a ser divertido!")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🐾 Comparando Tamaños",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E88E5)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "En este nivel descubrirás cuáles animales son grandes y cuáles son pequeños. Usa tu voz para responder.",
                fontSize = 18.sp,
                color = Color.DarkGray,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Muy bien Empecemos la actividad.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SizeComparisonActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }

    if (showIntro) {
        SizeComparisonIntroDialog(
            kidName = kidName,
            onStartGame = { showIntro = false },
            context = context
        )
    } else {
        SizeComparisonGame(kidName, navController, context)
    }
}

@Composable
fun SizeComparisonGame(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { SizeComparisonData.level1.shuffled().take(4) }

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val expectedSize by remember { derivedStateOf { items[index].second } }
    val correctAnimals by remember { derivedStateOf { items[index].third } }

    var message by remember {
        mutableStateOf(
            if (expectedSize == "grande") "¿Cuál es el más grande?"
            else "¿Cuál es el más pequeño?"
        )
    }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    var showConfetti by remember { mutableStateOf(false) }

    val requiredCorrectAnswers = 3

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
        feedback.speak(text)
        val estimated = text.length * 60L + delayAfter
        delay(estimated)
    }

    fun similar(a: String, b: String): Boolean {
        if (a.length < 3 || b.length < 3) return false
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
                    feedback.speak("Se acabó el tiempo cariño, intentemos otra vez. ${message.lowercase()}")
                    message = "⏰ Se acabó el tiempo. Intenta otra vez."
                    Log.w("SizeComparison", "⏰ Tiempo agotado sin respuesta.")
                }
            }
            Log.i("SizeComparison", "🎙️ Ciclo de escucha iniciado.")
        } catch (e: Exception) {
            Log.e("SizeComparison", "❌ Error iniciando escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.i("SizeComparison", "🛑 Escucha detenida.")
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("SizeComparison", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }

        Log.d("SizeComparison", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extractedText = match?.groups?.get(1)?.value ?: rawText
        val normalized = Normalizer.normalize(extractedText.lowercase(), Normalizer.Form.NFD)
        var recognized = normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()

        if (recognized.isBlank()) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return

        Log.d("SizeComparison", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-")
            .filter { it.isNotBlank() }

        val animalCorrecto = correctAnimals.any { correct ->
            val partes = correct.split(" ")
            partes.all { palabraEsperada ->
                palabras.any { p -> similar(p, palabraEsperada) }
            }
        }

        Log.d("SizeComparison", "✅ Comparando con esperado='${correctAnimals.first()}' (${expectedSize}) → match=$animalCorrecto")

        if (animalCorrecto) {
            stopListeningCycle()
            success = true
            correctCount++
            message = if (expectedSize == "grande")
                "¡Excelente! El ${correctAnimals.first()} es el más grande. ¡Muy bien!"
            else
                "¡Perfecto! El ${correctAnimals.first()} es el más pequeño. ¡Bravo!"

            scope.launch {
                speakAndWait(message, 1200L)
                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Tamaños",
                    level = 1,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 3 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                Log.i("SizeComparison", "[DB] Resultado guardado correctamente (score=100)")

                if (correctCount < requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Lo lograste $kidName! Has completado el nivel de tamaños. Ahora pasaremos al siguiente desafío.", 1500L)
                    message = "¡Nivel 1 completado! 🚀"
                    Log.i("SizeComparison", "🏆 Nivel completado.")
                    delay(3000) // ⏩ Más rápido al siguiente nivel
                    navController.navigate("tamaniosNivel2/$kidName") {
                        popUpTo("tamaniosNivel1/{kidName}") { inclusive = true }
                    }
                } else {
                    success = false
                    attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message =
                        if (items[index].second == "grande") "¿Cuál es el más grande?" else "¿Cuál es el más pequeño?"
                    speakAndWait("Vamos a seguir... ${message.lowercase()}", 1500L)
                    startListeningCycle()
                }
            }
        } else {
            attempts++ // ✅ solo se incrementa si falla
            Log.d("SizeComparison", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("Intenta otra vez. Ahora dime nuevamente ${message.lowercase()}", 1000L)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("La respuesta correcta era el ${correctAnimals.first()} ${expectedSize}. Vamos con otro intento, ¿sí?", 2000L)
                    message = "La respuesta era el ${correctAnimals.first()} ${expectedSize}."

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Tamaños",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 3
                    )

                    saveResult(result)

                    Log.i("SizeComparison", "[DB] Resultado guardado correctamente (score=0)")
                    success = false
                    attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message =
                        if (items[index].second == "grande") "¿Cuál es el más grande?" else "¿Cuál es el más pequeño?"
                    speakAndWait(message, 1500L)
                    startListeningCycle()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(
            context = context,
            onSpeechRecognized = { handleVoiceResult(it) },
            mode = VoskEngine.Mode.HYBRID,
            onSilenceDetected = {
                if (isListening) {
                    Log.w("SizeComparison", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta hablar más fuerte.")
                    message = "🤫 No te escucho, intenta hablar más fuerte."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(2500) // ✅ Esperar a que el agente esté listo
        speakAndWait("¡Hola $kidName! Vamos a comenzar. Observa con atención... y dime con tu voz ${message.lowercase()}", 1500L)
        startListeningCycle()
    }

    // 🎨 UI (sin cambios + botón de prueba)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3F2FD))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🐾 Tamaños - Nivel 1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = currentImage), contentDescription = "Comparación", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 3", fontSize = 16.sp, color = Color(0xFF1565C0))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { if (!isListening && correctCount < 3) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) { Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White) }

                Button(
                    onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))
                ) { Text("🛑 Detener", color = Color.White) }
            }

            Spacer(Modifier.height(20.dp))

            // 🔹 Botón de prueba manual para ir al nivel 2
            Button(
                onClick = {
                    navController.navigate("tamaniosNivel2/$kidName") {
                        popUpTo("tamaniosNivel1/{kidName}") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Text("🧩 Ir al Nivel 2 (Prueba)", color = Color.White)
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF64B5F6),
                    trackColor = Color(0xFFE3F2FD)
                )
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s", fontSize = 16.sp, color = Color(0xFF0D47A1), modifier = Modifier.padding(top = 8.dp))
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
                        colors = listOf(0xFF42A5F5.toInt(), 0xFF81C784.toInt(), 0xFFFFEB3B.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80)
                    )
                )
            )
        }
    }
}
