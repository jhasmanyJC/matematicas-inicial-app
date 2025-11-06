package com.example.comnicomatincial.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// 🟢 Modelo de datos actualizado con campo correctAnswers y timestamp
data class ActivityResult(
    val id: Int = 0,
    val kidName: String,
    val activityName: String,
    val level: Int,
    val attempts: Int,
    val score: Int,
    val correctAnswers: Int = 0, // 🟢 nuevo campo opcional
    val totalQuestions: Int = 5, // 🟢 Nuevo campo
    val timestamp: Long = System.currentTimeMillis()
)

class ResultsDatabase(context: Context) :
    SQLiteOpenHelper(context, "results.db", null, 3) { // 🟢 versión 3 (nueva columna correctAnswers)

    override fun onCreate(db: SQLiteDatabase?) {
        Log.i("RESULTS_DB", "📦 Creando base de datos local 'results.db'...")
        db?.execSQL(
            """
            CREATE TABLE results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kidName TEXT,
                activityName TEXT,
                level INTEGER,
                attempts INTEGER,
                score INTEGER,
                correctAnswers INTEGER,  -- 🟢 nueva columna
                timestamp INTEGER
            )
            """.trimIndent()
        )
        Log.i("RESULTS_DB", "✅ Tabla 'results' creada correctamente.")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.i("RESULTS_DB", "⚙️ Actualizando base de datos de versión $oldVersion a $newVersion...")
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE results ADD COLUMN timestamp INTEGER DEFAULT 0")
            Log.i("RESULTS_DB", "🟢 Columna 'timestamp' añadida.")
        }
        if (oldVersion < 3) {
            db?.execSQL("ALTER TABLE results ADD COLUMN correctAnswers INTEGER DEFAULT 0")
            Log.i("RESULTS_DB", "🟢 Columna 'correctAnswers' añadida.")
        }
    }

    // 🟢 Inserta un nuevo resultado en la base de datos
    fun insertResult(result: ActivityResult) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("kidName", result.kidName)
            put("activityName", result.activityName)
            put("level", result.level)
            put("attempts", result.attempts)
            put("score", result.score)
            put("correctAnswers", result.correctAnswers)
            put("timestamp", result.timestamp)
        }
        val id = db.insert("results", null, values)
        Log.i("RESULTS_DB", "💾 Insertado resultado ID=$id | kid=${result.kidName} | actividad=${result.activityName} | nivel=${result.level} | score=${result.score}")
        db.close()
    }

    // 🟢 Obtiene los resultados de un niño específico
    fun getResultsByKid(kidName: String): List<ActivityResult> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM results WHERE kidName=?", arrayOf(kidName))
        val list = mutableListOf<ActivityResult>()
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    ActivityResult(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        kidName = cursor.getString(cursor.getColumnIndexOrThrow("kidName")),
                        activityName = cursor.getString(cursor.getColumnIndexOrThrow("activityName")),
                        level = cursor.getInt(cursor.getColumnIndexOrThrow("level")),
                        attempts = cursor.getInt(cursor.getColumnIndexOrThrow("attempts")),
                        score = cursor.getInt(cursor.getColumnIndexOrThrow("score")),
                        correctAnswers = cursor.getInt(cursor.getColumnIndexOrThrow("correctAnswers")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            } while (cursor.moveToNext())
        }
        Log.i("RESULTS_DB", "📥 Cargados ${list.size} resultados locales del niño '$kidName'")
        cursor.close()
        db.close()
        return list
    }

    // 🟢 Obtiene todos los resultados de la base de datos
    fun getAllResults(): List<ActivityResult> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM results", null)
        val list = mutableListOf<ActivityResult>()
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    ActivityResult(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        kidName = cursor.getString(cursor.getColumnIndexOrThrow("kidName")),
                        activityName = cursor.getString(cursor.getColumnIndexOrThrow("activityName")),
                        level = cursor.getInt(cursor.getColumnIndexOrThrow("level")),
                        attempts = cursor.getInt(cursor.getColumnIndexOrThrow("attempts")),
                        score = cursor.getInt(cursor.getColumnIndexOrThrow("score")),
                        correctAnswers = cursor.getInt(cursor.getColumnIndexOrThrow("correctAnswers")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            } while (cursor.moveToNext())
        }
        Log.i("RESULTS_DB", "📋 Total de resultados en BD local: ${list.size}")
        cursor.close()
        db.close()
        return list
    }

    // 🧹 Elimina todos los resultados locales después de subirlos a Firestore
    fun clearAllResults() {
        val db = writableDatabase
        val rows = db.delete("results", null, null)
        Log.w("RESULTS_DB", "🧹 Base de datos limpiada: $rows registros eliminados.")
        db.close()
    }
}
