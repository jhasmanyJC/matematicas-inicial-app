package com.example.comnicomatincial.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.util.Log
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.comnicomatincial.R
import com.example.comnicomatincial.audio.MusicPlayer
import com.example.comnicomatincial.audio.SfxPlayer
import com.example.comnicomatincial.data.Kid
import com.example.comnicomatincial.data.KidDatabase
import com.example.comnicomatincial.data.ResultsDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.example.comnicomatincial.data.FirestoreUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private object MENUCFG {
    val TITLE_SIZE = 22.sp
    const val BUTTON_PRESS_SCALE = 0.92f
}

@Composable
fun MenuScreen(
    kidName: String,
    onJuegos: () -> Unit = {},
    onEjercicios: () -> Unit = {},
    onLogros: () -> Unit = {},
    onRegistro: () -> Unit = {},
    onExit: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }

    var kidData by remember { mutableStateOf<Kid?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showResultsDialog by remember { mutableStateOf(false) }
    var showLogrosDialog by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }

    val resultsDb = remember { ResultsDatabase(ctx) }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var localResults by remember { mutableStateOf(emptyList<com.example.comnicomatincial.data.ActivityResult>()) }

    // --- Cargar datos del niño ---
    LaunchedEffect(kidName) {
        val db = KidDatabase(ctx)
        kidData = db.getKidByName(kidName)
    }

    // --- Control de música ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> music.start(R.raw.bgm_registro, volume = 0.25f)
                Lifecycle.Event.ON_PAUSE -> music.pause()
                Lifecycle.Event.ON_STOP -> music.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            music.stop(); sfx.stop()
        }
    }

    // --- UI principal ---
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.menu_fondo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Color(0x77000000)))

        // --- Cabecera ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.avatar_default),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = (kidData?.kidName ?: kidName).uppercase(),
                    fontSize = MENUCFG.TITLE_SIZE,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Image(
                painter = painterResource(R.drawable.logo_menu),
                contentDescription = "Logo del Menú",
                modifier = Modifier.size(100.dp)
            )
        }

        // --- Botón Registro (Datos del Niño) ---
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 120.dp)
        ) {
            MenuButton(
                resId = R.drawable.btn_registro,
                desc = "Registro",
                buttonWidth = 150.dp,
                buttonHeight = 70.dp
            ) {
                vibrateStrong(ctx); sfxClick(sfx, scope)
                val db = KidDatabase(ctx)
                kidData = db.getKidByName(kidName)
                if (kidData != null) showDialog = true
                onRegistro()
            }
        }

        // --- Botones principales ---
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuButton(
                R.drawable.btn_juegos, "Juegos", 140.dp, 150.dp, Modifier.offset(x = 40.dp, y = 65.dp)
            ) { vibrateStrong(ctx); sfxClick(sfx, scope); onJuegos() }

            Spacer(Modifier.height(10.dp))
            MenuButton(
                R.drawable.btn_ejercicios, "Ejercicios", 130.dp, 170.dp, Modifier.offset(x = -40.dp, y = 70.dp)
            ) { vibrateStrong(ctx); sfxClick(sfx, scope); onEjercicios() }
            Spacer(Modifier.height(10.dp))
            MenuButton(
                R.drawable.btn_logros, "Logros", 110.dp, 150.dp, Modifier.offset(x = 70.dp, y = 75.dp)
            ) {
                vibrateStrong(ctx); sfxClick(sfx, scope)
                scope.launch { localResults = resultsDb.getResultsByKid(kidName) }
                showLogrosDialog = true
            }

            Spacer(Modifier.height(10.dp))
            MenuButton(
                R.drawable.btn_subir, "Subir Resultados", 160.dp, 100.dp, Modifier.offset(x = -70.dp, y = -20.dp)
            ) {
                vibrateStrong(ctx); sfxClick(sfx, scope)
                scope.launch {
                    localResults = resultsDb.getResultsByKid(kidName)
                    showResultsDialog = true
                }
            }
        }

        // --- Botones de navegación ---
        MenuButton(R.drawable.btn_atras, "Atrás", 160.dp, 90.dp,
            Modifier.align(Alignment.BottomStart).padding(5.dp)
        ) { vibrateStrong(ctx); sfxClick(sfx, scope); onBack() }

        MenuButton(R.drawable.btn_salir, "Salir", 145.dp, 75.dp,
            Modifier.align(Alignment.BottomEnd).padding(15.dp)
        ) { vibrateStrong(ctx); sfxClick(sfx, scope); onExit(); activity?.finishAffinity() }
    }

    /* =======================
       DIALOGO DE DATOS DEL NIÑO
       ======================= */
    if (showDialog && kidData != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar", color = Color.White)
                }
            },
            title = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF64B5F6), Color(0xFF90CAF9))
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "👦 Datos del Niño 👦",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text("👦 Nombre: ${kidData!!.kidName}", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    Text("👨 Papá: ${kidData!!.dadName}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Text("👩 Mamá: ${kidData!!.momName}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    Text("🏫 Grado: ${kidData!!.grade}", color = Color(0xFFF57C00), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }

    /* =======================
   DIALOGO DE RESULTADOS INDIVIDUALES
   ======================= */
    if (showLogrosDialog) {
        val resumen = generarResumenIndividual(localResults)

        AlertDialog(
            onDismissRequest = { showLogrosDialog = false },
            confirmButton = {
                TextButton(onClick = { showLogrosDialog = false }) {
                    Text("Cerrar", color = Color.White)
                }
            },
            title = {
                Text(
                    "📘 Reporte Individual de ${kidName.uppercase()}",
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (localResults.isEmpty()) {
                    Text("Aún no hay actividades registradas.", color = Color.Gray)
                } else {
                    val detalle = resumen["detalle"] as Map<String, List<ResumenNivel>>

                    Column(Modifier.verticalScroll(rememberScrollState())) {

                        // 🟢 PANEL 1: RESUMEN GLOBAL
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("👦 Nombre: $kidName", fontWeight = FontWeight.Bold)
                                Text("📊 Actividades subidas: ${resumen["actividadesSubidas"]}")
                                Text("⭐ Promedio de puntaje: ${resumen["promedioGlobal"]}%")
                            }
                        }

                        // 🟢 PANEL 2: DETALLE POR ACTIVIDAD
                        detalle.forEach { (actividad, niveles) ->
                            Text(
                                "🎨 Actividad: $actividad",
                                color = Color(0xFF4A148C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(Modifier.height(4.dp))

                            niveles.forEach { n ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text("🧩 Nivel ${n.nivel}", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                        Text("🔢 Intentos: ${n.intentosTotales}", color = Color(0xFF2E7D32))
                                        Text("⭐ Puntaje: ${n.mejorPuntaje}%", color = Color(0xFFF57C00))
                                        Text("🎯 Aciertos: ${n.aciertos}", color = Color(0xFF6A1B9A))
                                        Text("📅 Fecha: ${n.fecha}", color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            },
            containerColor = Color.White
        )
    }



    if (showResultsDialog) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val recent = localResults
            .filter { isWithinLastThreeDays(it.timestamp) }
            .sortedByDescending { it.timestamp }
            .groupBy { sdf.format(Date(it.timestamp)) }

        val expandedDatesUpload = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showResultsDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    Log.i("UPLOAD_DEBUG", "🚀 Botón 'Subir Resultados' presionado.")
                    showResultsDialog = false

                    scope.launch {
                        val local = resultsDb.getAllResults()
                        Log.i("UPLOAD_DEBUG", "📦 Total resultados locales: ${local.size}")

                        if (local.isNotEmpty()) {
                            Toast.makeText(ctx, "Subiendo resultados, por favor espera...", Toast.LENGTH_SHORT).show()

                            Log.i("UPLOAD_DEBUG", "📤 Iniciando subida con FirestoreUploader...")
                            FirestoreUploader.subirResultados(ctx)

                            Log.i("UPLOAD_DEBUG", "✅ Subida enviada al servidor (ver FIRESTORE_TRACK para detalles).")
                            showConfirmation = true
                        } else {
                            Log.w("UPLOAD_DEBUG", "⚠️ No hay resultados locales para subir.")
                            Toast.makeText(ctx, "No hay resultados para subir.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Subir", color = Color(0xFF2E7D32))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.i("UPLOAD_DEBUG", "❌ Usuario canceló la subida de resultados.")
                    showResultsDialog = false
                }) {
                    Text("Cancelar", color = Color.Red)
                }
            },
            title = { Text("📊 Resultados recientes", color = Color(0xFF1E88E5)) },
            text = {
                if (recent.isEmpty()) {
                    Text("No hay resultados recientes para subir.", color = Color.Gray)
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        recent.forEach { (fecha, lista) ->
                            val expanded = expandedDatesUpload.contains(fecha)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        if (expanded) expandedDatesUpload.remove(fecha)
                                        else expandedDatesUpload.add(fecha)
                                    },
                                color = Color.Transparent
                            ) {
                                Box(
                                    Modifier
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(Color(0xFF64B5F6), Color(0xFF90CAF9))
                                            )
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📅 $fecha",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Icon(
                                            imageVector = if (expanded)
                                                Icons.Filled.KeyboardArrowUp
                                            else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = expanded) {
                                Column(Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                                    lista.sortedByDescending { it.id }.forEach {
                                        Text(
                                            "🎨 ${it.activityName}",
                                            color = Color(0xFF6A1B9A),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "💡 ${getExerciseDescription(it.activityName)}",
                                            color = Color.DarkGray,
                                            fontSize = 13.sp
                                        )
                                        Text("🔢 Intentos: ${it.attempts}", color = Color(0xFF2E7D32))
                                        Text("⭐ Puntaje: ${it.score}", color = Color(0xFFF57C00))
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.White
        )
    }



    /* =======================
       DIALOGO DE CONFIRMACION FINAL
       ======================= */
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            confirmButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Aceptar", color = Color.White)
                }
            },
            title = { Text("🎉 Subida completada", color = Color(0xFF2E7D32)) },
            text = {
                Text(
                    "Los resultados recientes se subieron correctamente a Firestore.",
                    color = Color.DarkGray
                )
            },
            containerColor = Color.White
        )
    }
}

/* =======================
   FUNCIONES AUXILIARES
   ======================= */
private fun getExerciseDescription(name: String): String = when {
    name.contains("color", true) -> "Ayuda al niño a reconocer y nombrar colores básicos."
    name.contains("forma", true) -> "Desarrolla el reconocimiento de figuras geométricas."
    name.contains("número", true) -> "Fomenta el conteo y la comprensión numérica."
    else -> "Fortalece habilidades cognitivas y de observación."
}

private fun isWithinLastThreeDays(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    val diff = (now.timeInMillis - date.timeInMillis) / (1000 * 60 * 60 * 24)
    return diff in 0..2
}

/* =======================
   BOTÓN REUSABLE
   ======================= */
@Composable
private fun MenuButton(
    resId: Int,
    desc: String,
    buttonWidth: Dp,
    buttonHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) MENUCFG.BUTTON_PRESS_SCALE else 1f, label = desc)

    Image(
        painter = painterResource(resId),
        contentDescription = desc,
        modifier = modifier
            .width(buttonWidth)
            .height(buttonHeight)
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentScale = ContentScale.FillBounds
    )
}

// =======================
// 🧠 GENERADOR DE REPORTE GLOBAL POR NIÑO
// =======================
private fun generarReporteEstudiante(
    kidName: String,
    results: List<com.example.comnicomatincial.data.ActivityResult>
): Map<String, Any> {
    if (results.isEmpty()) return mapOf("kidName" to kidName, "status" to "Sin resultados")

    val totalActividades = results.size
    val completadas = results.count { it.score >= 60 }
    val eficienciaPromedio = results.map { it.score }.average().toInt()

    val estadoGeneral = when {
        eficienciaPromedio < 40 -> "🔴 NECESITA AYUDA URGENTE"
        eficienciaPromedio < 60 -> "🟡 REQUIERE ATENCIÓN"
        eficienciaPromedio < 80 -> "🟢 EN BUEN CAMINO"
        else -> "💙 EXCELENTE DESEMPEÑO"
    }

    val detallePorActividad = results.groupBy { it.activityName }.mapValues { (_, lista) ->
        val mejorScore = lista.maxOfOrNull { it.score } ?: 0
        val intentosTotales = lista.sumOf { it.attempts }
        val aciertosProm = lista.map { it.correctAnswers }.average()
        val completado = mejorScore >= 60
        mapOf(
            "estado" to if (completado) "✅ COMPLETADO" else "❌ EN PROGRESO",
            "mejorScore" to mejorScore,
            "intentosTotales" to intentosTotales,
            "aciertosPromedio" to aciertosProm,
            "eficiencia" to "$mejorScore%"
        )
    }

    return mapOf(
        "kidName" to kidName,
        "estadoGeneral" to estadoGeneral,
        "progresoGlobal" to mapOf(
            "nivelesCompletados" to completadas,
            "actividadesTotales" to totalActividades,
            "eficienciaGlobal" to "$eficienciaPromedio%",
            "tendencia" to "⬆️ MEJORANDO"
        ),
        "detallePorActividad" to detallePorActividad,
        "timestamp" to System.currentTimeMillis()
    )
}


/* =======================
   HELPERS DE SONIDO Y VIBRACION
   ======================= */
private fun sfxClick(sfx: SfxPlayer, scope: CoroutineScope) {
    scope.launch {
        sfx.stop()
        sfx.play(R.raw.sfx_empezar, loop = false, volume = 1f)
        delay(2800)
        sfx.stop()
    }
}

private fun vibrateStrong(context: Context) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        v?.vibrate(100)
    }
}

// =======================
// 🧩 RESUMEN INDIVIDUAL (por niño)
// =======================
data class ResumenNivel(
    val nivel: Int,
    val intentosTotales: Int,
    val mejorPuntaje: Int,
    val aciertos: String,
    val fecha: String
)

private fun generarResumenIndividual(
    results: List<com.example.comnicomatincial.data.ActivityResult>
): Map<String, Any> {
    if (results.isEmpty()) return emptyMap()

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Agrupar por actividad y nivel
    val agrupado = results.groupBy { it.activityName }.mapValues { (_, listaActividad) ->
        val porNivel = listaActividad.groupBy { it.level }.mapValues { (_, listaNivel) ->
            val intentosTotales = listaNivel.sumOf { it.attempts }
            val mejorPuntaje = listaNivel.maxOfOrNull { it.score } ?: 0
            val aciertosProm = listaNivel.map { it.correctAnswers }.average().toInt()
            val totalPreguntas = 5 // ajusta según cuántas preguntas haya por nivel
            val fecha = sdf.format(Date(listaNivel.maxOf { it.timestamp }))

            ResumenNivel(
                nivel = listaNivel.first().level,
                intentosTotales = intentosTotales,
                mejorPuntaje = mejorPuntaje,
                aciertos = "$aciertosProm/$totalPreguntas",
                fecha = fecha
            )
        }
        porNivel.values.sortedBy { it.nivel } // nivel 1, luego nivel 2
    }

    // Calcular resumen global
    val actividadesSubidas = agrupado.size // número de actividades únicas
    val promedioGlobal = results.map { it.score }.average().toInt()

    return mapOf(
        "actividadesSubidas" to actividadesSubidas,
        "promedioGlobal" to promedioGlobal,
        "detalle" to agrupado
    )
}

