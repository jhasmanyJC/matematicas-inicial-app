package com.example.comnicomatincial.data


import com.example.comnicomatincial.R

object SumemosJugandoData {
    val level1 = listOf(
        // 1. 🍎 + 🍎 → 2 manzanas
        Triple(
            R.drawable.suma_manzanas_1_1, // imagen de la operación
            "dos",                        // respuesta esperada
            R.drawable.resultado_manzanas_2 // imagen con la respuesta final
        ),
        // 2. 🍌 + 🍌 + 🍌 → 3 plátanos
        Triple(
            R.drawable.suma_platanos_1_1_1,
            "tres",
            R.drawable.resultado_platanos_3
        ),
        // 3. 🍎🍎 + 🍎🍎 → 4 manzanas
        Triple(
            R.drawable.suma_manzanas_2_2,
            "cuatro",
            R.drawable.resultado_manzanas_4
        ),
        // 4. 🍌🍌🍌 + 🍌🍌 → 5 plátanos
        Triple(
            R.drawable.suma_platanos_3_2,
            "cinco",
            R.drawable.resultado_platanos_5
        )
    )

    // 🧮 Estructura del Nivel 2 (nueva)
    data class SumemosLevel2Item(
        val firstImage: Int,
        val secondImage: Int,
        val answerText: String,
        val resultImage: Int
    )

    val level2 = listOf(
        // 1️⃣ 3 + 3 = 6
        SumemosLevel2Item(
            R.drawable.robots3a,     // primera imagen (3 robots)
            R.drawable.robots3b,     // segunda imagen (otros 3 robots)
            "seis",                  // respuesta esperada
            R.drawable.result6       // imagen con el resultado
        ),

        // 2️⃣ 4 + 3 = 7
        SumemosLevel2Item(
            R.drawable.aviones4,     // primera imagen (4 aviones)
            R.drawable.aviones3,     // segunda imagen (3 aviones)
            "siete",                 // respuesta
            R.drawable.result7
        ),

        // 3️⃣ 4 + 4 = 8
        SumemosLevel2Item(
            R.drawable.autos4a,      // primera imagen
            R.drawable.autos4b,      // segunda imagen
            "ocho",
            R.drawable.result8
        ),

        // 4️⃣ 6 + 3 = 9
        SumemosLevel2Item(
            R.drawable.robots6,      // primera imagen
            R.drawable.robots3c,     // segunda imagen
            "nueve",
            R.drawable.result9
        ),

        // 5️⃣ 5 + 5 = 10
        SumemosLevel2Item(
            R.drawable.aviones5a,    // primera imagen
            R.drawable.aviones5b,    // segunda imagen
            "diez",
            R.drawable.result10
        )
    )
}
