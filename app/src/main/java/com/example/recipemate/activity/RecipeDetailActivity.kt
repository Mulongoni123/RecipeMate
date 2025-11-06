package com.example.recipemate.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.recipemate.R
import com.example.recipemate.data.local.FavoritesManager
import com.example.recipemate.data.remote.Recipe
import com.example.recipemate.data.remote.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var ingredientsListView: ListView
    private lateinit var instructionsListView: ListView
    private lateinit var btnFavorite: ImageButton

    private var currentRecipe: Recipe? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        initViews()
        setupClickListeners()

        // Get basic recipe data from intent
        val recipeId = intent.getIntExtra("RECIPE_ID", -1)
        val recipeTitle = intent.getStringExtra("RECIPE_TITLE") ?: "Unknown Recipe"
        val recipeImage = intent.getStringExtra("RECIPE_IMAGE")
        val cookingTime = intent.getIntExtra("RECIPE_TIME", 0)

        // Display basic info immediately
        displayBasicInfo(recipeTitle, recipeImage, cookingTime)

        // Fetch full recipe details from Spoonacular API
        if (recipeId != -1) {
            fetchRecipeDetails(recipeId)
        } else {
            showLoading(false)
            showPlaceholderData()
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)
        ingredientsListView = findViewById(R.id.lvIngredients)
        instructionsListView = findViewById(R.id.lvInstructions)
        btnFavorite = findViewById(R.id.btnFavorite)

        // Show loading state initially
        showLoading(true)
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Favorite button
        btnFavorite.setOnClickListener {
            currentRecipe?.let { recipe ->
                if (isFavorite) {
                    removeFromFavorites(recipe)
                } else {
                    addToFavorites(recipe)
                }
            }
        }
    }

    private fun displayBasicInfo(title: String, image: String?, cookingTime: Int) {
        try {
            val imageView = findViewById<ImageView>(R.id.ivRecipeImage)
            val titleTextView = findViewById<TextView>(R.id.tvRecipeTitle)
            val timeTextView = findViewById<TextView>(R.id.tvCookingTime)

            titleTextView.text = title

            val timeText = if (cookingTime > 0) {
                "Cooking Time: $cookingTime minutes"
            } else {
                "Cooking Time: Not specified"
            }
            timeTextView.text = timeText

            // Load image
            if (!image.isNullOrEmpty()) {
                Glide.with(this)
                    .load(image)
                    .placeholder(R.drawable.ic_recipe_placeholder)
                    .error(R.drawable.ic_recipe_placeholder)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_recipe_placeholder)
            }
        } catch (e: Exception) {
            Log.e("RecipeDetailActivity", "Error in displayBasicInfo: ${e.message}")
        }
    }

    private fun fetchRecipeDetails(recipeId: Int) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getRecipeInformation(
                    id = recipeId,
                    includeNutrition = false
                )

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        val fullRecipe = response.body()!!
                        fullRecipe.isDetailed = true
                        updateUIWithFullRecipe(fullRecipe)
                    } else {
                        showError("Could not load full recipe details")
                        // Don't call showBasicRecipeData() if it's causing crashes
                        showPlaceholderData()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Network error: ${e.message}")
                    showPlaceholderData()
                }
            }
        }
    }

    private fun updateUIWithFullRecipe(recipe: Recipe) {
        currentRecipe = recipe

        // Check if recipe is in favorites
        isFavorite = FavoritesManager.isFavorite(recipe.id)
        updateFavoriteButton()

        // Update ingredients
        val ingredients = recipe.getIngredientsOrEmpty().map { ingredient ->
            ingredient.original ?: "${ingredient.amount} ${ingredient.unit} ${ingredient.name}".trim()
        }

        if (ingredients.isNotEmpty()) {
            val ingredientsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ingredients)
            ingredientsListView.adapter = ingredientsAdapter
            ingredientsListView.visibility = View.VISIBLE
        } else {
            showNoIngredientsMessage()
        }

        // Update instructions
        val instructions = mutableListOf<String>()

        recipe.getInstructionsOrEmpty().forEach { instruction ->
            if (!instruction.name.isNullOrEmpty()) {
                instructions.add("🔸 ${instruction.name}:")
            }

            instruction.steps.forEach { step ->
                instructions.add("${step.number}. ${step.description}")
            }

            if (instruction != recipe.getInstructionsOrEmpty().last()) {
                instructions.add("")
            }
        }

        if (instructions.isNotEmpty()) {
            val instructionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, instructions)
            instructionsListView.adapter = instructionsAdapter
            instructionsListView.visibility = View.VISIBLE
        } else {
            showNoInstructionsMessage()
        }
    }

    private fun addToFavorites(recipe: Recipe) {
        FavoritesManager.addToFavorites(recipe)
        isFavorite = true
        updateFavoriteButton()
        Toast.makeText(this, "✓ Added to favorites", Toast.LENGTH_SHORT).show()
    }

    private fun removeFromFavorites(recipe: Recipe) {
        FavoritesManager.removeFromFavorites(recipe.id)
        isFavorite = false
        updateFavoriteButton()
        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
    }

    private fun updateFavoriteButton() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
            btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.red))
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.gray_medium))
        }
    }

    private fun showNoIngredientsMessage() {
        val message = arrayOf("No ingredients information available")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, message)
        ingredientsListView.adapter = adapter
    }

    private fun showNoInstructionsMessage() {
        val message = arrayOf("No cooking instructions available")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, message)
        instructionsListView.adapter = adapter
    }

    private fun showPlaceholderData() {
        val placeholderIngredients = arrayOf(
            "Ingredients data not available",
            "Please check the original recipe source"
        )

        val placeholderInstructions = arrayOf(
            "Instructions data not available",
            "Please check the original recipe source for cooking instructions"
        )

        val ingredientsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, placeholderIngredients)
        val instructionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, placeholderInstructions)

        ingredientsListView.adapter = ingredientsAdapter
        instructionsListView.adapter = instructionsAdapter
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        contentLayout.visibility = if (show) View.GONE else View.VISIBLE
    }
}