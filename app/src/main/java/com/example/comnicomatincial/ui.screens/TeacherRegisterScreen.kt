package com.example.comnicomatincial.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.comnicomatincial.R
import com.example.comnicomatincial.audio.MusicPlayer
import com.example.comnicomatincial.audio.SfxPlayer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TeacherRegisterScreen(
    onStart: (fullName: String, email: String, password: String, grade: String) -> Unit = { _, _, _, _ -> },
    onRegistered: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }
    val auth = FirebaseAuth.getInstance()

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

    // 🔧 Variables visuales editables directamente
    val titleSize = 40.sp             // tamaño del título principal
    val labelSize = 20.sp             // subtítulos (“¿Qué grado enseñas?”)
    val formFontSize = 16.sp          // texto dentro de los campos
    val chipFontSize = 18.sp          // texto dentro de los chips
    val titleColors = listOf(Color(0xFFC7CDD3), Color(0xFFE7EBEF)) // gradiente título
    val labelColor = Color(0xFF0F3A81)        // color subtítulos
    val formLabelColor = Color(0xFF1565C0)    // color borde campos
    val backgroundOverlay = Color(0x88000000) // fondo oscuro translúcido
    val panelCorner = 32.dp           // radios panel
    val panelMinHeight = 440.dp       // alto mínimo panel
    val panelPaddingH = 20.dp         // padding horizontal
    val panelPaddingV = 25.dp         // padding vertical
    val topSpacer = 15.dp             // espacio superior
    val fieldSpacing = 20.dp          // espacio entre campos
    val betweenTitlePanel = 10.dp     // espacio título/panel
    val betweenPanelButton = 18.dp    // espacio panel/botón
    val buttonBottomMargin = 80.dp    // margen inferior
    val buttonWidthFraction = 0.66f   // ancho relativo botón
    val buttonPressScale = 0.92f      // escala al presionar
    val buttonOffsetX = 0.dp          // 🔧 mueve horizontal botón
    val buttonOffsetY = 30.dp          // 🔧 mueve vertical botón
    val chipHeight = 56.dp
    val chipCorner = 18.dp
    val chipSpacing = 12.dp
    val chipStroke = 3.dp
    val successBg = Color(0xCC000000)
    val successTextSize = 22.sp
    val successDelay = 4000L

    // 🔧 Gradientes
    val prekGradient = listOf(Color(0xFF1565C0), Color(0xFF64B5F6))
    val kindGradient = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
    val bothGradient = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val listo = fullName.isNotBlank() && email.isNotBlank() && password.length == 6
    var gradeSelected by rememberSaveable { mutableStateOf("Prekínder") }
    var showSuccess by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fondo_registro_pro),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(backgroundOverlay)) // 🔧 fondo translúcido

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(topSpacer)) // 🔧 espacio superior

            BlueHeadline("Registro profesor/a", titleSize, titleColors) // 🔧 título principal

            Spacer(Modifier.height(betweenTitlePanel))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = panelMinHeight)
                    .offset(y = 0.dp),
                shape = RoundedCornerShape(panelCorner),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.panel_rigistro_pro),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.88f)))

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = panelPaddingH, vertical = panelPaddingV),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BlueOutlinedField(
                            label = "NOMBRE COMPLETO",
                            value = fullName,
                            onValueChange = { fullName = it },
                            fontSize = formFontSize,
                            borderColor = formLabelColor
                        )
                        Spacer(Modifier.height(fieldSpacing))

                        BlueOutlinedField(
                            label = "CORREO ELECTRÓNICO",
                            value = email,
                            onValueChange = { email = it },
                            fontSize = formFontSize,
                            borderColor = formLabelColor
                        )
                        Spacer(Modifier.height(fieldSpacing))

                        BlueOutlinedField(
                            label = "CONTRASEÑA (6 caracteres)",
                            value = password,
                            onValueChange = { if (it.length <= 6) password = it },
                            fontSize = formFontSize,
                            borderColor = formLabelColor,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(fieldSpacing))

                        Text(
                            text = "¿Qué grado enseñas?",
                            fontSize = labelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(chipSpacing)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(chipSpacing)
                            ) {
                                GradeChipPro(
                                    modifier = Modifier.weight(1f),
                                    text = "Prekínder",
                                    selected = gradeSelected == "Prekínder",
                                    gradient = prekGradient,
                                    fontSize = chipFontSize,
                                    chipHeight = chipHeight,
                                    chipCorner = chipCorner,
                                    chipStroke = chipStroke
                                ) { gradeSelected = "Prekínder" }

                                GradeChipPro(
                                    modifier = Modifier.weight(1f),
                                    text = "Kínder",
                                    selected = gradeSelected == "Kínder",
                                    gradient = kindGradient,
                                    fontSize = chipFontSize,
                                    chipHeight = chipHeight,
                                    chipCorner = chipCorner,
                                    chipStroke = chipStroke
                                ) { gradeSelected = "Kínder" }
                            }

                            GradeChipPro(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Ambos",
                                selected = gradeSelected == "Ambos",
                                gradient = bothGradient,
                                fontSize = chipFontSize,
                                chipHeight = chipHeight,
                                chipCorner = chipCorner,
                                chipStroke = chipStroke
                            ) { gradeSelected = "Ambos" }
                        }
                    }
                }
            }

            Spacer(Modifier.height(betweenPanelButton))

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) buttonPressScale else 1f, label = "btnScalePro")

            Image(
                painter = painterResource(R.drawable.btn_registrarme_pro),
                contentDescription = "Registrarme",
                modifier = Modifier
                    .fillMaxWidth(buttonWidthFraction)
                    .scale(scale)
                    .offset(x = buttonOffsetX, y = buttonOffsetY) // 🔧 mueve el botón
                    .clickable(interactionSource = interaction, indication = null) {
                        vibrateStrong(ctx)
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        music.stop()
                        scope.launch {
                            sfx.stop()
                            sfx.play(R.raw.sfx_empezar, loop = false, volume = 1f)
                            delay(2800)
                            sfx.stop()
                            if (listo) {
                                auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
                                                onStart(fullName.trim(), email.trim(), password.trim(), gradeSelected)
                                                showSuccess = true
                                                scope.launch {
                                                    delay(successDelay)
                                                    showSuccess = false
                                                    onRegistered()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(ctx, "Error al registrar. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                    }
                    .padding(bottom = buttonBottomMargin),
                contentScale = ContentScale.FillWidth
            )
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(successBg),
                contentAlignment = Alignment.Center
            ) {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉✨🎈", fontSize = 26.sp)
                        Spacer(Modifier.height(6.dp))
                        BlueHeadline("¡Registro completado, revisa tu correo!", successTextSize, titleColors)
                        Spacer(Modifier.height(8.dp))
                        Text("Regresando.....", color = Color(0xFF292B2D), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text("🎊🎉✨", fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

/* ======= COMPONENTES AUXILIARES ======= */

@Composable
private fun BlueHeadline(text: String, sizeSp: androidx.compose.ui.unit.TextUnit, colors: List<Color>) {
    val styled = buildAnnotatedString {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') append(ch) else {
                withStyle(SpanStyle(color = colors[i % colors.size], fontWeight = FontWeight.ExtraBold)) { append(ch) }
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
            shadow = Shadow(Color.Black.copy(alpha = 0.18f), Offset(2f, 2f), 4f)
        )
    )
}

@Composable
private fun BlueOutlinedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    borderColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Box(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(3.dp, Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.7f)))), shape).clip(shape)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { Text(label, color = borderColor, fontWeight = FontWeight.SemiBold, fontSize = fontSize) },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            visualTransformation = visualTransformation,
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
    }
}

@Composable
private fun GradeChipPro(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    gradient: List<Color>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    chipHeight: Dp,
    chipCorner: Dp,
    chipStroke: Dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(chipCorner)
    val border = if (selected) BorderStroke(chipStroke, gradient.first()) else BorderStroke(chipStroke, gradient.first().copy(alpha = 0.4f))
    Surface(
        modifier = modifier.height(chipHeight).clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = border
    ) {
        Box(
            modifier = Modifier.background(Brush.horizontalGradient(if (selected) gradient else listOf(Color.White, Color.White))).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = if (selected) Color.White else Color(0xFF1F2937))
        }
    }
}

/* ====== Vibración ====== */
private fun vibrateStrong(context: Context) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator
    } else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    else @Suppress("DEPRECATION") v?.vibrate(100)
}
