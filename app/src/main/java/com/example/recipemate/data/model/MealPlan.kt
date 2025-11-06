package com.example.recipemate.data.model

import java.util.*

data class MealPlan(
    val id: String = "",
    val recipeId: Int = 1,
    val recipeTitle: String = "",
    val recipeImage: String? = null,
    val date: Date = Date(),
    val mealType: String = "", // "Breakfast", "Lunch", "Dinner", etc.
    val servings: Int = 1,
    val userId: String = ""
)