package com.example.recipemate.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

class LanguageManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("recipe_mate_prefs", Context.MODE_PRIVATE)

    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_AFRIKAANS = "af"
        const val KEY_SELECTED_LANGUAGE = "selected_language"
    }

    fun getCurrentLanguage(): String {
        return sharedPreferences.getString(KEY_SELECTED_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    fun setLanguage(languageCode: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_LANGUAGE, languageCode).apply()
        updateAppLanguage(languageCode)
    }

    fun updateAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun applySavedLanguage() {
        updateAppLanguage(getCurrentLanguage())
    }
}