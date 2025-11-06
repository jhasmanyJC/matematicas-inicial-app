package com.example.comnicomatincial.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ============== CONFIG RÁPIDA (editas aquí) ============== */
private object REGCFG {
    val MUSIC_RES = R.raw.bgm_registro
    val SFX_BTN_RES = R.raw.sfx_empezar

    val TITLE_SIZE = 30.sp
    val LABEL_SIZE = 14.sp
    val GRADE_TEXT_SIZE = 18.sp

    val PANEL_CORNER = 32.dp
    const val PANEL_SCRIM_ALPHA = 0.88f
    val PANEL_MIN_HEIGHT = 440.dp
    val PANEL_PADDING_H = 20.dp
    val PANEL_PADDING_V = 20.dp
    val FIELD_SPACING = 12.dp

    val TOP_SPACER = 8.dp
    val BETWEEN_TITLE_PANEL = 10.dp
    val BETWEEN_PANEL_BUTTON = 18.dp

    const val BUTTON_WIDTH_FRACTION = 0.66f
    const val BUTTON_PRESS_SCALE = 0.92f
    val BUTTON_BOTTOM_MARGIN = 16.dp

    val CHIP_HEIGHT = 56.dp
    val CHIP_CORNER = 18.dp
    val CHIP_SPACING = 12.dp
    val CHIP_STROKE = 3.dp
    val CHIP_SELECTED_ELEV = 8.dp
    val CHIP_UNSELECTED_ELEV = 2.dp

    val PREK_GRADIENT = listOf(Color(0xFFFF7A7A), Color(0xFFFFC36B))
    val PREK_BORDER   = Color(0xFFFF7A7A)
    val KIND_GRADIENT = listOf(Color(0xFF66CCFF), Color(0xFF66E0A3))
    val KIND_BORDER   = Color(0xFF2FB4FF)

    val SUCCESS_BG = Color(0xCC000000)
    val SUCCESS_CARD_CORNER = 24.dp
    val SUCCESS_TEXT_SIZE = 22.sp
    const val SUCCESS_DELAY_MS = 4000L
}
/* ========================================================= */

