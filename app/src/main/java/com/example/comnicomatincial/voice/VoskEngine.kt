package com.example.comnicomatincial.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream

/**
 * Motor de voz flexible basado en Vosk con soporte para:
 * - CONTINUOUS: reinicia automáticamente tras cada resultado o silencio.
 * - HYBRID: escucha continua controlada externamente (ideal para juegos).
 * - TURN_BASED: escucha por turnos (una frase y se detiene).
 * - Detección de silencio para feedback dinámico.
 * - Ignora resultados parciales para evitar falsos positivos.
 */
class VoskEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val mode: Mode = Mode.HYBRID,
    private val onSilenceDetected: (() -> Unit)? = null, // 🆕 callback opcional
    private val silenceTimeoutMs: Long = 5000L // 🕒 tiempo sin voz para avisar (por defecto 5 s)
) : RecognitionListener {

    enum class Mode { CONTINUOUS, HYBRID, TURN_BASED }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var isModelReady = false
    private var isLoading = false

    private var silenceJob: Job? = null // 🆕 corutina para medir silencio

    // ---------------------------------------------------------------------------------------------
    // 🔹 Inicialización
    // ---------------------------------------------------------------------------------------------

    fun initModel(onLoaded: (() -> Unit)? = null) {
        if (isModelReady || isLoading) return
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelDir = File(context.filesDir, "model")
                if (!modelDir.exists()) {
                    Log.i("VoskEngine", "📦 Copiando modelo desde assets...")
                    copyAssets("model", modelDir)
                }

                model = Model(modelDir.absolutePath)
                recognizer = Recognizer(model, 16000.0f)
                isModelReady = true
                Log.i("VoskEngine", "✅ Modelo cargado correctamente desde ${modelDir.absolutePath}")

                withContext(Dispatchers.Main) { onLoaded?.invoke() }
            } catch (e: Exception) {
                Log.e("VoskEngine", "❌ Error al cargar modelo: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun copyAssets(assetDir: String, outDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetDir) ?: return
        outDir.mkdirs()

        for (fileName in files) {
            val inPath = "$assetDir/$fileName"
            val outFile = File(outDir, fileName)
            val subFiles = assetManager.list(inPath)
            if (subFiles?.isNotEmpty() == true) {
                copyAssets(inPath, outFile)
            } else {
                assetManager.open(inPath).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 🎙️ Control de la escucha
    // ---------------------------------------------------------------------------------------------

    fun startListening() {
        if (!isModelReady) {
            Log.w("VoskEngine", "⚠️ Modelo aún no está listo, inicializando...")
            initModel { startListening() }
            return
        }

        try {
            recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            Log.i("VoskEngine", "🎙️ Escuchando (${mode.name.lowercase()})...")

            startSilenceTimer() // 🆕 arranca el temporizador de silencio
        } catch (e: Exception) {
            Log.e("VoskEngine", "❌ Error al iniciar escucha: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechService?.stop()
            speechService = null
            silenceJob?.cancel()
            Log.i("VoskEngine", "🛑 Escucha detenida manualmente.")
        } catch (e: Exception) {
            Log.e("VoskEngine", "⚠️ Error al detener: ${e.message}")
        }
    }

    fun restart() {
        Log.d("VoskEngine", "🔄 Reiniciando escucha manualmente...")
        stopListening()
        startListening()
    }

    fun shutdown() {
        try {
            recognizer?.close()
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
            silenceJob?.cancel()
            Log.i("VoskEngine", "🧹 VoskEngine apagado limpiamente.")
        } catch (e: Exception) {
            Log.e("VoskEngine", "⚠️ Error en shutdown: ${e.message}")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 🧠 Detección de silencio
    // ---------------------------------------------------------------------------------------------

    private fun startSilenceTimer() {
        silenceJob?.cancel()
        silenceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(silenceTimeoutMs)
            Log.d("VoskEngine", "🤫 Silencio detectado tras ${silenceTimeoutMs / 1000}s")
            onSilenceDetected?.invoke()
        }
    }

    private fun resetSilenceTimer() {
        silenceJob?.cancel()
        silenceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(silenceTimeoutMs)
            Log.d("VoskEngine", "🤫 Silencio detectado tras ${silenceTimeoutMs / 1000}s")
            onSilenceDetected?.invoke()
        }
    }

    // ---------------------------------------------------------------------------------------------
// 🔊 Listener de resultados — ahora detecta finales reales correctamente
// ---------------------------------------------------------------------------------------------

    override fun onPartialResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            resetSilenceTimer()
            Log.d("VoskEngine", "🟡 Parcial detectado: $hypothesis")
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis.isNullOrBlank()) {
            Log.w("VoskEngine", "⚠️ Resultado vacío o nulo recibido en onResult()")
            return
        }

        resetSilenceTimer()
        Log.d("VoskEngine", "🟢 Resultado detectado (posible final): $hypothesis")

        try {
            // ⚙️ Intentamos extraer el texto JSON limpio
            val match = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(hypothesis)
            val text = match?.groups?.get(1)?.value?.trim() ?: hypothesis.trim()

            if (text.isNotEmpty()) {
                Log.i("VoskEngine", "✅ Texto final reconocido: \"$text\"")
                onSpeechRecognized(hypothesis) // 🚀 Notifica al Composable
            } else {
                Log.w("VoskEngine", "⚠️ Texto vacío en resultado, ignorado.")
            }
        } catch (e: Exception) {
            Log.e("VoskEngine", "💥 Error procesando resultado: ${e.message}")
        }

        // 🔁 Reinicio o parada según el modo
        when (mode) {
            Mode.CONTINUOUS -> {
                try {
                    speechService?.startListening(this)
                    Log.d("VoskEngine", "♻️ Reinicio automático (modo continuo)")
                } catch (e: Exception) {
                    Log.e("VoskEngine", "⚠️ Error al reiniciar: ${e.message}")
                }
            }
            Mode.HYBRID -> {
                Log.d("VoskEngine", "🎧 Fin del reconocimiento (modo híbrido, sin reinicio automático)")
            }
            Mode.TURN_BASED -> {
                Log.d("VoskEngine", "✅ Finalizado (modo por turnos)")
                stopListening()
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        // 🔹 Este callback rara vez se usa con SpeechService, pero lo dejamos como respaldo
        if (!hypothesis.isNullOrBlank()) {
            Log.d("VoskEngine", "🏁 onFinalResult recibido (backup): $hypothesis")
            onSpeechRecognized(hypothesis)
        }
    }

    override fun onTimeout() {
        when (mode) {
            Mode.CONTINUOUS -> {
                try {
                    speechService?.startListening(this)
                    Log.d("VoskEngine", "⏱️ Reinicio tras timeout (modo continuo)")
                } catch (e: Exception) {
                    Log.e("VoskEngine", "⚠️ Error tras timeout: ${e.message}")
                }
            }
            Mode.HYBRID -> {
                Log.d("VoskEngine", "⏲️ Timeout detectado (modo híbrido, sin reinicio automático)")
            }
            Mode.TURN_BASED -> {
                Log.d("VoskEngine", "⏲️ Timeout finalizado (modo por turnos)")
                stopListening()
            }
        }
    }

    override fun onError(e: Exception?) {
        Log.e("VoskEngine", "⚠️ Error en reconocimiento: ${e?.message}")
        if (mode == Mode.CONTINUOUS) {
            try {
                speechService?.startListening(this)
                Log.d("VoskEngine", "🔁 Reinicio tras error (modo continuo)")
            } catch (_: Exception) { }
        } else {
            Log.d("VoskEngine", "🚫 Sin reinicio automático (modo híbrido o por turnos)")
        }
    }
}
