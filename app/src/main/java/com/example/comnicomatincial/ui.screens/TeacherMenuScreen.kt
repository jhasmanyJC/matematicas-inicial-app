package com.example.comnicomatincial.ui.screens

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.comnicomatincial.R
import com.example.comnicomatincial.audio.MusicPlayer
import com.example.comnicomatincial.audio.SfxPlayer
import com.example.comnicomatincial.data.TeacherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/* ===== CONFIG ===== */
private object TMENUCFG {
    val TITLE_SIZE = 30.sp
    val BUTTON_PRESS_SCALE = 0.92f
    val RESULTADOS_COLOR = Color(0xFFEBF1ED)
}

/* === Colores de chips (igual que registro) === */
private val PREK_GRADIENT = listOf(Color(0xFF1565C0), Color(0xFF64B5F6))
private val KIND_GRADIENT = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
private val BOTH_GRADIENT = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
private val CHIP_BORDER = 3.dp
private val CHIP_CORNER = 18.dp

@Composable
fun TeacherMenuScreen(
    profName: String,
    firstName: String,
    email: String,
    grade: String,
    onPreKinder: () -> Unit = {},
    onKinder: () -> Unit = {},
    onExit: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }

    var profNameState by remember { mutableStateOf(profName) }
    var gradeState by remember { mutableStateOf(grade) }
    var showEditDialog by remember { mutableStateOf(false) }

    val currentDateTime = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

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

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.menu_fondo_pro),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Color(0x77000000)))

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido/a Prof. $firstName",
                fontSize = TMENUCFG.TITLE_SIZE,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.menu_panel_pro),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.9f)))

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📌 Mis Datos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF0D47A1)
                            )
                            Text(
                                text = "✏️ Editar",
                                color = Color(0xFF0D47A1),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    vibrateStrong(ctx)
                                    showEditDialog = true
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("👤 Nombre: $profNameState", fontSize = 16.sp)
                        Text("📧 Email: $email", fontSize = 16.sp)
                        Text("🎓 Grado: $gradeState", fontSize = 16.sp)
                        Text("⏰ Inicio Sesión: $currentDateTime", fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "RESULTADOS POR GRADO",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TMENUCFG.RESULTADOS_COLOR,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.btn_prekinder),
                contentDescription = "Prekínder",
                modifier = Modifier
                    .width(200.dp)
                    .height(130.dp)
                    .scale(1f)
                    .clickable {
                        vibrateStrong(ctx)
                        sfxClick(sfx, scope)
                        onPreKinder()
                    }
                    .offset(x = 0.dp, y = 10.dp),
                contentScale = ContentScale.FillBounds
            )

            Spacer(Modifier.height(12.dp))

            Image(
                painter = painterResource(R.drawable.btn_kinder),
                contentDescription = "Kínder",
                modifier = Modifier
                    .width(200.dp)
                    .height(130.dp)
                    .scale(1f)
                    .clickable {
                        vibrateStrong(ctx)
                        sfxClick(sfx, scope)
                        onKinder()
                    }
                    .offset(x = 0.dp, y = 40.dp),
                contentScale = ContentScale.FillBounds
            )
        }

        Image(
            painter = painterResource(R.drawable.btn_atras),
            contentDescription = "Atrás",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(5.dp)
                .width(165.dp)
                .height(85.dp)
                .clickable {
                    vibrateStrong(ctx)
                    sfxClick(sfx, scope)
                    onBack()
                },
            contentScale = ContentScale.FillBounds
        )

        Image(
            painter = painterResource(R.drawable.btn_salir),
            contentDescription = "Salir",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .width(150.dp)
                .height(70.dp)
                .clickable {
                    vibrateStrong(ctx)
                    sfxClick(sfx, scope)
                    onExit()
                    activity?.finishAffinity()
                },
            contentScale = ContentScale.FillBounds
        )
    }

    // ======= DIALOGO DE EDICIÓN =======
    if (showEditDialog) {
        var newName by remember { mutableStateOf(profNameState) }
        var selectedGrade by remember { mutableStateOf(gradeState) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        val db = TeacherDatabase(ctx)
                        val updated = db.updateTeacherData(email, newName, selectedGrade)
                        db.close()

                        if (updated) {
                            profNameState = newName.trim()
                            gradeState = selectedGrade.trim()
                            Toast.makeText(ctx, "✅ Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "⚠️ No se pudo actualizar el registro", Toast.LENGTH_SHORT).show()
                        }
                        showEditDialog = false
                    }
                }) {
                    Text("Guardar", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            title = { Text("Editar perfil", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre completo") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Selecciona el grado:", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))

                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradeChip(
                                text = "Prekínder",
                                selected = selectedGrade == "Prekínder",
                                gradient = PREK_GRADIENT
                            ) { selectedGrade = "Prekínder" }

                            GradeChip(
                                text = "Kínder",
                                selected = selectedGrade == "Kínder",
                                gradient = KIND_GRADIENT
                            ) { selectedGrade = "Kínder" }
                        }

                        GradeChip(
                            text = "Ambos",
                            selected = selectedGrade == "Ambos",
                            gradient = BOTH_GRADIENT
                        ) { selectedGrade = "Ambos" }
                    }
                }
            }
        )
    }
}

@Composable
private fun GradeChip(
    text: String,
    selected: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(CHIP_CORNER)
    Box(
        modifier = Modifier
            .width(120.dp) // 👈 reemplaza weight() por un ancho fijo
            .height(50.dp)
            .clip(shape)
            .border(
                width = CHIP_BORDER,
                color = if (selected) gradient.first() else Color.Gray.copy(alpha = 0.3f),
                shape = shape
            )
            .background(
                Brush.horizontalGradient(
                    if (selected) gradient else listOf(Color.White, Color.White)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) Color.White else Color(0xFF1F2937)
        )
    }
}


/* ====== Helpers ====== */
private fun sfxClick(sfx: SfxPlayer, scope: CoroutineScope) {
    scope.launch {
        sfx.stop()
        sfx.play(R.raw.sfx_empezar, loop = false, volume = 1f)
        delay(2100)
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
