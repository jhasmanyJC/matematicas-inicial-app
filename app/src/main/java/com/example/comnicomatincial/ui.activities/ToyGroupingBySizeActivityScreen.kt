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


// 🔹 Dataset actualizado
object ToyGroupingSizeData {
    val level2 = listOf(
        Triple(R.drawable.juguetes_tam_1, "pequeño", listOf("auto")),
        Triple(R.drawable.juguetes_tam_2, "grande", listOf("oso", "auto")),
        Triple(R.drawable.juguetes_tam_3, "grande", listOf("elefante", "robot", "oso")),
        Triple(R.drawable.juguetes_tam_4, "pequeño", listOf("auto", "soldado", "robot")),
        Triple(R.drawable.juguetes_tam_5, "grande", listOf("elefante", "oso", "robot", "soldado")),
        Triple(R.drawable.juguetes_tam_6, "pequeño", listOf("elefante", "oso", "robot", "soldado", "auto")),
        Triple(R.drawable.juguetes_tam_7, "grande", listOf("oso", "auto"))
    )
}


// 🔹 Pantalla introductoria
@Composable
fun ToyGroupingSizeIntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    LaunchedEffect(Unit) {
        delay(800)
        feedback.speak("¡Hola $kidName! En este nivel aprenderás a clasificar juguetes por su tamaño. Escucha bien y di con tu voz cuáles son los juguetes grandes y cuáles son los pequeños.")
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
                .background(Color(0xFFC8E6C9), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧸 Clasifiquemos Juguetes por Tamaño", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(16.dp))
            Text("En este nivel aprenderás a reconocer cuáles juguetes son grandes y cuáles son pequeños. Escucha bien y responde con tu voz.",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp)
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text("👉 ¡Empezar!", color = Color.White, fontSize = 18.sp) }
        }
    }
}


// 🔹 Pantalla principal
@Composable
fun ToyGroupingBySizeActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }
    AnimatedVisibility(visible = showIntro, enter = fadeIn(), exit = fadeOut()) {
        ToyGroupingSizeIntroDialog(kidName, { showIntro = false }, context)
    }
    AnimatedVisibility(visible = !showIntro, enter = fadeIn(), exit = fadeOut()) {
        ToyGroupingBySizeGame(kidName, navController, context)
    }
}


// 🔹 Juego completo (voz + temporizador como nivel 1)
@Composable
fun ToyGroupingBySizeGame(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { ToyGroupingSizeData.level2.shuffled().take(5) }

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val targetSize by remember { derivedStateOf { items[index].second } }
    val correctToys by remember { derivedStateOf { items[index].third } }

    var message by remember { mutableStateOf("Dime los juguetes ${targetSize}s.") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    val maxAttempts = 3

    var isListening by remember { mutableStateOf(false) }

    val requiredCorrectAnswers = 4

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
        Log.d("ToySizeL2", "🗣️ [AGENT] Hablando: \"$text\"")
        feedback.speak(text)
        val estimated = text.length * 60L + delayAfter
        delay(estimated)
    }

    // 🔹 Función de similitud
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

    // 🔹 Iniciar ciclo de escucha
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
                    Log.w("ToySizeL2", "⏰ Tiempo agotado sin respuesta.")
                    message = "Se acabó el tiempo. Intenta otra vez: dime los juguetes ${targetSize}s."
                    speakAndWait("Se acabó el tiempo, cariño. Intentemos otra vez. Dime los juguetes ${targetSize}s.", 1500L)
                    delay(2000)
                    startListeningCycle()
                }
            }
            Log.i("ToySizeL2", "🎙️ Ciclo de escucha iniciado.")
        } catch (e: Exception) {
            Log.e("ToySizeL2", "❌ Error iniciando escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.i("ToySizeL2", "🛑 Escucha detenida.")
    }

    // 🔹 Procesar resultado de voz
    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("ToySizeL2", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }
        Log.d("ToySizeL2", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extracted = match?.groups?.get(1)?.value ?: rawText
        val recognized = Normalizer.normalize(extracted.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()
        if (recognized.isBlank()) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return
        Log.d("ToySizeL2", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-").filter { it.isNotBlank() }
        val sizeCorrecto = palabras.any { similar(it, targetSize) }
        val juguetesCorrectos = correctToys.any { toy -> palabras.any { similar(it, toy) } }
        val acierto = sizeCorrecto || juguetesCorrectos

        Log.d("ToySizeL2", "✅ Comparando con esperado='${correctToys.joinToString()}' ($targetSize) → match=$acierto")

        if (acierto) {
            stopListeningCycle()
            success = true; correctCount++
            message = "¡Muy bien! 🎉 Has reconocido los juguetes ${targetSize}s."
            scope.launch {
                speakAndWait("Excelente trabajo, reconociste los juguetes ${targetSize}s.", 1200L)

                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Clasifiquemos juguetes",
                    level = 2,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 4 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                Log.i("ToySizeL2", "[DB] Resultado guardado correctamente (score=100)")
                if (correctCount < requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Felicidades, $kidName! Has aprendido a clasificar los juguetes por tamaño.", 1500L)
                    message = "🎉 ¡Nivel 2 completado!"
                    Log.i("ToySizeL2", "🏆 Nivel completado.")
                    delay(6000)
                    navController.navigate("menu/$kidName") {
                        popUpTo("clasifiquemosNivel2/{kidName}") { inclusive = true }
                    }
                } else {
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "Dime los juguetes ${items[index].second}s."
                    speakAndWait("Muy bien, ahora dime los juguetes ${items[index].second}s.", 1000L)
                    startListeningCycle()
                }
            }
        } else {
            attempts++
            Log.d("ToySizeL2", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("No del todo, cariño. Intenta decir los juguetes ${targetSize}s.", 1000L)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    val correctNames = correctToys.joinToString(", ")
                    speakAndWait("Eran el $correctNames ${targetSize}s. No te preocupes, sigamos.", 2000L)
                    message = "Eran el $correctNames ${targetSize}s."

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Clasifiquemos juguetes",
                        level = 2,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 4
                    )

                    saveResult(result)

                    Log.i("ToySizeL2", "[DB] Resultado guardado correctamente (score=0)")
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "Dime los juguetes ${items[index].second}s."
                    speakAndWait("Muy bien, ahora dime los juguetes ${items[index].second}s.", 1500L)
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
                    Log.w("ToySizeL2", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta hablar más fuerte.")
                    message = "🤫 No te escucho, intenta hablar más fuerte."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(3000)
        speakAndWait("Muy bien, empecemos. Dime los juguetes ${targetSize}s.", 1500L)
        startListeningCycle()
    }

    // 🎨 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🧸 Clasifiquemos Juguetes - Nivel 2", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.size(280.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = currentImage), contentDescription = "juguetes ${targetSize}", modifier = Modifier.fillMaxSize())
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

            // 🔹 Botón directo (pruebas)
            if (!showConfetti) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = { navController.navigate("clasifiquemosNivel2/$kidName") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
                    Text("➡ Ir al Nivel 2 (test)", color = Color.White, fontSize = 16.sp)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(0.8f)
                    .height(14.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF66BB6A), trackColor = Color(0xFFC8E6C9))
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp, color = Color(0xFF33691E), modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 10f, maxSpeed = 30f, spread = 360,
                        colors = listOf(0xFF81C784.toInt(), 0xFFA5D6A7.toInt(), 0xFF388E3C.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80))
                )
            )
        }
    }
}
