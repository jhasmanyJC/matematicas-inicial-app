package com.example.comnicomatincial.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ===== helper de red (compatible con minSdk < 23) ===== */
private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    } else {
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo
        @Suppress("DEPRECATION")
        info != null && info.isConnectedOrConnecting
    }
}

@Composable
fun WelcomeScreen(
    onKid: () -> Unit = {},
    onTeacher: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Ajustes rápidos
    val LOGO_SIZE = 180.dp
    val SCRIM = Color(0x99000000)
    val BUTTON_CORNER = 20.dp
    val KID_BTN_WIDTH = 200.dp
    val TEACHER_BTN_WIDTH = 200.dp
    val PRESS_SCALE = 0.95f
    val TITLE_SIZE = 35.sp

    // Música de fondo según ciclo de vida
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> music.start(R.raw.bgm_bienvenida, volume = 0.25f)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fondo_bienvenida),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(SCRIM))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== TÍTULO 3D COLORIDO
            RainbowText3D(
                text = "UNIDAD EDUCATIVA MIXTO “CARACAS”",
                fontSizeSp = TITLE_SIZE,
                maxLines = 3
            )
            Spacer(Modifier.height(8.dp))

            // Logo (escudo)
            Image(
                painter = painterResource(R.drawable.escudo_colegio),
                contentDescription = "Escudo del colegio",
                modifier = Modifier.size(LOGO_SIZE),
                contentScale = ContentScale.Fit
            )

            // Subtítulo 3D bajo el logo
            Spacer(Modifier.height(10.dp))
            Puffy3DText(
                text = "¡BIENVENIDOS A MATEMÁTICAS INICIAL!",
                fontSizeSp = 35.sp
            )

            Spacer(Modifier.height(30.dp))

            // ------- SOY NIÑO/A
            val kidInteraction = remember { MutableInteractionSource() }
            val kidPressed by kidInteraction.collectIsPressedAsState()
            val kidScale by animateFloatAsState(if (kidPressed) PRESS_SCALE else 1f, label = "kidScale")

            Box(
                modifier = Modifier
                    .width(KID_BTN_WIDTH)
                    .shadow(if (kidPressed) 2.dp else 10.dp, RoundedCornerShape(BUTTON_CORNER))
                    .clip(RoundedCornerShape(BUTTON_CORNER))
                    .clickable(interactionSource = kidInteraction, indication = null) {
                        vibrateStrong(ctx)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            music.stop(); sfx.stop()
                            sfx.play(R.raw.sfx_nino, loop = false, volume = 1f)
                            delay(1400)
                            sfx.stop()
                            onKid()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.btn_soy_nino),
                    contentDescription = "Soy niño/niña",
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = kidScale; scaleY = kidScale },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(14.dp))

            // ------- SOY PROFESOR/A (verifica internet)
            val tInteraction = remember { MutableInteractionSource() }
            val tPressed by tInteraction.collectIsPressedAsState()
            val tScale by animateFloatAsState(if (tPressed) PRESS_SCALE else 1f, label = "teacherScale")

            Box(
                modifier = Modifier
                    .width(TEACHER_BTN_WIDTH)
                    .shadow(if (tPressed) 2.dp else 10.dp, RoundedCornerShape(BUTTON_CORNER))
                    .clip(RoundedCornerShape(BUTTON_CORNER))
                    .clickable(interactionSource = tInteraction, indication = null) {
                        vibrateStrong(ctx)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            music.stop(); sfx.stop()
                            sfx.play(R.raw.sfx_profesor, loop = false, volume = 1f)
                            delay(2800)
                            sfx.stop()

                            if (isOnline(ctx)) {
                                onTeacher()
                            } else {
                                Toast.makeText(ctx, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.btn_soy_profesor),
                    contentDescription = "Soy profesor/a",
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = tScale; scaleY = tScale },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/* ====== Textos 3D coloridos ====== */

// Título principal: cada letra con color distinto + sombra para efecto 3D
@Composable
private fun RainbowText3D(
    text: String,
    fontSizeSp: androidx.compose.ui.unit.TextUnit,
    maxLines: Int = 1
) {
    val palette = listOf(
        Color(0xFFFF5252), // rojo
        Color(0xFFFFA726), // naranja
        Color(0xFFFFEB3B), // amarillo
        Color(0xFF66BB6A), // verde
        Color(0xFF42A5F5), // azul
        Color(0xFFAB47BC)  // violeta
    )
    val styled = buildAnnotatedString {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') append(ch) else {
                withStyle(
                    SpanStyle(
                        color = palette[i % palette.size],
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.45f),
                            offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                            blurRadius = 12f
                        )
                    )
                ) { append(ch) }
            }
        }
    }
    Text(
        text = styled,
        fontSize = fontSizeSp,
        textAlign = TextAlign.Center,
        lineHeight = fontSizeSp * 1.1f,
        maxLines = maxLines
    )
}

// Subtítulo inflado/“puffy” con bordecito y luz
@Composable
private fun Puffy3DText(
    text: String,
    fontSizeSp: androidx.compose.ui.unit.TextUnit
) {
    // contorno
    val outline = Color(0xFF0B4D9C)
    val o = 1.4.dp

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = fontSizeSp, color = outline, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.offset(-o, -o))
        Text(text, fontSize = fontSizeSp, color = outline, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.offset( o, -o))
        Text(text, fontSize = fontSizeSp, color = outline, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.offset(-o,  o))
        Text(text, fontSize = fontSizeSp, color = outline, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.offset( o,  o))

        val rainbow = listOf(
            Color(0xFFE91E63), Color(0xFFFFC107),
            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0)
        )
        val styled = buildAnnotatedString {
            text.forEachIndexed { i, ch ->
                if (ch == ' ') append(ch) else {
                    withStyle(
                        SpanStyle(
                            color = rainbow[i % rainbow.size],
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.30f),
                                offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                                blurRadius = 10f
                            )
                        )
                    ) { append(ch) }
                }
            }
        }
        Text(
            text = styled,
            fontSize = fontSizeSp,
            textAlign = TextAlign.Center,
            lineHeight = fontSizeSp * 1.05f
        )
    }
}

/* Vibración */
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

