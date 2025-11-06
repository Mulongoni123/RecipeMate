package com.example.recipemate.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Search recipes (returns basic info) - now returns Response
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("number") number: Int = 20,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("instructionsRequired") instructionsRequired: Boolean = true
    ): Response<RecipeSearchResponse>  // Now returns Response

    // Get detailed recipe information - now returns Response

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): Response<Recipe>

    // Get random recipes - now returns Response
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 10
    ): Response<RandomRecipeResponse>  // Now returns Response
}

