package com.example.recipemate.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    var displayName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val favoritesCount: Int = 0,
    val recipesTried: Int = 0,
    val cookingStreak: Int = 0,
    val passwordHash: String? = null,
    val isSSOUser: Boolean = false,
    val ssoProvider: String? = null
)