package com.example.comnicomatincial.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object TLOGIN_STYLE {
    val TITLE_COLOR = Color(0xFFEBEFF6)
    val TITLE_SIZE = 36.sp
    val FIELD_LABEL_COLOR = Color(0xFF1565C0)
    val FIELD_TEXT_COLOR = Color.Black
    val RESET_TEXT_COLOR = Color(0xFF1976D2)
    val DIALOG_ICON_SUCCESS = Color(0xFF2E7D32)
    val DIALOG_ICON_ERROR = Color(0xFFC62828)
}

@Composable
fun TeacherLoginScreen(
    onLogin: (email: String, pass: String) -> Unit = { _, _ -> },
    onRegister: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val music = remember { MusicPlayer(ctx) }
    val sfx = remember { SfxPlayer(ctx) }
    val auth = FirebaseAuth.getInstance()

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
            music.stop(); sfx.stop()
        }
    }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val listo = email.isNotBlank() && password.isNotBlank()

    // 🔹 Estados para diálogo visual
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccessDialog by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fondo_login_pro),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Color(0x99000000)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(R.drawable.logo_login_pro),
                contentDescription = "Logo Profesor",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "INICIAR SESIÓN",
                fontSize = TLOGIN_STYLE.TITLE_SIZE,
                color = TLOGIN_STYLE.TITLE_COLOR,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.panel_fondo_pro),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.85f)))

                Column(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RainbowOutlinedField(
                        value = email,
                        onValueChange = { email = it },
                        label = "CORREO ELECTRÓNICO",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    RainbowOutlinedField(
                        value = password,
                        onValueChange = { password = it },
                        label = "CONTRASEÑA",
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))

                    // 🔹 Enviar correo de recuperación con diálogo animado
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        fontSize = 14.sp,
                        color = TLOGIN_STYLE.RESET_TEXT_COLOR,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                if (email.isBlank()) {
                                    isSuccessDialog = false
                                    dialogMessage = "Por favor ingresa tu correo electrónico antes de solicitar el restablecimiento."
                                    showDialog = true
                                } else {
                                    auth.sendPasswordResetEmail(email.trim())
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                isSuccessDialog = true
                                                dialogMessage = "📬 Se ha enviado un correo de recuperación a:\n$email\n\nRevisa tu bandeja de entrada o la carpeta de spam."
                                            } else {
                                                isSuccessDialog = false
                                                dialogMessage = "❌ Ocurrió un error al enviar el correo.\nVerifica que la dirección sea correcta."
                                            }
                                            showDialog = true
                                        }
                                }
                            }
                            .padding(vertical = 6.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // 🔹 Botón INGRESAR
                    val interactionIngresar = remember { MutableInteractionSource() }
                    val pressedIngresar by interactionIngresar.collectIsPressedAsState()
                    val scaleIngresar by animateFloatAsState(
                        if (pressedIngresar) 0.9f else 1f, label = "ingresarScale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(220.dp)
                            .height(130.dp)
                            .scale(scaleIngresar)
                            .clickable(interactionSource = interactionIngresar, indication = null) {
                                vibrateStrong(ctx)
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                )
                                music.stop()
                                scope.launch {
                                    sfx.stop()
                                    sfx.play(R.raw.sfx_vamos, loop = false, volume = 1f)
                                    delay(2800)
                                    sfx.stop()

                                    if (listo) {
                                        auth.signInWithEmailAndPassword(email.trim(), password.trim())
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val user = auth.currentUser
                                                    if (user != null && user.isEmailVerified) {
                                                        val teacherDatabase = TeacherDatabase(ctx)
                                                        teacherDatabase.updatePassword(email.trim(), password.trim())
                                                        onLogin(email.trim(), password.trim())
                                                    } else {
                                                        isSuccessDialog = false
                                                        dialogMessage = "Por favor verifica tu correo antes de iniciar sesión."
                                                        showDialog = true
                                                    }
                                                } else {
                                                    isSuccessDialog = false
                                                    dialogMessage = "Correo o contraseña incorrectos."
                                                    showDialog = true
                                                }
                                            }
                                    } else {
                                        isSuccessDialog = false
                                        dialogMessage = "Completa todos los campos para iniciar sesión."
                                        showDialog = true
                                    }
                                }
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.btn_ingresar_pro),
                            contentDescription = "Ingresar",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            // 🔹 Botón REGISTRARME
            val interactionRegister = remember { MutableInteractionSource() }
            val pressedRegister by interactionRegister.collectIsPressedAsState()
            val scaleRegister by animateFloatAsState(
                if (pressedRegister) 1.1f else 1f, label = "regScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(230.dp)
                    .height(150.dp)
                    .scale(scaleRegister)
                    .clickable(interactionSource = interactionRegister, indication = null) {
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
                            onRegister()
                        }
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.btn_registrate_pro),
                    contentDescription = "Registrarse",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        // 🔹 Diálogo con ícono animado
        AnimatedVisibility(showDialog, enter = fadeIn(), exit = fadeOut()) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Aceptar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Ícono redondo animado
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSuccessDialog)
                                        TLOGIN_STYLE.DIALOG_ICON_SUCCESS.copy(alpha = 0.15f)
                                    else
                                        TLOGIN_STYLE.DIALOG_ICON_ERROR.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isSuccessDialog) "📬" else "❌",
                                fontSize = 36.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isSuccessDialog) "Correo enviado" else "Error",
                            color = if (isSuccessDialog)
                                TLOGIN_STYLE.DIALOG_ICON_SUCCESS
                            else
                                TLOGIN_STYLE.DIALOG_ICON_ERROR,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Text(
                        dialogMessage,
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(25.dp)
            )
        }
    }
}

/* ====== CAMPO DE TEXTO ====== */
@Composable
private fun RainbowOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier.border(
            BorderStroke(4.dp, Brush.horizontalGradient(listOf(Color(0xFF1976D2), Color(0xFF64B5F6)))),
            shape = shape
        ).clip(shape)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { Text(label, color = TLOGIN_STYLE.FIELD_LABEL_COLOR, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            visualTransformation = visualTransformation,
            colors = TextFieldDefaults.colors(
                focusedTextColor = TLOGIN_STYLE.FIELD_TEXT_COLOR,
                unfocusedTextColor = TLOGIN_STYLE.FIELD_TEXT_COLOR,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

/* ====== Vibración ====== */
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
