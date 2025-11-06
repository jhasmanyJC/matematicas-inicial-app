package com.example.comnicomatincial.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.firebase.firestore.FirebaseFirestore
import com.example.comnicomatincial.data.FirestoreUploader

import kotlinx.coroutines.tasks.await
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

// === Librerías iTextG (compatibles con Android) ===
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.BaseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    gradeFilter: String,
    onBack: () -> Unit = {}
) {
    val firestore = FirebaseFirestore.getInstance()

    var allResults by remember { mutableStateOf(emptyList<Map<String, Any>>()) }
    var kidNames by remember { mutableStateOf(emptyList<String>()) }
    var selectedKid by remember { mutableStateOf<String?>(null) }
    var expandedKidMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // 🔹 NUEVO: modo de vista (individual / global)
    var showReports by remember { mutableStateOf(false) }
    var reports by remember { mutableStateOf(emptyList<Map<String, Any>>()) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // === Cargar resultados individuales desde Firestore ===
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val snapshot = firestore.collection("results").get().await()
            val data = snapshot.documents.mapNotNull { it.data }
            allResults = data

            val normalizedFilter = gradeFilter.lowercase(Locale.getDefault()).trim()
            kidNames = data
                .filter {
                    val gradeRaw = (it["grade"] as? String)?.lowercase(Locale.getDefault())?.trim() ?: ""
                    when {
                        normalizedFilter.contains("prek") ->
                            gradeRaw.startsWith("prek") || gradeRaw.startsWith("pre-k")
                        normalizedFilter.contains("kínder") || normalizedFilter.contains("kinder") ->
                            gradeRaw.startsWith("kínder") || gradeRaw.startsWith("kinder")
                        else -> false
                    }
                }
                .mapNotNull { it["kidName"] as? String }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            println("❌ Error Firestore: ${e.message}")
        }
        isLoading = false
    }

    // 🔹 Cargar reportes globales (solo si se activa el toggle)
    LaunchedEffect(showReports) {
        if (showReports) {
            isLoading = true
            try {
                val snapshot = firestore.collection("reports").get().await()
                val data = snapshot.documents.mapNotNull { it.data }

                // 🔸 Filtro por grado
                val normalizedFilter = gradeFilter.lowercase(Locale.getDefault()).trim()
                reports = data.filter {
                    val gradeRaw = (it["grade"] as? String)?.lowercase(Locale.getDefault())?.trim() ?: ""
                    when {
                        normalizedFilter.contains("prek") ->
                            gradeRaw.startsWith("prek") || gradeRaw.startsWith("pre-k")
                        normalizedFilter.contains("kínder") || normalizedFilter.contains("kinder") ->
                            gradeRaw.startsWith("kínder") || gradeRaw.startsWith("kinder")
                        else -> false
                    }
                }
            } catch (e: Exception) {
                println("❌ Error Firestore Reports: ${e.message}")
            }
            isLoading = false
        }
    }

    val normalizedGradeFilter = gradeFilter.lowercase(Locale.getDefault()).trim()

    val filteredResults by remember(allResults, selectedKid, normalizedGradeFilter) {
        derivedStateOf {
            allResults.filter { result ->
                val gradeRaw = (result["grade"] as? String)?.lowercase(Locale.getDefault())?.trim() ?: ""
                val kidRaw = (result["kidName"] as? String)?.lowercase(Locale.getDefault())?.trim() ?: ""

                val isSameGrade = when {
                    normalizedGradeFilter.contains("prek") -> gradeRaw.startsWith("prek")
                    normalizedGradeFilter.contains("kínder") || normalizedGradeFilter.contains("kinder") ->
                        gradeRaw.startsWith("kínder") || gradeRaw.startsWith("kinder")
                    else -> false
                }

                isSameGrade && (selectedKid == null || kidRaw == selectedKid!!.lowercase(Locale.getDefault()).trim())
            }
        }
    }

    val grouped = filteredResults
        .sortedByDescending { it["timestamp"] as? Long ?: 0L }
        .groupBy { sdf.format(Date((it["timestamp"] as? Long) ?: 0L)) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFBBDEFB), Color(0xFF90CAF9)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "📘 Resultados de $gradeFilter",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(color = Color(0x55000000), blurRadius = 8f)
                )
            )

            Spacer(Modifier.height(10.dp))

            // 🔹 Toggle de modo de vista
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showReports = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showReports) Color(0xFF1565C0) else Color(0xFF90CAF9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).padding(4.dp)
                ) {
                    Text("📄 Resultados individuales", color = Color.White, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = { showReports = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showReports) Color(0xFF1565C0) else Color(0xFF90CAF9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).padding(4.dp)
                ) {
                    Text("🏆 Reportes globales", color = Color.White, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!showReports) {
                // ===============================
                // 🔸 VISTA DE RESULTADOS INDIVIDUALES
                // ===============================
                val currentKidNames by rememberUpdatedState(newValue = kidNames)

                ExposedDropdownMenuBox(
                    expanded = expandedKidMenu,
                    onExpandedChange = { expandedKidMenu = !expandedKidMenu },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                ) {
                    OutlinedTextField(
                        value = selectedKid ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar niño") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKidMenu)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFF64B5F6),
                            focusedLabelColor = Color(0xFF1565C0)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedKidMenu,
                        onDismissRequest = { expandedKidMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("👦 Todos los niños") },
                            onClick = {
                                selectedKid = null
                                expandedKidMenu = false
                            }
                        )
                        currentKidNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text("👧 $name") },
                                onClick = {
                                    selectedKid = name
                                    expandedKidMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (selectedKid != null) {
                    val kidResults = filteredResults.filter {
                        (it["kidName"] as? String)?.equals(selectedKid, ignoreCase = true) == true
                    }
                    val avgScore = if (kidResults.isNotEmpty()) kidResults.mapNotNull {
                        (it["score"] as? Number)?.toInt()
                    }.average().toInt() else 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("👧 Niño: $selectedKid", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))

                            val actividadesUnicas = kidResults.mapNotNull { it["activityName"] as? String }.distinct().size
                            Text("📊 Actividades subidas: $actividadesUnicas", color = Color(0xFF2E7D32))

                            Text("⭐ Promedio de puntaje: $avgScore", color = Color(0xFFF57C00))
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (grouped.isEmpty()) {
                    Text(
                        "No hay resultados para mostrar.",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                        grouped.forEach { (fecha, lista) ->
                            item {
                                var expanded by remember { mutableStateOf(true) }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { expanded = !expanded },
                                    color = Color.Transparent
                                ) {
                                    Box(
                                        Modifier
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    listOf(Color(0xFF42A5F5), Color(0xFF64B5F6))
                                                )
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📅 $fecha",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Icon(
                                                imageVector = if (expanded)
                                                    Icons.Filled.KeyboardArrowUp
                                                else Icons.Filled.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = expanded) {
                                    // 🔹 Agrupamos por actividad + niño
                                    val actividadesAgrupadas = lista
                                        .groupBy { "${it["kidName"]}_${it["activityName"]}" }

                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.9f))
                                            .padding(8.dp)
                                    ) {
                                        actividadesAgrupadas.forEach { (_, actividadLista) ->
                                            val first = actividadLista.first()
                                            val kidName = first["kidName"] as? String ?: "Sin nombre"
                                            val activity = first["activityName"] as? String ?: "Sin actividad"
                                            val fechaActividad = actividadLista.maxOfOrNull {
                                                (it["timestamp"] as? Long) ?: 0L
                                            }?.let {
                                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                                            } ?: "-"

                                            // 🔸 Agrupamos por nivel dentro de la actividad
                                            val niveles = actividadLista.groupBy { (it["level"] ?: 0).toString() }

                                            Card(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text("👦 Niño: $kidName", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                                    Text("🎨 Actividad: $activity", color = Color(0xFF6A1B9A), fontWeight = FontWeight.SemiBold)
                                                    Spacer(Modifier.height(6.dp))

                                                    niveles.forEach { (nivel, intentosLista) ->
                                                        val promedio = intentosLista.mapNotNull {
                                                            (it["score"] as? Number)?.toDouble()
                                                        }.average().takeIf { !it.isNaN() } ?: 0.0

                                                        val totalAciertos = intentosLista.mapNotNull {
                                                            (it["correctAnswers"] as? Number)?.toInt()
                                                        }.average().takeIf { !it.isNaN() }?.toInt() ?: 0

                                                        val maxAciertos = intentosLista.maxOfOrNull {
                                                            (it["totalQuestions"] as? Number)?.toInt() ?: 5
                                                        } ?: 5

                                                        Text(
                                                            "🧩 Nivel $nivel → ${"%.0f".format(promedio)}%  ($totalAciertos/$maxAciertos)",
                                                            color = Color(0xFF2E7D32),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    Spacer(Modifier.height(4.dp))
                                                    Text("📅 Fecha: $fechaActividad", color = Color(0xFF5D4037))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }

            } else {
                // ===============================
                // 🔸 NUEVA VISTA: REPORTES GLOBALES
                // ===============================
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (reports.isEmpty()) {
                    Text(
                        "No hay reportes globales disponibles.",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    // 🔹 Mostrar resumen global
                    val eficienciaProm = reports.mapNotNull {
                        val progreso = it["progresoGlobal"] as? Map<*, *> ?: return@mapNotNull null
                        (progreso["eficienciaGlobal"] as? Number)?.toDouble()
                    }.average()

                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("📊 Promedio global del grado:", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("⭐ Eficiencia promedio: ${"%.1f".format(eficienciaProm)}%", color = Color(0xFFF57C00))
                            Text("👧 Niños con reportes: ${reports.size}", color = Color(0xFF2E7D32))
                        }
                    }

                    LazyColumn(Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                        items(reports.size) { index ->
                            val rep = reports[index]
                            val kidName = rep["kidName"] ?: "Sin nombre"
                            val estado = rep["estadoGeneral"] ?: "-"
                            val progreso = rep["progresoGlobal"] as? Map<*, *> ?: emptyMap<Any, Any>()
                            val eficiencia = progreso["eficienciaGlobal"] ?: "--"
                            val completadas = progreso["nivelesCompletados"] ?: "--"
                            val total = progreso["actividadesTotales"] ?: "--"

                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("👧 Niño: $kidName", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                    Text("⭐ Eficiencia: $eficiencia", color = Color(0xFFF57C00))
                                    Text("🏁 Completadas: $completadas / $total", color = Color(0xFF2E7D32))
                                    Text("📊 Estado: $estado", color = Color(0xFF6A1B9A))
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .width(180.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF1565C0))
                Spacer(Modifier.width(6.dp))
                Text("Volver al menú", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            }

            // 🔹 Botón para subir resultados a Firestore
            Button(
                onClick = {
                    FirestoreUploader.subirResultados(context)
                    Toast.makeText(context, "☁️ Subiendo resultados a Firestore...", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .width(220.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("☁️ Subir resultados", color = Color.White, fontWeight = FontWeight.Bold)
            }


            if (!showReports) {
                Button(
                    onClick = {
                        generateResultsPdf(
                            context = context,
                            results = filteredResults.filter {
                                selectedKid == null ||
                                        (it["kidName"] as? String)?.equals(selectedKid, ignoreCase = true) == true
                            },
                            grade = gradeFilter
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .width(200.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📄 Descargar PDF", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// === Generar PDF con campos adicionales ===
fun generateResultsPdf(
    context: Context,
    results: List<Map<String, Any>>,
    grade: String
) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Resultados_$grade.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        val outputStream: OutputStream? = uri?.let { resolver.openOutputStream(it) }

        if (outputStream == null) {
            Toast.makeText(context, "No se pudo crear el PDF", Toast.LENGTH_SHORT).show()
            return
        }

        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, outputStream)
        document.open()

        val titleFont = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BaseColor(33, 150, 243))
        document.add(Paragraph("ColorMagic 🎨", titleFont))
        document.add(Paragraph("Resultados de $grade", Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)))
        document.add(Paragraph("Fecha: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}"))
        document.add(Paragraph(" "))

        // === Tabla con columnas extendidas ===
        val table = PdfPTable(7)
        table.widthPercentage = 100f

        val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        val headerBg = BaseColor(33, 150, 243)
        val headers = listOf("Niño", "Actividad", "Nivel", "Intentos", "Puntaje", "Aciertos", "Fecha")

        headers.forEach {
            val cell = PdfPCell(Paragraph(it, headerFont))
            cell.backgroundColor = headerBg
            cell.horizontalAlignment = Element.ALIGN_CENTER
            table.addCell(cell)
        }

        val cellFont = Font(Font.FontFamily.HELVETICA, 10f)
        var alternate = false

        results.forEach { result ->
            val bgColor = if (alternate) BaseColor(227, 242, 253) else BaseColor(255, 255, 255)
            alternate = !alternate

            val kidName = result["kidName"] as? String ?: "-"
            val activity = result["activityName"] as? String ?: "-"
            val level = (result["level"] as? Number)?.toString() ?: "-"
            val attempts = result["attempts"]?.toString() ?: "-"
            val score = result["score"]?.toString() ?: "-"
            val correctAnswers = result["correctAnswers"]?.toString() ?: "0"
            val timestamp = (result["timestamp"] as? Long) ?: 0L
            val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

            listOf(kidName, activity, level, attempts, score, correctAnswers, dateFormatted).forEach {
                val cell = PdfPCell(Paragraph(it, cellFont))
                cell.backgroundColor = bgColor
                table.addCell(cell)
            }
        }

        document.add(table)
        document.add(
            Paragraph(
                "\nGenerado automáticamente por ColorMagic 🎨",
                Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC)
            )
        )

        document.close()
        outputStream.close()

        Toast.makeText(context, "✅ PDF guardado en Descargas", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "❌ Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
