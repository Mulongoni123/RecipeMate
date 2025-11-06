package com.example.recipemate.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("recipe_mate_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    fun getCurrentTheme(): String {
        return sharedPreferences.getString("app_theme", THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setTheme(theme: String) {
        sharedPreferences.edit().putString("app_theme", theme).apply()
        applyTheme(theme)
    }

    fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun applySavedTheme() {
        applyTheme(getCurrentTheme())
    }
}