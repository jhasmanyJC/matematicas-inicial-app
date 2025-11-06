package com.example.comnicomatincial.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class Teacher(
    val id: Int = 0,
    val fullName: String,
    val email: String,
    val password: String,
    val grade: String
)

class TeacherDatabase(context: Context) : SQLiteOpenHelper(context, "teachers.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE teachers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "fullName TEXT, " +
                    "email TEXT UNIQUE, " +
                    "password TEXT, " +
                    "grade TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS teachers")
        onCreate(db)
    }

    fun insertTeacher(fullName: String, email: String, password: String, grade: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("fullName", fullName.trim())
            put("email", email.trim())
            put("password", password.trim())
            put("grade", grade.trim())
        }
        db.insert("teachers", null, values)
        db.close()
    }

    fun getTeacherByEmail(email: String): Teacher? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM teachers WHERE email = ? LIMIT 1",
            arrayOf(email.trim())
        )
        var teacher: Teacher? = null
        if (cursor.moveToFirst()) {
            teacher = Teacher(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                grade = cursor.getString(cursor.getColumnIndexOrThrow("grade"))
            )
        }
        cursor.close()
        db.close()
        return teacher
    }

    fun getAllTeachers(): List<Teacher> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM teachers", null)
        val teachers = mutableListOf<Teacher>()

        if (cursor.moveToFirst()) {
            do {
                teachers.add(
                    Teacher(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        fullName = cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                        grade = cursor.getString(cursor.getColumnIndexOrThrow("grade"))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return teachers
    }

    fun updateTeacherData(email: String, newName: String, newGrade: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("fullName", newName.trim())
            put("grade", newGrade.trim())
        }
        val rows = db.update("teachers", values, "email = ?", arrayOf(email.trim()))
        db.close()
        return rows > 0
    }

    fun updatePassword(email: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("password", newPassword.trim())
        }
        val rows = db.update("teachers", values, "email = ?", arrayOf(email.trim()))
        db.close()
        return rows > 0
    }

    // Método para imprimir los datos del profesor en el log (útil para depuración)
    fun printTeacherData(email: String) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM teachers WHERE email = ? LIMIT 1",
            arrayOf(email.trim())
        )
        if (cursor.moveToFirst()) {
            val teacher = Teacher(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                grade = cursor.getString(cursor.getColumnIndexOrThrow("grade"))
            )
            Log.d("TeacherData", "Email: ${teacher.email}, Password: ${teacher.password}")
        } else {
            Log.d("TeacherData", "No teacher found with this email.")
        }
        cursor.close()
        db.close()
    }
}
