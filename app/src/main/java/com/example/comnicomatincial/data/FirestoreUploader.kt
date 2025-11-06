package com.example.comnicomatincial.data

import android.content.Context
import android.widget.Toast
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object FirestoreUploader {

    /**
     * 🔹 Sube todos los resultados locales (SQLite) a Firestore.
     * Lee ResultsDatabase y KidDatabase, luego sube a la colección "results".
     * Al finalizar, genera:
     *   - Reportes globales (para vista general)
     *   - Reporte individual resumido (para el perfil del profesor)
     * y finalmente limpia la base de datos local.
     */
    fun subirResultados(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val firestore = FirebaseFirestore.getInstance()
            val resultsDb = ResultsDatabase(context)
            val kidsDb = KidDatabase(context)

            try {
                // 1️⃣ Leer todos los resultados locales
                val resultadosLocales = resultsDb.getAllResults()
                Log.i("FIRESTORE_TRACK", "📊 Total resultados locales a subir: ${resultadosLocales.size}")

                if (resultadosLocales.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No hay resultados locales para subir.", Toast.LENGTH_SHORT).show()
                    }
                    Log.w("FIRESTORE_TRACK", "⚠️ No hay resultados locales. Cancelando subida.")
                    return@launch
                }

                // 2️⃣ Subir cada resultado individual
                for ((index, res) in resultadosLocales.withIndex()) {
                    val kidInfo = kidsDb.getKidByName(res.kidName)
                    Log.d(
                        "FIRESTORE_TRACK",
                        "⬆ [${index + 1}/${resultadosLocales.size}] Subiendo → kid=${res.kidName}, activity=${res.activityName}, level=${res.level}, score=${res.score}, attempts=${res.attempts}, correctAnswers=${res.correctAnswers}"
                    )

                    val data = mapOf(
                        "kidName" to res.kidName,
                        "activityName" to res.activityName,
                        "level" to res.level,
                        "attempts" to res.attempts,
                        "score" to res.score,
                        "correctAnswers" to res.correctAnswers,
                        "timestamp" to res.timestamp,
                        "grade" to (kidInfo?.grade ?: "Desconocido"),
                        "dadName" to (kidInfo?.dadName ?: "-"),
                        "momName" to (kidInfo?.momName ?: "-")
                    )

                    firestore.collection("results").add(data).await()
                    Log.i("FIRESTORE_TRACK", "✅ Subido correctamente: ${res.kidName} (${res.activityName}) nivel=${res.level}")
                }

                // 3️⃣ Generar los reportes globales
                withContext(Dispatchers.Main) {
                    Log.i("FIRESTORE_TRACK", "🧮 Iniciando generación de reportes globales...")
                    Toast.makeText(context, "Subiendo reportes globales...", Toast.LENGTH_SHORT).show()
                }
                ReportsUploader.generarReportesGlobales()
                Log.i("FIRESTORE_TRACK", "✅ Reportes globales generados correctamente.")

                // 3️⃣.5️⃣ 🔹 Generar y subir reporte individual resumido (para el profesor)
                val resumenIndividual = generarResumenIndividual(resultadosLocales)
                if (resumenIndividual.isNotEmpty()) {
                    val kidName = resultadosLocales.first().kidName
                    firestore.collection("reports_individuales")
                        .document(kidName)
                        .set(resumenIndividual)
                        .addOnSuccessListener {
                            Log.i("FIRESTORE_TRACK", "✅ Reporte individual subido correctamente para $kidName")
                        }
                        .addOnFailureListener {
                            Log.e("FIRESTORE_TRACK", "❌ Error al subir reporte individual: ${it.message}")
                        }
                }

                // 4️⃣ 🧹 Limpiar los resultados locales ya subidos
                withContext(Dispatchers.IO) {
                    Log.w("FIRESTORE_TRACK", "🧽 Limpiando base de datos local (results)...")
                    resultsDb.clearAllResults()
                }

                // 5️⃣ Confirmación final
                withContext(Dispatchers.Main) {
                    Log.i("FIRESTORE_TRACK", "🎉 Subida y limpieza completadas sin errores.")
                    Toast.makeText(context, "✅ Resultados subidos y limpiados correctamente.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FIRESTORE_TRACK", "❌ Error al subir resultados: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Error al subir: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ==============================
    // 🧩 Generador de resumen individual (por niño)
    // ==============================
    private fun generarResumenIndividual(
        results: List<ActivityResult>
    ): Map<String, Any> {
        if (results.isEmpty()) return emptyMap()

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Agrupar por actividad y nivel
        val agrupado = results.groupBy { it.activityName }.mapValues { (_, listaActividad) ->
            val porNivel = listaActividad.groupBy { it.level }.mapValues { (_, listaNivel) ->
                val intentosTotales = listaNivel.sumOf { it.attempts }
                val mejorPuntaje = listaNivel.maxOfOrNull { it.score } ?: 0
                val aciertosProm = listaNivel.map { it.correctAnswers }.average().toInt()
                val totalPreguntas = 5 // ajusta según tu app
                val fecha = sdf.format(Date(listaNivel.maxOf { it.timestamp }))

                mapOf(
                    "nivel" to listaNivel.first().level,
                    "intentosTotales" to intentosTotales,
                    "mejorPuntaje" to mejorPuntaje,
                    "aciertos" to "$aciertosProm/$totalPreguntas",
                    "fecha" to fecha
                )
            }
            porNivel.values.sortedBy { (it["nivel"] as Int) }
        }

        // Calcular resumen global
        val actividadesSubidas = agrupado.size
        val promedioGlobal = results.map { it.score }.average().toInt()

        return mapOf(
            "actividadesSubidas" to actividadesSubidas,
            "promedioGlobal" to promedioGlobal,
            "detalle" to agrupado,
            "timestamp" to System.currentTimeMillis()
        )
    }
}
