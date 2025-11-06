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


// 🔹 Dataset de imágenes compuestas con juguetes por color
object ToyGroupingColorData {
    val level1 = listOf(
        Triple(R.drawable.juguetes_azul, "azul", listOf("muñeca", "dado", "auto")),
        Triple(R.drawable.juguetes_verde, "verde", listOf("auto", "dado", "tren")),
        Triple(R.drawable.juguetes_violeta, "violeta", listOf("oso", "avion")),
        Triple(R.drawable.juguetes_naranja, "naranja", listOf("pelota")),
        Triple(R.drawable.juguetes_rojo, "rojo", listOf("robot", "avion", "auto", "dado")),
        Triple(R.drawable.juguetes_naranja2, "naranja", listOf("avion", "robot", "dado"))
    )
}


// 🔹 Pantalla introductoria antes de comenzar el nivel
@Composable
fun ToyGroupingIntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            delay(500)
            feedback.speak("¡Hola $kidName! En este nivel aprenderás a clasificar juguetes por su color. Observa bien las imágenes y di con tu voz cuáles tienen el mismo color.")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
                .background(Color(0xFFFFF3E0), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🎨 Clasifiquemos Juguetes por su Color",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "En este nivel aprenderás a reconocer los juguetes según su color. Escucha bien y responde con tu voz.",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp
            )
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = {
                    feedback.speak("¡Muy bien! Empecemos la actividad.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))
            ) { Text("👉 ¡Empezar!", color = Color.White, fontSize = 18.sp) }
        }
    }
}


// 🔹 Pantalla principal del juego
@Composable
fun ToyGroupingByColorActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }
    if (showIntro) {
        ToyGroupingIntroDialog(kidName = kidName, onStartGame = { showIntro = false }, context = context)
    } else {
        ToyGroupingByColorGame(kidName, navController, context)
    }
}


