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
fun SizeComparisonLevel2IntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            delay(1000)
            feedback.speak("¡Hola $kidName! Bienvenido al segundo nivel de tamaños.")
        }
        scope.launch {
            delay(6500)
            feedback.speak("Esta vez observaremos tres objetos y descubrirás cuál es el más grande, mediano o pequeño.")
        }
        scope.launch {
            delay(13500)
            feedback.speak("Escucha con atención... y usa tu voz para responder.")
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
            Text("🐾 Comparando Tamaños - Nivel 2",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            Spacer(Modifier.height(16.dp))
            Text("En este nivel deberás decir cuál es el objeto más grande, mediano o pequeño. ¡Tú puedes!",
                fontSize = 18.sp, color = Color.DarkGray, lineHeight = 24.sp)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    feedback.speak("¡Muy bien! Empecemos la actividad.")
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) { Text("👉 ¡Comenzar!", color = Color.White, fontSize = 18.sp) }
        }
    }
}

@Composable
fun SizeComparisonLevel2ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }
    if (showIntro) {
        SizeComparisonLevel2IntroDialog(kidName = kidName, onStartGame = { showIntro = false }, context = context)
    } else {
        SizeComparisonLevel2Game(kidName, navController, context)
    }
}

@Composable
fun SizeComparisonLevel2Game(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val items = remember { SizeComparisonData.level2.shuffled().take(4) }
    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { items[index].first } }
    val objectMap by remember { derivedStateOf { items[index].second } }
    var currentQuestion by remember { mutableStateOf(listOf("grande", "mediano", "pequeño").random()) }

    var message by remember { mutableStateOf("¿Cuál es el más $currentQuestion?") }
    var attempts by remember { mutableStateOf(0) }
    var success by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    var showConfetti by remember { mutableStateOf(false) }

    val requiredCorrectAnswers = 5

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

    // ✅ Nueva función con tolerancia fonética y acentos
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

        // Capa fonética: elimina vocales y repeticiones (ej. "perro" -> "pr")
        fun simplifyPhonetic(s: String): String =
            s.replace("[aeiou]".toRegex(), "").replace("(.)\\1+".toRegex(), "$1")

        val phoneticA = simplifyPhonetic(cleanA)
        val phoneticB = simplifyPhonetic(cleanB)
        val phoneticMatch = phoneticA == phoneticB

        return similarity >= 0.7 || phoneticMatch
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
                    feedback.speak("Se acabó el tiempo, intenta otra vez.")
                    message = "⏰ Se acabó el tiempo. Intenta otra vez."
                    Log.w("SizeComparisonL2", "⏰ Tiempo agotado sin respuesta.")
                    delay(2500)
                    startListeningCycle()
                }
            }
            Log.i("SizeComparisonL2", "🎙️ Ciclo de escucha iniciado.")
        } catch (e: Exception) {
            Log.e("SizeComparisonL2", "❌ Error iniciando escucha", e)
            isListening = false
        }
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
        Log.i("SizeComparisonL2", "🛑 Escucha detenida.")
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening || success || attempts >= maxAttempts) {
            Log.i("SizeComparisonL2", "[GUARD] Ignorando entrada: no en modo escucha o límite alcanzado.")
            return
        }

        Log.d("SizeComparisonL2", "🎧 Texto recibido del motor: $rawText")
        if (rawText.contains("\"partial\"") || !rawText.contains("\"text\"")) return
        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val extractedText = match?.groups?.get(1)?.value ?: rawText
        val normalized = Normalizer.normalize(extractedText.lowercase(), Normalizer.Form.NFD)
        val recognized = normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()
        if (recognized.isBlank()) return
        val elapsed = System.currentTimeMillis() - listenStartTime
        if (elapsed < 800L) return
        Log.d("SizeComparisonL2", "🎙️ Texto reconocido limpio: '$recognized'")

        val palabras = recognized.split(" ", ",", ".", "?", "¿", "¡", "!", "-").filter { it.isNotBlank() }
        val correctWords = objectMap[currentQuestion] ?: emptyList()

        val fraseReconocida = palabras.joinToString(" ")
        val acierto = correctWords.any { palabraEsperada ->
            val incluyePalabra = palabras.any { similar(it, palabraEsperada) }
            val incluyeCategoria = palabras.any { similar(it, currentQuestion) }
            val contieneFrase = similar(fraseReconocida, palabraEsperada)
            incluyePalabra || contieneFrase || (incluyePalabra && incluyeCategoria)
        }

        Log.d("SizeComparisonL2", "✅ Comparando con esperado='${correctWords.firstOrNull()}' ($currentQuestion) → match=$acierto")

        if (acierto) {
            stopListeningCycle()
            success = true; correctCount++
            message = "¡Muy bien! Ese es el más $currentQuestion."
            scope.launch {
                speakAndWait(message, 1200L)

                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "Tamaños",
                    level = 2,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = 5 // 🟢 según tu cantidad real de figuras en nivel 1
                )

                saveResult(result)

                Log.i("SizeComparisonL2", "[DB] Resultado guardado correctamente (score=100)")
                if (correctCount < requiredCorrectAnswers) {
                    showConfetti = true
                    speakAndWait("¡Excelente trabajo $kidName! Has completado el nivel 2 de tamaños.", 1500L)
                    message = "🎉 ¡Nivel 2 completado! 🚀"
                    Log.i("SizeComparisonL2", "🏆 Nivel completado.")
                    delay(3000)
                    navController.navigate("tamaniosNivel3/$kidName") {
                        popUpTo("tamaniosNivel2/{kidName}") { inclusive = true }
                    }
                } else {
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    currentQuestion = listOf("grande", "mediano", "pequeño").random()
                    message = "¿Cuál es el más $currentQuestion?"
                    speakAndWait("Vamos a seguir... ${message.lowercase()}", 1500L)
                    startListeningCycle()
                }
            }
        } else {
            attempts++
            Log.d("SizeComparisonL2", "❌ Intento $attempts de $maxAttempts fallido.")
            if (attempts < maxAttempts) {
                stopListeningCycle()
                scope.launch {
                    speakAndWait("Intenta otra vez. Ahora dime nuevamente ${message.lowercase()}", 1000L)
                    startListeningCycle()
                }
            } else {
                stopListeningCycle()
                scope.launch {
                    val correctName = correctWords.firstOrNull() ?: "objeto correcto"
                    speakAndWait("La respuesta era el $correctName $currentQuestion. Vamos con otro intento.", 2000L)
                    message = "La respuesta era el $correctName $currentQuestion."

                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "Formas y colores",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = 3
                    )

                    saveResult(result)

                    Log.i("SizeComparisonL2", "[DB] Resultado guardado correctamente (score=0)")
                    success = false; attempts = 0
                    index = if (index < items.size - 1) index + 1 else 0
                    currentQuestion = listOf("grande", "mediano", "pequeño").random()
                    message = "¿Cuál es el más $currentQuestion?"
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
                    Log.w("SizeComparisonL2", "⚠️ Silencio detectado durante escucha.")
                    feedback.speak("No te escucho, intenta hablar más fuerte.")
                    message = "🤫 No te escucho, intenta hablar más fuerte."
                }
            },
            silenceTimeoutMs = 6000L
        )
        delay(2500)
        speakAndWait("Muy bien $kidName, observa con atención... y dime con tu voz cuál es el más $currentQuestion.", 1500L)
        startListeningCycle()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🐾 Tamaños - Nivel 2", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(280.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = currentImage), contentDescription = "Comparación", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / 5", fontSize = 16.sp, color = Color(0xFF1565C0))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening && correctCount < 5) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))) {
                    Text("🛑 Detener", color = Color.White)
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                navController.navigate("tamaniosNivel3/$kidName") {
                    popUpTo("tamaniosNivel2/{kidName}") { inclusive = true }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
                Text("🧩 Ir al Nivel 3 (Prueba)", color = Color.White)
            }

            if (isListening) {
                Spacer(Modifier.height(25.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(50.dp)),
                    color = Color(0xFF64B5F6),
                    trackColor = Color(0xFFE3F2FD)
                )
                Text("⏳ Tiempo restante: ${(20 - (progress * 20)).toInt()} s",
                    fontSize = 16.sp, color = Color(0xFF0D47A1), modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(angle = 270, speed = 10f, maxSpeed = 30f, spread = 360,
                        colors = listOf(0xFF42A5F5.toInt(), 0xFF81C784.toInt(), 0xFFFFEB3B.toInt()),
                        position = Position.Relative(0.5, 1.0),
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80)
                    )
                )
            )
        }
    }
}
