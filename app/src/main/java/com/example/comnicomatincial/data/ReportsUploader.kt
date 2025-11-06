package com.example.comnicomatincial.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * ✅ Esta clase genera o actualiza un reporte global en Firestore
 * basado en los resultados que ya existen en la colección "results".
 *
 * Colección destino: "reports"
 */
object ReportsUploader {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun generarReportesGlobales() {
        try {
            // Obtenemos todos los resultados existentes
            val snapshot = firestore.collection("results").get().await()
            val resultados = snapshot.documents.mapNotNull { it.data }

            // Agrupamos los resultados por niño
            val resultadosPorNiño = resultados.groupBy { it["kidName"] as? String ?: "Sin nombre" }

            for ((kidName, lista) in resultadosPorNiño) {
                if (lista.isEmpty()) continue

                // Tomamos grado (asumimos que es el mismo para ese niño)
                val grade = lista.firstOrNull()?.get("grade") as? String ?: "Desconocido"

                // Calcular datos globales
                val totalActividades = lista.size
                val nivelesCompletados = lista.mapNotNull { it["level"] as? Number }.distinct().count()
                val promedioEficiencia = lista.mapNotNull { (it["score"] as? Number)?.toDouble() }.average()
                val eficienciaGlobal = if (promedioEficiencia.isNaN()) 0 else promedioEficiencia.toInt()

                val estadoGeneral = when {
                    eficienciaGlobal >= 90 -> "Excelente"
                    eficienciaGlobal >= 75 -> "Muy bien"
                    eficienciaGlobal >= 50 -> "En progreso"
                    else -> "Necesita apoyo"
                }

                // Documento que se subirá/actualizará en Firestore
                val reporteGlobal = mapOf(
                    "kidName" to kidName,
                    "grade" to grade,
                    "progresoGlobal" to mapOf(
                        "eficienciaGlobal" to eficienciaGlobal,
                        "nivelesCompletados" to nivelesCompletados,
                        "actividadesTotales" to totalActividades
                    ),
                    "estadoGeneral" to estadoGeneral,
                    "timestamp" to System.currentTimeMillis()
                )

                // Guardar o actualizar documento
                firestore.collection("reports").document(kidName).set(reporteGlobal).await()
            }

            println("✅ Reportes globales actualizados correctamente en Firestore.")

        } catch (e: Exception) {
            println("❌ Error al generar reportes globales: ${e.message}")
        }
    }
}
