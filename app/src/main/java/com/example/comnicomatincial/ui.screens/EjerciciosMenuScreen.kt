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
fun EjerciciosMenuScreen(navController: NavController, kidName: String) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFBBDEFB), Color(0xFFE3F2FD)))
            )
            .padding(16.dp)
    ) {

        Text(
            "🧩 Ejercicios",
            color = Color(0xFF0D47A1),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        LazyColumn {

            // 🟢 PANEL PREKÍNDER
            item {
                PanelTitulo("Prekínder")

                PanelBoton("Contemos juntos") {
                    navController.navigate("contamosNivel1/$kidName")
                }

                PanelBoton("Grande, mediano, pequeño") {
                    navController.navigate("tamaniosNivel1/$kidName")
                }

                PanelBoton("Formas y colores") {
                    navController.navigate("formasMagicas/$kidName")
                }

                PanelBoton("¿Cuántos faltan?") {
                    navController.navigate("faltanNivel1/$kidName")
                }

                PanelBoton("Arriba, abajo, al lado") {
                    navController.navigate("posicionesNivel1/$kidName")
                }
            }

            item { Spacer(Modifier.height(30.dp)) }

            // 🟣 PANEL KÍNDER
            item {
                PanelTitulo("Kínder")

                PanelBoton("Sumemos jugando") {
                    navController.navigate("sumemosNivel1/$kidName")
                }

                PanelBoton("Restemos jugando") {
                    navController.navigate("restemosNivel1/$kidName")
                }

                PanelBoton("El número que falta") {
                    navController.navigate("numeroFaltaNivel1/$kidName")
                }

                PanelBoton("Mayor y menor") {
                    navController.navigate("mayorMenorNivel1/$kidName")
                }

                PanelBoton("Posiciones en la carrera") {
                    navController.navigate("carreraPosicionesNivel1/$kidName")
                }
            }
        }
    }
}

// ----------- COMPONENTES REUSABLES -----------

@Composable
private fun PanelTitulo(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1976D2), Color(0xFF42A5F5))
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PanelBoton(text: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFE3F2FD),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = "🔹 $text",
            color = Color(0xFF0D47A1),
            fontSize = 18.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}
