package com.example.recipemate.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.example.recipemate.data.local.FavoritesManager
import com.example.recipemate.data.remote.Recipe
import com.example.recipemate.data.remote.RecipeSummary
import com.example.recipemate.data.remote.RetrofitInstance
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultsCount: TextView
    private lateinit var rvRecipes: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var noResultsState: LinearLayout

    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var authManager: AuthManager

    private var currentRecipes: List<Recipe> = emptyList()
    private var isMealPlanMode = false
    private var mealType = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AuthManager with proper context
        authManager = AuthManager(requireContext())

        initViews(view)
        setupRecyclerView()
        setupClickListeners()

        // Check if we're in meal plan mode
        checkMealPlanMode()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        progressBar = view.findViewById(R.id.progressBar)
        tvResultsCount = view.findViewById(R.id.tvResultsCount)
        rvRecipes = view.findViewById(R.id.rvRecipes)
        emptyState = view.findViewById(R.id.emptyState)
        noResultsState = view.findViewById(R.id.noResultsState)
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter { recipe ->
            onRecipeClick(recipe)
        }

        recipeAdapter.setOnFavoriteClickListener { recipe ->
            toggleFavorite(recipe)
        }

        rvRecipes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
        }
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchRecipes(query)
            } else {
                etSearch.error = "Please enter a search term"
            }
        }

        // Handle keyboard search action
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchRecipes(query)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun checkMealPlanMode() {
        // Check if we're in meal plan mode (if launched from MealPlannerFragment)
        arguments?.let { bundle ->
            isMealPlanMode = bundle.getBoolean("MEAL_PLAN_MODE", false)
            mealType = bundle.getString("MEAL_TYPE", "")

            if (isMealPlanMode) {
                // Update UI for meal plan mode
                updateUIForMealPlanMode()
            }
        }
    }

    private fun updateUIForMealPlanMode() {
        // Change the title or add a indicator that we're selecting for meal plan
        requireActivity().title = "Select Recipe for $mealType"
        btnSearch.text = "Search Recipes for $mealType"
    }

    private fun searchRecipes(query: String) {
        showLoading(true)
        hideKeyboard()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitInstance.api.searchRecipes(
                    query = query,
                    number = 20,
                    addRecipeInformation = true,  // This should include basic info
                    fillIngredients = true        // This should include ingredients
                )

                if (response.isSuccessful) {
                    val recipes = response.body()?.getRecipesOrEmpty() ?: emptyList()
                    currentRecipes = recipes

                    if (recipes.isNotEmpty()) {
                        showResults(recipes)
                        tvResultsCount.text = "Found ${recipes.size} recipes"

                        // Debug: Check what data we're getting
                        recipes.forEach { recipe ->
                            Log.d("SearchFragment", "=== Recipe: ${recipe.title} ===")
                            Log.d("SearchFragment", "Time: ${recipe.readyInMinutes}")
                            Log.d("SearchFragment", "Servings: ${recipe.servings}")
                            Log.d("SearchFragment", "Ingredients count: ${recipe.getIngredientsOrEmpty().size}")
                            Log.d("SearchFragment", "Instructions count: ${recipe.getInstructionsOrEmpty().size}")

                            // Log instruction details
                            recipe.getInstructionsOrEmpty().forEachIndexed { index, instruction ->
                                Log.d("SearchFragment", "Instruction $index - Name: ${instruction.name}")
                                Log.d("SearchFragment", "Steps count: ${instruction.steps.size}")
                                instruction.steps.forEach { step ->
                                    Log.d("SearchFragment", "  Step ${step.number}: ${step.description}")
                                }
                            }
                        }
                    } else {
                        showNoResults()
                    }
                } else {
                    showError("Search failed: ${response.message()}")
                }
            } catch (e: Exception) {
                showError("Search failed: ${e.message}")
                Log.e("SearchFragment", "Search error", e)
            } finally {
                showLoading(false)
            }
        }
    }
    private fun onRecipeClick(recipe: Recipe) {
        if (isMealPlanMode) {
            // Return the selected recipe to MealPlannerFragment
            returnRecipeToMealPlanner(recipe)
        } else {
            // Normal behavior - navigate to recipe details
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
                putExtra("RECIPE_ID", recipe.id)
                putExtra("RECIPE_TITLE", recipe.title)
                putExtra("RECIPE_IMAGE", recipe.image)
                putExtra("RECIPE_TIME", recipe.readyInMinutes)
                putExtra("RECIPE_SERVINGS", recipe.servings)
            }
            startActivity(intent)
        }
    }

    private fun returnRecipeToMealPlanner(recipe: Recipe) {
        // Create result intent with recipe data
        val resultIntent = Intent().apply {
            putExtra("RECIPE_ID", recipe.id)
            putExtra("RECIPE_TITLE", recipe.title)
            putExtra("RECIPE_IMAGE", recipe.image)
            putExtra("RECIPE_TIME", recipe.readyInMinutes)
            putExtra("RECIPE_SERVINGS", recipe.servings)
        }

        // Set result and finish (if this is an activity)
        // Since we're in a fragment, we'll use parent fragment manager to communicate
        returnToMealPlannerWithResult(recipe)
    }

    private fun returnToMealPlannerWithResult(recipe: Recipe) {
        try {
            // Use requireActivity().supportFragmentManager to ensure we're using the activity's fragment manager
            val resultBundle = Bundle().apply {
                putInt("RECIPE_ID", recipe.id)
                putString("RECIPE_TITLE", recipe.title)
                putString("RECIPE_IMAGE", recipe.image)
                putInt("RECIPE_TIME", recipe.readyInMinutes)
                putInt("RECIPE_SERVINGS", recipe.servings)
                putString("MEAL_TYPE", mealType)
            }

            Log.d("SearchFragment", "Sending recipe to meal planner: ${recipe.title} for $mealType")

            // Use the activity's supportFragmentManager
            requireActivity().supportFragmentManager.setFragmentResult("MEAL_PLAN_RESULT", resultBundle)

            // Navigate back
            requireActivity().supportFragmentManager.popBackStack()

            Toast.makeText(requireContext(), "Added ${recipe.title} to $mealType", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SearchFragment", "Error returning to meal planner", e)
            Toast.makeText(requireContext(), "Error adding recipe to meal plan", Toast.LENGTH_SHORT).show()
        }
    }
    private fun toggleFavorite(recipe: Recipe) {
        val currentUser = authManager.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to save favorites", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("SearchFragment", "=== TOGGLE FAVORITE START ===")
                Log.d("SearchFragment", "Recipe: ${recipe.title} (ID: ${recipe.id})")
                Log.d("SearchFragment", "Current favorite status: ${recipe.isFavorite}")
                Log.d("SearchFragment", "User UID: ${currentUser.uid}")

                if (recipe.isFavorite) {
                    // Remove from favorites
                    Log.d("SearchFragment", "🔄 Removing from favorites...")
                    authManager.removeFavorite(currentUser.uid, recipe.id)
                    recipe.isFavorite = false
                    Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
                } else {
                    // Add to favorites
                    Log.d("SearchFragment", "⭐ Adding to favorites...")
                    val recipeSummary = RecipeSummary(
                        id = recipe.id,
                        title = recipe.title,
                        image = recipe.image ?: ""
                    )
                    Log.d("SearchFragment", "RecipeSummary: $recipeSummary")

                    authManager.addFavorite(currentUser.uid, recipeSummary)
                    recipe.isFavorite = true
                    Toast.makeText(requireContext(), "Added to favorites! ✅", Toast.LENGTH_SHORT).show()
                }

                // Update adapter
                recipeAdapter.updateRecipe(recipe)
                Log.d("SearchFragment", "=== TOGGLE FAVORITE COMPLETE ===")

            } catch (e: Exception) {
                Log.e("SearchFragment", "❌ ERROR in toggleFavorite: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to update favorite", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showResults(recipes: List<Recipe>) {
        recipeAdapter.submitList(recipes)
        rvRecipes.visibility = View.VISIBLE
        tvResultsCount.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        noResultsState.visibility = View.GONE
    }

    private fun showNoResults() {
        rvRecipes.visibility = View.GONE
        tvResultsCount.visibility = View.GONE
        emptyState.visibility = View.GONE
        noResultsState.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSearch.isEnabled = !show
        btnSearch.text = if (show) "Searching..." else if (isMealPlanMode) "Search for $mealType" else "Search Recipes"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        showNoResults()
    }

    @SuppressLint("ServiceCast")
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    companion object {
        fun newInstance(mealPlanMode: Boolean = false, mealType: String = ""): SearchFragment {
            return SearchFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("MEAL_PLAN_MODE", mealPlanMode)
                    putString("MEAL_TYPE", mealType)
                }
            }

        }
    }
}