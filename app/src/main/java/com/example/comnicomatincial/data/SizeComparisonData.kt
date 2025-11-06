package com.example.comnicomatincial.data

import com.example.comnicomatincial.R

/**
 * 🔹 Dataset general para las actividades de comparación de tamaños.
 * Incluye:
 *  - Nivel 1: Comparación de dos tamaños (grande / pequeño)
 *  - Nivel 2: Comparación de tres tamaños (grande / mediano / pequeño)
 *
 * Este objeto centraliza todas las imágenes, palabras clave y respuestas esperadas
 * para los ejercicios de pensamiento lógico-matemático de Tamaños.
 */
object SizeComparisonData {

    // -------------------------------------------------------------------------
    // 🟢 NIVEL 1 – Dos tamaños (grande / pequeño)
    // -------------------------------------------------------------------------
    val level1: List<Triple<Int, String, List<String>>> = listOf(
        Triple(R.drawable.img_perro_jirafa, "pequeño", listOf("perro", "perrito")),
        Triple(R.drawable.img_elefante_perro, "pequeño", listOf("elefante")),
        Triple(R.drawable.img_leon_pinguino, "grande", listOf("leon")),
        Triple(R.drawable.img_gato_pez, "grande", listOf("gato", "gatito")),
        Triple(R.drawable.img_oso_perro, "grande", listOf("oso")),
        Triple(R.drawable.img_jirafa_perro, "grande", listOf("jirafa")),
        Triple(R.drawable.img_leon_pinguino2, "pequeño", listOf("pinguino"))
    )

    // -------------------------------------------------------------------------
    // 🟠 NIVEL 2 – Tres tamaños (grande / mediano / pequeño)
    // -------------------------------------------------------------------------
    val level2: List<Triple<Int, Map<String, List<String>>, Unit>> = listOf(
        // 🦒🐶🦜 Imagen 1: Jirafa grande, perro mediano, loro pequeño
        Triple(
            R.drawable.img_jirafa_perro_loro,
            mapOf(
                "grande" to listOf("jirafa"),
                "mediano" to listOf("perro", "perrito"),
                "pequeño" to listOf("loro")
            ),
            Unit
        ),

        // 🚢🧸🚗 Imagen 2: Barco grande, muñeco mediano, auto pequeño
        Triple(
            R.drawable.img_barco_muneco_auto,
            mapOf(
                "grande" to listOf("barco"),
                "mediano" to listOf("muñeco", "niño", "muñeca"),
                "pequeño" to listOf("auto", "carro")
            ),
            Unit
        ),

        // ✈️🚗🤖 Imagen 3: Avión grande, auto mediano, robot pequeño
        Triple(
            R.drawable.img_avion_auto_robot,
            mapOf(
                "grande" to listOf("avion"),
                "mediano" to listOf("auto", "carro"),
                "pequeño" to listOf("robot")
            ),
            Unit
        ),

        // 🤡✈️🤖 Imagen 4: Payaso grande, avión mediano, robot pequeño
        Triple(
            R.drawable.img_payaso_avion_robot,
            mapOf(
                "grande" to listOf("payaso"),
                "mediano" to listOf("avion"),
                "pequeño" to listOf("robot")
            ),
            Unit
        ),

        // 🤖👧🚗 Imagen 5: Robot grande, muñeca mediana, auto pequeño
        Triple(
            R.drawable.img_robot_muneca_auto,
            mapOf(
                "grande" to listOf("robot"),
                "mediano" to listOf("muñeca", "niña"),
                "pequeño" to listOf("auto", "carro")
            ),
            Unit
        ),

        // 🍌🍇🍍 Imagen 6: Plátano grande, uva mediana, piña pequeña
        Triple(
            R.drawable.img_platano_uva_pina,
            mapOf(
                "grande" to listOf("platano", "banana"),
                "mediano" to listOf("uva"),
                "pequeño" to listOf("piña")
            ),
            Unit
        ),

        // ✈️🚗🤖 Imagen 7: Avión grande, auto mediano, robot pequeño (variante)
        Triple(
            R.drawable.img_avion_auto_robot2,
            mapOf(
                "grande" to listOf("avion"),
                "mediano" to listOf("auto", "carro"),
                "pequeño" to listOf("robot")
            ),
            Unit
        ),

        // 🍌🍇🍌 Imagen 8: Plátano grande, uva mediana, piña pequeña (repetición)
        Triple(
            R.drawable.img_platano_uva_platano,
            mapOf(
                "grande" to listOf("platano", "banana"),
                "mediano" to listOf("uva"),
                "pequeño" to listOf("piña")
            ),
            Unit
        )
    )
}
