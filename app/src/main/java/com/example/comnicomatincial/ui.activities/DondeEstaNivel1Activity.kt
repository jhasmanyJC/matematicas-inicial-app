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
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import kotlin.math.min

// ---------------------------------------------------------
// 🎬 INTRODUCCIÓN DEL JUEGO
// ---------------------------------------------------------
@Composable
fun DondeEstaNivel1IntroDialog(
    kidName: String,
    onStartGame: () -> Unit,
    context: Context = LocalContext.current
) {
    val feedback = remember { FeedbackAgent(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            delay(1000)
            feedback.speak("¡Hola $kidName! Bienvenido al juego ¿Dónde está?.")
        }
        scope.launch {
            delay(5500)
            feedback.speak("Observa las imágenes y responde con tu voz.")
        }
        scope.launch {
            delay(9000)
            feedback.speak("Por ejemplo, el gato puede estar dentro o fuera de la casa.")
        }
        scope.launch {
            delay(13500)
            feedback.speak("Usa tu voz para decir dentro, fuera, arriba o abajo.")
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
            Text("📍 ¿Dónde está? - Nivel 1",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            Spacer(Modifier.height(16.dp))
            Text("Mira las imágenes y responde con tu voz: dentro, fuera, arriba o abajo. ¡Vamos a jugar!",
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

// ---------------------------------------------------------
// 🎮 PANTALLA PRINCIPAL
// ---------------------------------------------------------
@Composable
fun DondeEstaNivel1ActivityScreen(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    var showIntro by remember { mutableStateOf(true) }
    if (showIntro) {
        DondeEstaNivel1IntroDialog(kidName = kidName, onStartGame = { showIntro = false }, context = context)
    } else {
        DondeEstaNivel1Game(kidName, navController, context)
    }
}

// ---------------------------------------------------------
// 🧠 LÓGICA PRINCIPAL DEL JUEGO
// ---------------------------------------------------------
@Composable
fun DondeEstaNivel1Game(
    kidName: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackAgent(context) }
    val db = remember { ResultsDatabase(context) }

    val images = listOf(
        Pair(R.drawable.gato_dentro, "dentro"),
        Pair(R.drawable.gato_fuera, "fuera"),
        Pair(R.drawable.pajaro_arriba, "arriba"),
        Pair(R.drawable.pajaro_abajo, "abajo"),
        Pair(R.drawable.nino_dentro, "dentro"),
        Pair(R.drawable.nino_fuera, "fuera"),
        Pair(R.drawable.pelota_dentro, "dentro"),
        Pair(R.drawable.pelota_fuera, "fuera"),
        Pair(R.drawable.flor_dentro, "dentro"),
        Pair(R.drawable.flor_fuera, "fuera")
    ).shuffled()

    var index by remember { mutableStateOf(0) }
    val currentImage by remember { derivedStateOf { images[index].first } }
    val correctAnswer by remember { derivedStateOf { images[index].second } }

    var message by remember { mutableStateOf("¿Dónde está el objeto?") }
    var attempts by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    val maxAttempts = 3
    var showConfetti by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var voskEngine by remember { mutableStateOf<VoskEngine?>(null) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var listenStartTime by remember { mutableStateOf(0L) }
    var saveCount by remember { mutableStateOf(0) }
    val totalQuestions = 5

    // ---------------------------------------------------
    // 🔠 Funciones auxiliares
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

    suspend fun speakAndWait(text: String, delayAfter: Long = 800L) {
        feedback.speak(text)
        delay(text.length * 50L + delayAfter)
    }

    fun calculateScore(attempts: Int): Int {
        return when (attempts) {
            0 -> 100
            1 -> 80
            2 -> 60
            else -> 0
        }
    }

    fun saveResult(result: ActivityResult) {
        saveCount++
        db.insertResult(result)
        Log.i("DB_TRACK", """
            💾 [#${saveCount}] Guardado local
            ├ kidName: ${result.kidName}
            ├ activityName: ${result.activityName}
            ├ level: ${result.level}
            ├ attempts: ${result.attempts}
            ├ score: ${result.score}
            ├ correctAnswers: ${result.correctAnswers}
            └ timestamp: ${result.timestamp}
        """.trimIndent())
    }

    fun stopListeningCycle() {
        voskEngine?.stopListening()
        listeningJob?.cancel()
        isListening = false
        progress = 0f
    }

    fun startListeningCycle() {
        listeningJob?.cancel()
        voskEngine?.stopListening()
        isListening = true
        progress = 0f
        listenStartTime = System.currentTimeMillis()
        voskEngine?.startListening()
        listeningJob = scope.launch {
            val totalTime = 20000L; val interval = 100L; var elapsed = 0L
            while (elapsed < totalTime && isListening) {
                delay(interval); elapsed += interval
                progress = elapsed / totalTime.toFloat()
            }
            if (isListening) {
                stopListeningCycle()
                attempts++
                speakAndWait("Se acabó el tiempo, $kidName. Vamos a repetir la pregunta.")
                delay(1000)
                speakAndWait(message)
                startListeningCycle()
            }
        }
    }

    fun handleVoiceResult(rawText: String) {
        if (!isListening) return
        if (rawText.contains("\"partial\"")) return
        val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(rawText)
        val recognized = match?.groups?.get(1)?.value ?: rawText
        val normalized = Normalizer.normalize(recognized.lowercase(), Normalizer.Form.NFD)
        val clean = normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")

        val palabras = listOf("dentro", "fuera", "arriba", "abajo")
        val respuesta = palabras.firstOrNull { similar(clean, it) }

        if (respuesta == null) {
            scope.launch {
                stopListeningCycle()
                speakAndWait("No entendí bien. Repitamos: ${message.lowercase()}")
                startListeningCycle()
            }
            return
        }

        if (respuesta == correctAnswer) {
            stopListeningCycle()
            correctCount++
            scope.launch {
                speakAndWait("¡Excelente $kidName! El objeto está $correctAnswer.")
                val score = calculateScore(attempts)
                val result = ActivityResult(
                    kidName = kidName,
                    activityName = "¿Dónde está?",
                    level = 1,
                    attempts = attempts + 1,
                    score = score,
                    correctAnswers = correctCount,
                    totalQuestions = totalQuestions
                )
                saveResult(result)
                if (correctCount < totalQuestions) {
                    index = (index + 1) % images.size
                    attempts = 0
                    delay(1000)
                    message = "¿Dónde está el objeto?"
                    speakAndWait(message)
                    startListeningCycle()
                } else {
                    showConfetti = true
                    speakAndWait("🎉 ¡Muy bien $kidName! Has terminado todas las imágenes. ¡Felicitaciones!", 2000L)
                    delay(3000)
                    navController.popBackStack()
                }
            }
        } else {
            attempts++
            if (attempts < maxAttempts) {
                scope.launch {
                    stopListeningCycle()
                    speakAndWait("Casi $kidName, intenta otra vez. ${message.lowercase()}")
                    startListeningCycle()
                }
            } else {
                scope.launch {
                    stopListeningCycle()
                    speakAndWait("No te preocupes $kidName. La respuesta correcta era $correctAnswer.")

                    // 🔹 Guardar resultado de intento fallido
                    val result = ActivityResult(
                        kidName = kidName,
                        activityName = "¿Dónde está?",
                        level = 1,
                        attempts = attempts,
                        score = 0,
                        correctAnswers = correctCount,
                        totalQuestions = totalQuestions
                    )
                    saveResult(result)

                    attempts = 0
                    index = (index + 1) % images.size
                    delay(1500)
                    speakAndWait("Vamos con otra imagen.")
                    startListeningCycle()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        voskEngine = VoskEngine(context, { handleVoiceResult(it) }, VoskEngine.Mode.HYBRID)
        delay(2000)
        speakAndWait("Muy bien $kidName, observa la imagen y dime con tu voz, ¿dónde está el objeto?")
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
            Text("📍 ¿Dónde está? - Nivel 1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(280.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = currentImage), contentDescription = "Imagen espacial", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(20.dp))
            Text(message, fontSize = 20.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Text("Intentos: $attempts / $maxAttempts", fontSize = 16.sp, color = Color.Gray)
            Text("Aciertos: $correctCount / $totalQuestions", fontSize = 16.sp, color = Color(0xFF1565C0))
            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (!isListening) startListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) {
                    Text(if (isListening) "🎧 Escuchando..." else "🎙️ Hablar", color = Color.White)
                }
                Button(onClick = { stopListeningCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))) {
                    Text("🛑 Detener", color = Color.White)
                }
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
