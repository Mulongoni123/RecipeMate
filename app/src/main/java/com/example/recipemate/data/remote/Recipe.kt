package com.example.recipemate.data.remote

import com.google.gson.annotations.SerializedName

data class Recipe(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("image")
    val image: String?,

    @SerializedName("readyInMinutes")
    val readyInMinutes: Int,

    @SerializedName("servings")
    val servings: Int,

    @SerializedName("summary")
    val summary: String? = null,

    @SerializedName("extendedIngredients")
    val ingredients: List<Ingredient> = emptyList(),

    @SerializedName("analyzedInstructions")
    val instructions: List<Instruction> = emptyList(),

    // Local fields for UI state
    var isFavorite: Boolean = false
) {
    // Helper function to convert to RecipeSummary for favorites
    fun toRecipeSummary(): RecipeSummary {
        return RecipeSummary(
            id = id,
            title = title,
            image = image ?: ""
        )
    }
}

data class Ingredient(
    @SerializedName("name")
    val name: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("unit")
    val unit: String
)

data class Instruction(
    @SerializedName("steps")
    val steps: List<Step>
)

data class Step(
    @SerializedName("number")
    val number: Int,

    @SerializedName("step")
    val description: String
)

data class RecipeSearchResponse(
    @SerializedName("results")
    val recipes: List<Recipe>,

    @SerializedName("totalResults")
    val totalResults: Int,

    @SerializedName("offset")
    val offset: Int,

    @SerializedName("number")
    val number: Int
)