// 🔹 Juego completo con motor de voz y logs del nivel 2 de tamaños
@Composable
fun ToyGroupingByColorGame(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { ToyGroupingColorData.level1.shuffled().take(4) }

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val targetColor by remember { derivedStateOf { items[index].second } }
    val correctToys by remember { derivedStateOf { items[index].third } }

    var message by remember { mutableStateOf("¿Qué juguetes son de color ${targetColor}?") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    var showConfetti by remember { mutableStateOf(false) }

    // 🟢 Nuevo: cantidad de aciertos necesarios para pasar el nivel
    val requiredCorrectAnswers = 3

    var isListening by remember { mutableStateOf(false) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var listenStartTime by remember { mutableStateOf(0L) }

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
        Log.d("ToyColorL1", "🗣️ [AGENT] Hablando: \"$text\"")
        feedback.speak(text)
        val estimated = text.length * 60L + delayAfter
        delay(estimated)
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
                    Log.w("ToyColorL1", "⏰ Tiempo agotado sin respuesta.")
                    message = "Se acabó el tiempo. Intenta otra vez: ¿qué juguetes son de color ${targetColor}?"
                    speakAndWait("Se acabó el tiempo, cariño. Intentemos otra vez. ¿Qué juguetes son de color ${targetColor}?", 1500L)
                    delay(2000)
                    startListeningCycle()
                }
            }
            Log.i("ToyColorL1", "🎙️ Ciclo de escucha iniciado.")
        } catch (e: Exception) {
            Log.e("ToyColorL1", "❌ Error iniciando escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.i("ToyColorL1", "🛑 Escucha detenida.")
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("ToyColorL1", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }
        Log.d("ToyColorL1", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return

        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extractedText = match?.groups?.get(1)?.value ?: rawText
        val normalized = Normalizer.normalize(extractedText.lowercase(), Normalizer.Form.NFD)
        val recognized = normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()
        if (recognized.isBlank()) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return
        Log.d("ToyColorL1", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-").filter { it.isNotBlank() }
        val colorCorrecto = palabras.any { similar(it, targetColor) }
        val juguetesCorrectos = correctToys.any { toy -> palabras.any { similar(it, toy) } }
        val acierto = colorCorrecto || juguetesCorrectos

        Log.d("ToyColorL1", "✅ Comparando con esperado='${correctToys.joinToString()}' ($targetColor) → match=$acierto")

        if (acierto) {
            stopListeningCycle()
            success = true; correctCount++
            message = "¡Muy bien! 🎉 Has reconocido los juguetes ${targetColor}s."
            scope.launch {
                speakAndWait("Excelente trabajo, reconociste los juguetes ${targetColor}s.", 1200L)

                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Clasifiquemos juguetes",
                    level = 1,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 3 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                Log.i("ToyColorL1", "[DB] Resultado guardado correctamente (score=100)")
                if (correctCount < requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Felicidades, $kidName! Has completado el nivel de colores. Ahora aprenderemos a clasificar los juguetes por su tamaño.", 1500L)
                    message = "🎉 ¡Nivel 1 completado! 🚀"
                    Log.i("ToyColorL1", "🏆 Nivel completado.")
                    delay(4000)
                    navController.navigate("clasifiquemosNivel2/$kidName") {
                        popUpTo("clasifiquemosNivel1/{kidName}") { inclusive = true }
                    }
                } else {
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "¿Qué juguetes son de color ${items[index].second}?"
                    speakAndWait("Muy bien, ahora dime los juguetes ${items[index].second}s.", 1000L)
                    startListeningCycle()
                }
            }
        } else {
            attempts++
            Log.d("ToyColorL1", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("No del todo, cariño. Intenta decir los juguetes ${targetColor}s.", 1000L)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    val correctNames = correctToys.joinToString(", ")
                    speakAndWait("Eran el $correctNames ${targetColor}s. Vamos con otro color.", 2000L)
                    message = "Eran el $correctNames ${targetColor}s."

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Clasifiquemos juguetes",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 3
                    )

                    saveResult(result)

                    Log.i("ToyColorL1", "[DB] Resultado guardado correctamente (score=0)")
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    message = "¿Qué juguetes son de color ${items[index].second}?"
                    speakAndWait("Muy bien, ahora dime los juguetes ${items[index].second}s.", 1500L)
                    startListeningCycle()
                }
            }
        }
    }

    // 🟢 CORREGIDO: el agente ahora habla siempre al iniciar el nivel (primera vez)
    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(
            context = context,
            onSpeechRecognized = { handleVoiceResult(it) },
            mode = VoskEngine.Mode.HYBRID,
            onSilenceDetected = {
                if (isListening) {
                    Log.w("ToyColorL1", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta hablar más fuerte.")
                    message = "🤫 No te escucho, intenta hablar más fuerte."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(3000)
        speakAndWait("Muy bien, empecemos. ¿Qué juguetes son de color ${targetColor}?", 1500L)
        startListeningCycle()
    }

    // 🎨 UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8E1)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎨 Clasifiquemos Juguetes - Nivel 1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(280.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = currentImage), contentDescription = "juguetes ${targetColor}", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 3", fontSize = 16.sp, color = Color(0xFFD84315))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening && correctCount < 3) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))) {
                    Text("🛑 Detener", color = Color.White)
                }
            }

            // 🧩 NUEVO: botón directo al nivel 2 (solo pruebas)
            if (!showConfetti) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        navController.navigate("clasifiquemosNivel2/$kidName")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Text("➡ Ir al Nivel 2 (test)", color = Color.White, fontSize = 16.sp)
                }
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFFFFA000),
                    trackColor = Color(0xFFFFECB3)
                )
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp, color = Color(0xFF6D4C41), modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 10f, maxSpeed = 30f, spread = 360,
                        colors = listOf(0xFFFFA726.toInt(), 0xFFFFC107.toInt(), 0xFF66BB6A.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80)
                    )
                )
            )
        }
    }
}
