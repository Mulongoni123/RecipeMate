package com.example.recipemate

import android.app.Application
import com.example.recipemate.utils.LanguageManager
import com.example.recipemate.utils.ThemeManager

class RecipeMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply saved theme on app start
        ThemeManager(this).applySavedTheme()

        // Apply saved language on app start
        LanguageManager(this).applySavedLanguage()

        // Initialize Firebase and other services
        initializeServices()
    }

    private fun initializeServices() {
        // Firebase and other initializations will go here
    }
}