package com.example.recipemate.data.model

data class GroceryItem(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val category: String, // Produce, Dairy, Meat, Pantry, etc.
    val isChecked: Boolean = false
)