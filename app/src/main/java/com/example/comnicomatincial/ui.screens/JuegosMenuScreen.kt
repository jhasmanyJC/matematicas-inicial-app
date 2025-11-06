package com.example.comnicomatincial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun JuegosMenuScreen(navController: NavController, kidName: String) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFFDE7)))
            )
            .padding(16.dp)
    ) {
        Text(
            "🎮 Juegos",
            color = Color(0xFFF57F17),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        LazyColumn {
            // 🟢 PANEL PREKÍNDER
            item {
                PanelTituloJuegos("Prekínder")

                PanelBotonJuegos("Atrapa el número") {
                    navController.navigate("atrapaNumeroNivel1/$kidName")
                }

                PanelBotonJuegos("Clasifiquemos juguetes") {
                    navController.navigate("clasifiquemosNivel1/$kidName")
                }

                PanelBotonJuegos("Sigue la secuencia") {
                    navController.navigate("secuenciaNivel1/$kidName")
                }

                PanelBotonJuegos("¿Dónde está?") {
                    navController.navigate("dondeEstaNivel1/$kidName")
                }

                PanelBotonJuegos("Cuenta los animales") {
                    navController.navigate("cuentaAnimalesNivel1/$kidName")
                }
            }

            item { Spacer(Modifier.height(30.dp)) }

            // 🟣 PANEL KÍNDER
            item {
                PanelTituloJuegos("Kínder")

                PanelBotonJuegos("Carrera de números") {
                    navController.navigate("carreradenumerosNivel1/$kidName")
                }

                PanelBotonJuegos("Suma y salta") {
                    navController.navigate("sumaYsaltaNivel1/$kidName")
                }

                PanelBotonJuegos("Encuentra el número") {
                    navController.navigate("encuentraNumeroNivel1/$kidName")
                }

                PanelBotonJuegos("Rompecabeza numérico") {
                    navController.navigate("rompecabezaNivel1/$kidName")
                }

                PanelBotonJuegos("Construye con números") {
                    navController.navigate("construyeNumerosNivel1/$kidName")
                }
            }
        }
    }
}

// ----------- COMPONENTES REUSABLES -----------

@Composable
private fun PanelTituloJuegos(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFF9A825), Color(0xFFFFD54F))
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PanelBotonJuegos(text: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFF9C4),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = "🎲 $text",
            color = Color(0xFFF57F17),
            fontSize = 18.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}
