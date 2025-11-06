package com.example.comnicomatincial.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Kid(
    val id: Int = 0,
    val kidName: String,
    val dadName: String,
    val momName: String,
    val grade: String
)

class KidDatabase(context: Context) : SQLiteOpenHelper(context, "kids.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE kids (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "kidName TEXT, " +
                    "dadName TEXT, " +
                    "momName TEXT, " +
                    "grade TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS kids")
        onCreate(db)
    }

    fun insertKid(kid: Kid) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("kidName", kid.kidName.trim())
            put("dadName", kid.dadName.trim())
            put("momName", kid.momName.trim())
            put("grade", kid.grade.trim())
        }
        db.insert("kids", null, values)
        db.close()
    }

    fun getKids(): List<Kid> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM kids", null)
        val kids = mutableListOf<Kid>()

        if (cursor.moveToFirst()) {
            do {
                kids.add(
                    Kid(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        kidName = cursor.getString(cursor.getColumnIndexOrThrow("kidName")),
                        dadName = cursor.getString(cursor.getColumnIndexOrThrow("dadName")),
                        momName = cursor.getString(cursor.getColumnIndexOrThrow("momName")),
                        grade = cursor.getString(cursor.getColumnIndexOrThrow("grade"))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return kids
    }

    // 🔎 Obtener un niño por nombre exacto
    fun getKidByName(name: String): Kid? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM kids WHERE LOWER(TRIM(kidName)) = ? LIMIT 1",
            arrayOf(name.trim().lowercase())
        )
        var kid: Kid? = null
        if (cursor.moveToFirst()) {
            kid = Kid(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                kidName = cursor.getString(cursor.getColumnIndexOrThrow("kidName")),
                dadName = cursor.getString(cursor.getColumnIndexOrThrow("dadName")),
                momName = cursor.getString(cursor.getColumnIndexOrThrow("momName")),
                grade = cursor.getString(cursor.getColumnIndexOrThrow("grade"))
            )
        }
        cursor.close()
        db.close()
        return kid
    }
}
