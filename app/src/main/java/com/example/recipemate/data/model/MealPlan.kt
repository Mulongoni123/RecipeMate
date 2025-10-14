package com.example.recipemate.data.model

import java.util.*

data class MealPlan(
    val id: String = UUID.randomUUID().toString(),
    val recipeId: Int,
    val recipeTitle: String,
    val recipeImage: String?,
    val date: Date,
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val servings: Int = 1
)