package com.example.dsatracker.data

import android.content.Context
import com.google.gson.Gson
import java.io.IOException

object JsonReader {
    fun loadSheet(context: Context): StriverSheet {
        return try {
            val jsonString = context.assets.open("problems.json").bufferedReader().use { it.readText() }
            val sheet = Gson().fromJson(jsonString, StriverSheet::class.java)

            // Pro Tip: Har static category ko ek specific 'userId' de do
            // taaki hum pehchan sakein ki ye "Custom" nahi "Official" sheet hai.
            val fixedCategories = sheet.categories.map { it.copy(userId = "admin_static") }

            StriverSheet(categories = fixedCategories)
        } catch (e: IOException) {
            e.printStackTrace()
            StriverSheet(emptyList()) // Return empty if file is missing
        } catch (e: Exception) {
            e.printStackTrace()
            StriverSheet(emptyList())
        }
    }
}