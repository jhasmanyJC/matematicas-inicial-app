package com.example.comnicomatincial

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.comnicomatincial.ui.screens.*
import com.example.comnicomatincial.ui.activities.ShapeMagicActivityScreen
import com.example.comnicomatincial.ui.activities.ShapeColorMagicLevel2Screen
import com.example.comnicomatincial.ui.activities.SizeComparisonActivityScreen
import com.example.comnicomatincial.ui.activities.SizeComparisonLevel2ActivityScreen
import com.example.comnicomatincial.ui.activities.SumemosJugandoLevel1ActivityScreen
import com.example.comnicomatincial.ui.activities.SumemosJugandoLevel2ActivityScreen
import com.example.comnicomatincial.ui.activities.SumaYSaltaLevel1ActivityScreen
import com.example.comnicomatincial.ui.activities.SumaYSaltaLevel2ActivityScreen
import com.example.comnicomatincial.ui.activities.CarreraNumerosLevel1ActivityScreen
import com.example.comnicomatincial.ui.activities.CarreraNumerosLevel2ActivityScreen
import com.example.comnicomatincial.ui.activities.DondeEstaNivel1ActivityScreen

import com.example.comnicomatincial.data.TeacherDatabase

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Verificar y solicitar permiso de micrófono si no está concedido
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }

        setContent {
            MaterialTheme {
                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = "welcome") {

                    // ---- Pantalla de Bienvenida
                    composable("welcome") {
                        WelcomeScreen(
                            onKid = {
                                nav.navigate("login") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onTeacher = { nav.navigate("teacherLogin") }
                        )
                    }

                    // ---- Login Niño
                    composable("login") {
                        LoginScreen(
                            onVamos = { kidName ->
                                nav.navigate("menu/$kidName") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onSuscribete = { nav.navigate("register") }
                        )
                    }

                    // ---- Registro Niño
                    composable("register") {
                        RegisterScreen(
                            onStart = { _, _ -> },
                            onRegistered = { nav.popBackStack() }
                        )
                    }

                    // ---- Login Profesor
                    composable("teacherLogin") {
                        TeacherLoginScreen(
                            onLogin = { email, pass ->
                                val db = TeacherDatabase(this@MainActivity)
                                val prof = db.getTeacherByEmail(email.trim())

                                if (prof != null && prof.password == pass.trim()) {
                                    val profName = prof.fullName
                                    val grade = prof.grade
                                    val firstName = profName.split(" ")[0]

                                    nav.navigate("teacherMenu/$profName/$email/$pass/$grade/$firstName") {
                                        popUpTo("teacherLogin") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Correo o contraseña incorrectos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onRegister = { nav.navigate("teacherRegister") }
                        )
                    }

                    // ---- Registro Profesor
                    composable("teacherRegister") {
                        TeacherRegisterScreen(
                            onStart = { fullName, email, password, grade ->
                                val db = TeacherDatabase(this@MainActivity)
                                db.insertTeacher(fullName, email, password, grade)
                            },
                            onRegistered = { nav.popBackStack() }
                        )
                    }

                    // ---- Menú del Niño
                    composable("menu/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        MenuScreen(
                            kidName = kidName,
                            onJuegos = {
                                nav.navigate("menuJuegos/$kidName")
                            },
                            onEjercicios = {
                                nav.navigate("menuEjercicios/$kidName")
                            },
                            onLogros = { /* Logros */ },
                            onRegistro = { /* Registro del niño */ },
                            onExit = {
                                nav.navigate("login") {
                                    popUpTo("menu/{kidName}") { inclusive = true }
                                }
                            },
                            onBack = {
                                nav.navigate("login") {
                                    popUpTo("menu/{kidName}") { inclusive = true }
                                }
                            }
                        )
                    }

                    // =====================================================
                    // 🧩 NUEVAS PANTALLAS DE MENÚ INTERMEDIO
                    // =====================================================

                    // ---- Menú de Ejercicios (Prekínder / Kínder)
                    composable("menuEjercicios/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        EjerciciosMenuScreen(navController = nav, kidName = kidName)
                    }

                    // ---- Menú de Juegos (Prekínder / Kínder)
                    composable("menuJuegos/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        JuegosMenuScreen(navController = nav, kidName = kidName)
                    }

                    // =====================================================
                    // 🧠 EJERCICIOS
                    // =====================================================

                    // ---- Nivel 1: Tamaños (Formas Mágicas)
                    composable("formasMagicas/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        ShapeMagicActivityScreen(kidName = kidName, navController = nav)
                    }

                    // ---- Nivel 2: Tamaños (Formas y Colores)
                    composable("formasMagicasNivel2/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        ShapeColorMagicLevel2Screen(kidName = kidName, navController = nav)
                    }

                    // ---- Nivel 1: Grande, mediano, pequeño
                    composable("tamaniosNivel1/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        SizeComparisonActivityScreen(kidName = kidName, navController = nav)
                    }

                    // ---- Nivel 2: Grande, mediano, pequeño
                    composable("tamaniosNivel2/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        SizeComparisonLevel2ActivityScreen(kidName = kidName, navController = nav)
                    }
                    //---- nivel 1: sumemeos juagndo------
                    composable("sumemosNivel1/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: ""
                        SumemosJugandoLevel1ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    //---- nivel 2: sumemeos juagndo------
                    composable("sumemosNivel2/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: ""
                        SumemosJugandoLevel2ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }




                    // =====================================================
                    // 🎮 JUEGOS
                    // =====================================================

                    // ---- Juego: Clasifiquemos Juguetes - Nivel 1
                    composable("clasifiquemosNivel1/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        com.example.comnicomatincial.ui.activities.ToyGroupingByColorActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    // ---- Juego: Clasificación por Tamaño - Nivel 2
                    composable("clasifiquemosNivel2/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        com.example.comnicomatincial.ui.activities.ToyGroupingBySizeActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    // ---- Juego: Suma y Salta - Nivel 1
                    composable("sumaYsaltaNivel1/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        SumaYSaltaLevel1ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    // ---- Juego: Suma y Salta - Nivel 2
                    composable("sumaYsaltaNivel2/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        SumaYSaltaLevel2ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    // ---- Juego: carrera de numeros - Nivel 1
                    composable("carreradenumerosNivel1/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        CarreraNumerosLevel1ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    // ---- Juego: carrera de numeros - Nivel 2
                    composable("carreradenumerosNivel2/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        CarreraNumerosLevel2ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }

                    // ---- Juego: ¿donde esta? - Nivel 1
                    composable("dondeEstaNivel1/{kidName}") { backStackEntry ->
                        val kidName = backStackEntry.arguments?.getString("kidName") ?: "Niño"
                        DondeEstaNivel1ActivityScreen(
                            kidName = kidName,
                            navController = nav
                        )
                    }



                    // =====================================================
                    // 👩‍🏫 PROFESOR
                    // =====================================================

                    // ---- Menú del Profesor
                    composable("teacherMenu/{profName}/{email}/{pass}/{grade}/{firstName}") { backStackEntry ->
                        val profName = backStackEntry.arguments?.getString("profName") ?: "Profesor"
                        val email = backStackEntry.arguments?.getString("email") ?: "-"
                        val grade = backStackEntry.arguments?.getString("grade") ?: "-"
                        val firstName = backStackEntry.arguments?.getString("firstName") ?: "Profesor"

                        val displayGrade = if (grade == "Ambos") {
                            "Prekínder y Kínder"
                        } else {
                            grade
                        }

                        TeacherMenuScreen(
                            profName = profName,
                            email = email,
                            grade = displayGrade,
                            firstName = firstName,
                            onPreKinder = { nav.navigate("results/Prekínder") },
                            onKinder = { nav.navigate("results/Kínder") },
                            onExit = {
                                nav.navigate("teacherLogin") {
                                    popUpTo("teacherMenu/{profName}/{email}/{pass}/{grade}/{firstName}") {
                                        inclusive = true
                                    }
                                }
                            },
                            onBack = {
                                nav.navigate("teacherLogin") {
                                    popUpTo("teacherMenu/{profName}/{email}/{pass}/{grade}/{firstName}") {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }

                    // ---- Pantalla de Resultados (Profesor)
                    composable("results/{grade}") { backStackEntry ->
                        val grade = backStackEntry.arguments?.getString("grade") ?: "Prekínder"
                        ResultsScreen(
                            gradeFilter = grade,
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
