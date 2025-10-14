package com.example.recipemate.data.local

import com.example.recipemate.data.remote.Recipe

object FavoritesManager {
    private val favorites = mutableSetOf<Int>() // Store recipe IDs

    fun addToFavorites(recipe: Recipe) {
        favorites.add(recipe.id)
    }

    fun removeFromFavorites(recipeId: Int) {
        favorites.remove(recipeId)
    }

    fun isFavorite(recipeId: Int): Boolean {
        return favorites.contains(recipeId)
    }

    fun getFavorites(): List<Int> {
        return favorites.toList()
    }
}