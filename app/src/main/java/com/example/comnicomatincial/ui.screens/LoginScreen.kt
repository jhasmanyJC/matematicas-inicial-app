package com.example.comnicomatincial.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.example.comnicomatincial.data.KidDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onVamos: (kidName: String) -> Unit = {},   // ✅ Solo enviamos el nombre
    onSuscribete: () -> Unit = {}
) {
    val LOGO_SIZE: Dp = 180.dp
    val TITLE_SIZE = 40.sp
    val FORM_CORNER = 40.dp
    val VAMOS_SCALE_PRESS = 0.75f
    val SUSCRIBE_SCALE_PRESS = 1.20f
    val SUSCRIBE_WIDTH_FRACTION = 0.78f

    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val db = remember { KidDatabase(ctx) }

    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> music.start(R.raw.bgm_login, volume = 0.25f)
                Lifecycle.Event.ON_PAUSE -> music.pause()
                Lifecycle.Event.ON_STOP -> music.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            music.stop()
            sfx.stop()
        }
    }

    var kidNameInput by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fondo_login),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Color(0x99000000)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(R.drawable.logo_login),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(LOGO_SIZE)
                    .shadow(50.dp, RoundedCornerShape(50.dp))
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(8.dp))
            RainbowHeadline("¡Vamos a entrar!", TITLE_SIZE)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(FORM_CORNER))
                    .clip(RoundedCornerShape(FORM_CORNER))
            ) {
                Image(
                    painter = painterResource(R.drawable.escuelita_fondo),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.85f)))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RainbowOutlinedField(
                        value = kidNameInput,
                        onValueChange = { kidNameInput = it },
                        label = "NOMBRE DEL NIÑO/A",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMsg!!, color = Color.Red, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    val interactionVamos = remember { MutableInteractionSource() }
                    val pressedVamos by interactionVamos.collectIsPressedAsState()
                    val scaleVamos by animateFloatAsState(
                        if (pressedVamos) VAMOS_SCALE_PRESS else 1f, label = "vamosScale"
                    )

                    Image(
                        painter = painterResource(R.drawable.btn_vamos),
                        contentDescription = "¡Vamos!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scaleVamos)
                            .clickable(
                                interactionSource = interactionVamos,
                                indication = null
                            ) {
                                vibrateStrong(ctx)
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                )
                                music.stop()
                                scope.launch {
                                    sfx.stop()
                                    sfx.play(R.raw.sfx_vamos, loop = false, volume = 1f)
                                }

                                scope.launch {
                                    if (kidNameInput.isBlank()) {
                                        errorMsg = "⚠️ Por favor escriba los datos"
                                    } else {
                                        val kids = db.getKids()
                                        val match = kids.find {
                                            kidNameInput.trim().equals(it.kidName, ignoreCase = true)
                                        }
                                        if (match != null) {
                                            errorMsg = null
                                            delay(2800)
                                            sfx.stop()
                                            onVamos(match.kidName) // ✅ Solo pasamos el nombre
                                        } else {
                                            errorMsg = "❌ Nombre no encontrado en el registro"
                                        }
                                    }
                                }
                            },
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        val interactionSub = remember { MutableInteractionSource() }
        val pressedSub by interactionSub.collectIsPressedAsState()
        val scaleSub by animateFloatAsState(
            if (pressedSub) SUSCRIBE_SCALE_PRESS else 1f, label = "susScale"
        )

        Image(
            painter = painterResource(R.drawable.btn_suscribete),
            contentDescription = "¡Suscríbete y aprende!",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
                .fillMaxWidth(SUSCRIBE_WIDTH_FRACTION)
                .scale(scaleSub)
                .clickable(
                    interactionSource = interactionSub,
                    indication = null
                ) {
                    vibrateStrong(ctx)
                    haptics.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )
                    music.stop()
                    scope.launch {
                        sfx.stop()
                        sfx.play(R.raw.sfx_suscribe, loop = false, volume = 1f)
                        delay(2800)
                        sfx.stop()
                        onSuscribete()
                    }
                },
            contentScale = ContentScale.FillWidth
        )
    }
}


/* ================= helpers ================= */

@Composable
private fun RainbowHeadline(text: String, sizeSp: androidx.compose.ui.unit.TextUnit) {
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFFFFC107), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFFFF5722), Color(0xFF9C27B0)
    )
    val styled = buildAnnotatedString {
        withStyle(
            SpanStyle(
                brush = Brush.horizontalGradient(colors),
                fontWeight = FontWeight.ExtraBold
            )
        ) { append(text) }
    }
    Text(
        text = styled,
        fontSize = sizeSp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
    )
}

@Composable
private fun RainbowLabel(text: String) {
    val styled = buildAnnotatedString {
        withStyle(
            SpanStyle(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF5252), Color(0xFFFFC107),
                        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0)
                    )
                ),
                fontWeight = FontWeight.SemiBold
            )
        ) { append(text) }
    }
    Text(text = styled)
}

@Composable
private fun RainbowOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .border(
                BorderStroke(
                    5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF5252), Color(0xFFFFC107),
                            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0)
                        )
                    )
                ),
                shape = shape
            )
            .clip(shape)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { RainbowLabel(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.90f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.90f),
                disabledContainerColor = Color.White.copy(alpha = 0.90f),
                errorContainerColor = Color.White.copy(alpha = 0.90f),
                cursorColor = Color(0xFF1E88E5)
            )
        )
    }
}

private fun vibrateStrong(context: Context) {
    val vibr: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibr?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibr?.vibrate(80)
    }
}
