package com.example.recipemate.data.remote

import com.google.gson.annotations.SerializedName

data class Recipe(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("image")
    val image: String? = null,

    @SerializedName("readyInMinutes")
    val readyInMinutes: Int = 0,

    @SerializedName("servings")
    val servings: Int = 0,

    @SerializedName("summary")
    val summary: String? = null,

    @SerializedName("extendedIngredients")
    val ingredients: List<Ingredient>? = null, // Make nullable

    @SerializedName("analyzedInstructions")
    val instructions: List<Instruction>? = null, // Make nullable

    @SerializedName("dishTypes")
    val dishTypes: List<String>? = null,

    @SerializedName("diets")
    val diets: List<String>? = null,

    @SerializedName("cuisines")
    val cuisines: List<String>? = null,

    // Local fields for UI state
    var isFavorite: Boolean = false,
    var isDetailed: Boolean = false
) {
    // Helper function to convert to RecipeSummary for favorites
    fun toRecipeSummary(): RecipeSummary {
        return RecipeSummary(
            id = id,
            title = title,
            image = image ?: ""
        )
    }

    // Get cooking time formatted
    fun getFormattedTime(): String {
        return if (readyInMinutes > 0) {
            if (readyInMinutes < 60) {
                "Ready in $readyInMinutes mins"
            } else {
                val hours = readyInMinutes / 60
                val minutes = readyInMinutes % 60
                if (minutes > 0) {
                    "Ready in ${hours}h ${minutes}m"
                } else {
                    "Ready in ${hours}h"
                }
            }
        } else {
            "Time not specified"
        }
    }

    // Get servings formatted
    fun getFormattedServings(): String {
        return if (servings > 0) {
            "Serves $servings"
        } else {
            "Servings not specified"
        }
    }

    // Get all instructions as a single list (safe from null)
    fun getAllSteps(): List<Step> {
        return instructions?.flatMap { it.steps } ?: emptyList()
    }

    // Safe getter for ingredients
    fun getIngredientsOrEmpty(): List<Ingredient> {
        return ingredients ?: emptyList()
    }

    // Safe getter for instructions
    fun getInstructionsOrEmpty(): List<Instruction> {
        return instructions ?: emptyList()
    }
}

data class Ingredient(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("amount")
    val amount: Double = 0.0,

    @SerializedName("unit")
    val unit: String = "",

    @SerializedName("original")
    val original: String? = null,

    @SerializedName("image")
    val image: String? = null
) {
    fun getFormattedAmount(): String {
        return if (amount % 1 == 0.0) {
            "${amount.toInt()} $unit"
        } else {
            String.format("%.1f %s", amount, unit)
        }
    }

    // Get display text for the ingredient
    fun getDisplayText(): String {
        return original ?: "${getFormattedAmount()} $name".trim()
    }
}

data class Instruction(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("steps")
    val steps: List<Step> = emptyList()
)

data class Step(
    @SerializedName("number")
    val number: Int = 0,

    @SerializedName("step")
    val description: String = "",

    @SerializedName("ingredients")
    val ingredients: List<Ingredient>? = null,

    @SerializedName("equipment")
    val equipment: List<Equipment>? = null
)

data class Equipment(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("image")
    val image: String? = null
)

data class RecipeSearchResponse(
    @SerializedName("results")
    val recipes: List<Recipe>? = null, // Make nullable with default

    @SerializedName("totalResults")
    val totalResults: Int = 0,

    @SerializedName("offset")
    val offset: Int = 0,

    @SerializedName("number")
    val number: Int = 0
) {
    // Helper function to safely get recipes
    fun getRecipesOrEmpty(): List<Recipe> {
        return recipes ?: emptyList()
    }
}

data class RandomRecipeResponse(
    @SerializedName("recipes")
    val recipes: List<Recipe>? = null
) {
    fun getRecipesOrEmpty(): List<Recipe> {
        return recipes ?: emptyList()
    }
}

