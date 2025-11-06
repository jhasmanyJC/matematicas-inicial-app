package com.example.comnicomatincial.data


import com.example.comnicomatincial.R

// 🔹 Dataset de juguetes grandes y pequeños para el Nivel 2
object ToyGroupingSizeData {
    val level2 = listOf(
        // 1️⃣ Primera imagen → auto pequeño
        Triple(R.drawable.juguetes_tam_1, "pequeño", listOf("auto")),

        // 2️⃣ Segunda imagen → oso grande y auto grande
        Triple(R.drawable.juguetes_tam_2, "grande", listOf("oso", "auto")),

        // 3️⃣ Tercera imagen → elefante grande, robot grande y oso grande
        Triple(R.drawable.juguetes_tam_3, "grande", listOf("elefante", "robot", "oso")),

        // 4️⃣ Cuarta imagen → auto pequeño, soldado pequeño y robot pequeño
        Triple(R.drawable.juguetes_tam_4, "pequeño", listOf("auto", "soldado", "robot")),

        // 5️⃣ Quinta imagen → elefante grande, oso grande, robot grande y soldado grande
        Triple(R.drawable.juguetes_tam_5, "grande", listOf("elefante", "oso", "robot", "soldado")),

        // 6️⃣ Sexta imagen → elefante pequeño, oso pequeño, robot pequeño, soldado pequeño y auto pequeño
        Triple(R.drawable.juguetes_tam_6, "pequeño", listOf("elefante", "oso", "robot", "soldado", "auto")),

        // 7️⃣ Séptima imagen → oso grande y auto grande
        Triple(R.drawable.juguetes_tam_7, "grande", listOf("oso", "auto"))
    )
}
