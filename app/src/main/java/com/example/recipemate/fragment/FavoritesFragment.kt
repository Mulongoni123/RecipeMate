package com.example.recipemate.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.recipemate.auth.AuthManager
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
    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AuthManager
        authManager = AuthManager(requireContext())

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
            // Remove favorite from Firebase when clicked
            removeFavorite(recipe)
        }

        rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
        }
    }

    private fun loadFavorites() {
        showLoading(true)

        val currentUser = authManager.currentUser
        if (currentUser == null) {
            showEmptyState("Please login to view favorites")
            showLoading(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FavoritesFragment", "Loading favorites from Firebase for user: ${currentUser.uid}")

                // Get favorites from Firebase
                val favoriteSummaries = authManager.getFavorites(currentUser.uid)

                Log.d("FavoritesFragment", "Found ${favoriteSummaries.size} favorites in Firebase")

                if (favoriteSummaries.isNotEmpty()) {
                    // Fetch full recipe details for each favorite
                    val recipes = mutableListOf<Recipe>()

                    favoriteSummaries.forEach { recipeSummary ->
                        try {
                            Log.d("FavoritesFragment", "Fetching details for recipe: ${recipeSummary.title} (ID: ${recipeSummary.id})")

                            val response = RetrofitInstance.api.getRecipeInformation(
                                id = recipeSummary.id,
                                includeNutrition = false
                            )
                            if (response.isSuccessful && response.body() != null) {
                                val recipe = response.body()!!.copy(isFavorite = true)
                                recipes.add(recipe)
                                Log.d("FavoritesFragment", "✅ Successfully loaded: ${recipe.title}")
                            } else {
                                Log.w("FavoritesFragment", "Failed to fetch recipe ${recipeSummary.id}: ${response.message()}")
                            }
                        } catch (e: Exception) {
                            Log.e("FavoritesFragment", "Error fetching recipe ${recipeSummary.id}: ${e.message}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (recipes.isNotEmpty()) {
                            recipeAdapter.submitList(recipes)
                            showResults()
                            Toast.makeText(requireContext(), "Loaded ${recipes.size} favorites", Toast.LENGTH_SHORT).show()
                        } else {
                            showEmptyState("No favorites found")
                            Log.w("FavoritesFragment", "No recipes could be loaded from the favorites list")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showEmptyState("You haven't added any favorites yet")
                        Log.d("FavoritesFragment", "No favorites found in Firebase")
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesFragment", "Error loading favorites from Firebase: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load favorites: ${e.message}", Toast.LENGTH_LONG).show()
                    showEmptyState("Error loading favorites")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun removeFavorite(recipe: Recipe) {
        val currentUser = authManager.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to manage favorites", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                showLoading(true)
                Log.d("FavoritesFragment", "Removing favorite: ${recipe.title} (ID: ${recipe.id})")

                // Remove from Firebase
                authManager.removeFavorite(currentUser.uid, recipe.id)

                // Update UI
                loadFavorites() // Reload the list
                Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("FavoritesFragment", "Error removing favorite: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to remove favorite: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
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
        // Disable interaction while loading
        rvFavorites.isEnabled = !show
    }
}