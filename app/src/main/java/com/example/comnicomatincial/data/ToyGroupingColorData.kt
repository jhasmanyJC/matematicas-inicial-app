package com.example.comnicomatincial.data

import com.example.comnicomatincial.R

// 🔹 Contiene las imágenes compuestas por ronda con color objetivo y juguetes esperados
object ToyGroupingColorData {
    val level1 = listOf(
        // 🟦 Imagen 1: juguetes azules
        Triple(
            R.drawable.juguetes_azul,  // nombre de tu imagen en res/drawable
            "azul",
            listOf("muñeca", "dado", "auto")
        ),

        // 🟩 Imagen 2: juguetes verdes
        Triple(
            R.drawable.juguetes_verde,
            "verde",
            listOf("auto", "dado", "tren")
        ),

        // 🟪 Imagen 3: juguetes violetas
        Triple(
            R.drawable.juguetes_violeta,
            "violeta",
            listOf("oso", "avion")
        ),

        // 🟧 Imagen 4: juguetes naranjas (una pelota)
        Triple(
            R.drawable.juguetes_naranja,
            "naranja",
            listOf("pelota")
        ),

        // 🔴 Imagen 5: juguetes rojos
        Triple(
            R.drawable.juguetes_rojo,
            "rojo",
            listOf("robot", "avion", "auto", "dado")
        ),

        // 🟠 Imagen 6: juguetes naranjas (varios)
        Triple(
            R.drawable.juguetes_naranja2,
            "naranja",
            listOf("avion", "robot", "dado")
        )
    )
}
