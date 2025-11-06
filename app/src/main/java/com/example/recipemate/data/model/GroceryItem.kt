package com.example.recipemate.data.model

data class GroceryItem(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = "",
    val category: String = "",
    val isChecked: Boolean = false
)