@Composable
fun RegisterScreen(
    onStart: (kid: String, grade: String) -> Unit = { _, _ -> },
    onRegistered: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> music.start(REGCFG.MUSIC_RES, volume = 0.25f)
                Lifecycle.Event.ON_PAUSE  -> music.pause()
                Lifecycle.Event.ON_STOP   -> music.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            music.stop(); sfx.stop()
        }
    }

    // ===== Estado del formulario =====
    var kidName by rememberSaveable { mutableStateOf("") }
    var dadName by rememberSaveable { mutableStateOf("") }
    var momName by rememberSaveable { mutableStateOf("") }
    val listo = kidName.isNotBlank() && dadName.isNotBlank() && momName.isNotBlank()

    val grades = listOf("Prekínder (4 años)", "Kínder (5 años)")
    var gradeSelected by rememberSaveable { mutableStateOf(grades.first()) }

    var showSuccess by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // Fondo
        Image(
            painter = painterResource(R.drawable.fondo_registro),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Color(0x88000000)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(REGCFG.TOP_SPACER))

            RainbowHeadline("¡Registra al niño para empezar!", REGCFG.TITLE_SIZE)

            Spacer(Modifier.height(REGCFG.BETWEEN_TITLE_PANEL))

            // ===== Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = REGCFG.PANEL_MIN_HEIGHT),
                shape = RoundedCornerShape(REGCFG.PANEL_CORNER),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.escuelita_fondo),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = REGCFG.PANEL_SCRIM_ALPHA))
                    )

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = REGCFG.PANEL_PADDING_H,
                                vertical = REGCFG.PANEL_PADDING_V
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RainbowOutlinedField(
                            label = "¿CÓMO SE LLAMA EL NIÑO?",
                            value = kidName,
                            onValueChange = { kidName = it }
                        )
                        Spacer(Modifier.height(REGCFG.FIELD_SPACING))
                        RainbowOutlinedField(
                            label = "¿CÓMO SE LLAMA EL PAPÁ?",
                            value = dadName,
                            onValueChange = { dadName = it }
                        )
                        Spacer(Modifier.height(REGCFG.FIELD_SPACING))
                        RainbowOutlinedField(
                            label = "¿CÓMO SE LLAMA LA MAMÁ?",
                            value = momName,
                            onValueChange = { momName = it }
                        )
                        Spacer(Modifier.height(REGCFG.FIELD_SPACING))

                        Text(
                            text = "¿A qué grado pertenece?",
                            fontSize = REGCFG.LABEL_SIZE,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(REGCFG.CHIP_SPACING)
                        ) {
                            GradeChip(
                                modifier = Modifier.weight(1f),
                                text = "Prekínder",
                                selected = gradeSelected.startsWith("Prekínder"),
                                gradient = REGCFG.PREK_GRADIENT,
                                borderColor = REGCFG.PREK_BORDER
                            ) { gradeSelected = grades[0] }

                            GradeChip(
                                modifier = Modifier.weight(1f),
                                text = "Kínder",
                                selected = gradeSelected.startsWith("Kínder"),
                                gradient = REGCFG.KIND_GRADIENT,
                                borderColor = REGCFG.KIND_BORDER
                            ) { gradeSelected = grades[1] }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            Spacer(Modifier.height(REGCFG.BETWEEN_PANEL_BUTTON))

            // ===== Botón “¡Empezar!”
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (pressed) REGCFG.BUTTON_PRESS_SCALE else 1f, label = "btnScale"
            )

            Image(
                painter = painterResource(R.drawable.btn_empezar),
                contentDescription = "¡Empezar a jugar!",
                modifier = Modifier
                    .fillMaxWidth(REGCFG.BUTTON_WIDTH_FRACTION)
                    .scale(scale)
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) {
                        vibrateStrong(ctx)
                        haptics.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                        music.stop()
                        scope.launch {
                            sfx.stop()
                            sfx.play(REGCFG.SFX_BTN_RES, loop = false, volume = 1f)
                            delay(2800)
                            sfx.stop()

                            if (listo) {
                                // Guardar en SQLite
                                val db = KidDatabase(ctx)
                                db.insertKid(
                                    Kid(
                                        kidName = kidName.trim(),
                                        dadName = dadName.trim(),
                                        momName = momName.trim(),
                                        grade = gradeSelected.trim()
                                    )
                                )

                                onStart(kidName.trim(), gradeSelected)
                                showSuccess = true
                                delay(REGCFG.SUCCESS_DELAY_MS)
                                showSuccess = false
                                onRegistered()
                            }

                        }
                    }
                    .padding(bottom = REGCFG.BUTTON_BOTTOM_MARGIN),
                contentScale = ContentScale.FillWidth
            )
        }

        // ===== Overlay de ÉXITO (animado + confetti) =====
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(REGCFG.SUCCESS_BG),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(REGCFG.SUCCESS_CARD_CORNER),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉✨🎈", fontSize = 26.sp)
                        Spacer(Modifier.height(6.dp))
                        RainbowHeadline("¡Registro completado, bien hecho!", REGCFG.SUCCESS_TEXT_SIZE)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Volviendo.....",
                            color = Color(0xFF374151),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("🎊🎉✨", fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

/* =================== UI Helpers =================== */

@Composable
private fun RainbowHeadline(text: String, sizeSp: androidx.compose.ui.unit.TextUnit) {
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFFFFC107), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFFFF5722), Color(0xFF9C27B0)
    )
    val styled = buildAnnotatedString {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') append(ch) else {
                withStyle(
                    SpanStyle(
                        color = colors[i % colors.size],
                        fontWeight = FontWeight.ExtraBold
                    )
                ) { append(ch) }
            }
        }
    }
    Text(
        text = styled,
        fontSize = sizeSp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            shadow = Shadow(Color.Black.copy(alpha = 0.25f), Offset(3f, 3f), 6f)
        )
    )
}

@Composable
private fun RainbowLabel(text: String) {
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFC107), Color(0xFF66BB6A),
        Color(0xFF42A5F5), Color(0xFFAB47BC)
    )
    val styled = buildAnnotatedString {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') append(ch) else {
                withStyle(SpanStyle(color = colors[i % colors.size], fontWeight = FontWeight.Bold)) {
                    append(ch)
                }
            }
        }
    }
    Text(text = styled)
}

@Composable
private fun RainbowOutlinedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF5252), Color(0xFFFFC107),
                        Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFAB47BC)
                    )
                ),
                shape = shape
            )
            .padding(2.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { RainbowLabel(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            )
        )
    }
}

/* ================ Chips de grado ================ */
@Composable
private fun GradeChip(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    gradient: List<Color>,
    borderColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(REGCFG.CHIP_CORNER)
    val elev = if (selected) REGCFG.CHIP_SELECTED_ELEV else REGCFG.CHIP_UNSELECTED_ELEV
    val bg = if (selected) Brush.horizontalGradient(gradient) else null
    val border = if (selected) BorderStroke(REGCFG.CHIP_STROKE, borderColor)
    else BorderStroke(REGCFG.CHIP_STROKE, borderColor.copy(alpha = 0.4f))

    Surface(
        modifier = modifier
            .height(REGCFG.CHIP_HEIGHT)
            .shadow(elev, shape)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        tonalElevation = 0.dp,
        color = Color.Transparent,
        border = border
    ) {
        Box(
            modifier = Modifier
                .background(bg ?: Brush.linearGradient(listOf(Color.White, Color.White)))
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = REGCFG.GRADE_TEXT_SIZE,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else Color(0xFF1E293B)
            )
        }
    }
}

/* ================= Vibración ================= */
private fun vibrateStrong(context: Context) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v?.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        v?.vibrate(90)
    }
}
