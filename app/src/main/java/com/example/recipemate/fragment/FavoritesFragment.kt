package com.example.recipemate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipemate.R
import com.example.recipemate.activity.RecipeDetailActivity
import com.example.recipemate.adapter.RecipeAdapter
import com.example.recipemate.data.local.FavoritesManager
import com.example.recipemate.data.remote.Recipe
import com.example.recipemate.data.remote.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView

    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        // Reload favorites when fragment becomes visible again
        loadFavorites()
    }

    private fun initViews(view: View) {
        rvFavorites = view.findViewById(R.id.rvFavorites)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateText = view.findViewById(R.id.emptyStateText)
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter { recipe ->
            onRecipeClick(recipe)
        }

        recipeAdapter.setOnFavoriteClickListener { recipe ->
            // This will be handled by the adapter itself via FavoritesManager
            // But we need to reload the list since a favorite was removed
            loadFavorites()
        }

        rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
        }
    }

    private fun loadFavorites() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favoriteIds = FavoritesManager.getFavorites()

                if (favoriteIds.isNotEmpty()) {
                    // Fetch full recipe details for each favorite
                    val recipes = mutableListOf<Recipe>()

                    favoriteIds.forEach { recipeId ->
                        try {
                            val response = RetrofitInstance.api.getRecipeInformation(
                                id = recipeId,
                                includeNutrition = false
                            )
                            if (response.isSuccessful && response.body() != null) {
                                val recipe = response.body()!!.copy(isFavorite = true)
                                recipes.add(recipe)
                            }
                        } catch (e: Exception) {
                            // Skip this recipe if there's an error fetching details
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (recipes.isNotEmpty()) {
                            recipeAdapter.submitList(recipes)
                            showResults()
                        } else {
                            showEmptyState("No favorites found")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showEmptyState("You haven't added any favorites yet")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show()
                    showEmptyState("Error loading favorites")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun onRecipeClick(recipe: Recipe) {
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_ID", recipe.id)
            putExtra("RECIPE_TITLE", recipe.title)
            putExtra("RECIPE_IMAGE", recipe.image)
            putExtra("RECIPE_TIME", recipe.readyInMinutes)
            putExtra("RECIPE_SERVINGS", recipe.servings)
        }
        startActivity(intent)
    }

    private fun showResults() {
        rvFavorites.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
    }

    private fun showEmptyState(message: String = "No favorites found") {
        rvFavorites.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyStateText.text = message
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}