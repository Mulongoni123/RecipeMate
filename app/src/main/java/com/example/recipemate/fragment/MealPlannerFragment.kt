package com.example.recipemate.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipemate.R
import com.example.recipemate.activity.RecipeDetailActivity
import com.example.recipemate.adapter.MealPlanAdapter
import com.example.recipemate.data.local.GroceryListManager
import com.example.recipemate.data.local.MealPlanManager
import com.example.recipemate.data.model.MealPlan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MealPlannerFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var rvMealPlan: RecyclerView
    private lateinit var btnAddRecipe: Button
    private lateinit var btnGroceryList: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var emptyState: LinearLayout

    private lateinit var mealPlanAdapter: MealPlanAdapter
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meal_planner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        setupFragmentResultListener() // Make sure this is called
        loadMealPlanForDate(selectedDate)


        // Set initial calendar selection
        calendarView.date = selectedDate.timeInMillis
    }

    private fun setupFragmentResultListener() {
        // Use requireActivity().supportFragmentManager for consistency
        requireActivity().supportFragmentManager.setFragmentResultListener("MEAL_PLAN_RESULT", viewLifecycleOwner) { requestKey, bundle ->
            Log.d("MealPlannerFragment", "Received fragment result: $requestKey")

            if (requestKey == "MEAL_PLAN_RESULT") {
                handleMealPlanResult(bundle)
            }
        }
    }

    private fun handleMealPlanResult(bundle: Bundle) {
        try {
            val recipeId = bundle.getInt("RECIPE_ID")
            val recipeTitle = bundle.getString("RECIPE_TITLE") ?: "Unknown Recipe"
            val recipeImage = bundle.getString("RECIPE_IMAGE")
            val mealType = bundle.getString("MEAL_TYPE", "Meal")
            val recipeTime = bundle.getInt("RECIPE_TIME", 0)
            val recipeServings = bundle.getInt("RECIPE_SERVINGS", 1)

            Log.d("MealPlannerFragment", "Adding recipe: $recipeTitle for $mealType")

            // Create and add the meal plan
            val mealPlan = MealPlan(
                id = UUID.randomUUID().toString(),
                recipeId = recipeId.toInt(),
                recipeTitle = recipeTitle,
                recipeImage = recipeImage,
                date = selectedDate.time,
                mealType = mealType,
                servings = recipeServings
            )

            MealPlanManager.addMeal(mealPlan)
            loadMealPlanForDate(selectedDate)

            Toast.makeText(requireContext(), "Successfully added $recipeTitle to $mealType", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MealPlannerFragment", "Error handling meal plan result", e)
            Toast.makeText(requireContext(), "Error adding recipe: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun initViews(view: View) {
        calendarView = view.findViewById(R.id.calendarView)
        rvMealPlan = view.findViewById(R.id.rvMealPlan)
        btnAddRecipe = view.findViewById(R.id.btnAddRecipe)
        btnGroceryList = view.findViewById(R.id.btnGroceryList)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        emptyState = view.findViewById(R.id.emptyState)

        updateSelectedDateText()
    }

    private fun setupRecyclerView() {
        mealPlanAdapter = MealPlanAdapter(
            onRemoveClick = { mealPlan ->
                removeMealFromPlan(mealPlan)
            },
            onRecipeClick = { mealPlan ->
                openRecipeDetails(mealPlan)
            }
        )

        rvMealPlan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealPlanAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateSelectedDateText()
            loadMealPlanForDate(selectedDate)
        }

        btnAddRecipe.setOnClickListener {
            showAddRecipeDialog()
        }

        btnGroceryList.setOnClickListener {
            generateGroceryList()
        }
    }

    private fun updateSelectedDateText() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        tvSelectedDate.text = dateFormat.format(selectedDate.time)
    }

    private fun loadMealPlanForDate(date: Calendar) {
        val meals = MealPlanManager.getMealsForDate(date.time)
        updateUI(meals)
    }

    private fun updateUI(meals: List<MealPlan>) {
        if (meals.isNotEmpty()) {
            mealPlanAdapter.submitList(meals)
            rvMealPlan.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        } else {
            rvMealPlan.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        }
    }

    private fun showAddRecipeDialog() {
        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Recipe to Meal Plan")
            .setMessage("Choose a meal type:")
            .setItems(mealTypes) { _, which ->
                val mealType = mealTypes[which]
                // DEBUG: Check if this is being called
                Toast.makeText(requireContext(), "Selected: $mealType", Toast.LENGTH_SHORT).show()
                launchSearchFragmentForMealPlan(mealType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchSearchFragmentForMealPlan(mealType: String) {
        try {
            Log.d("MealPlannerFragment", "Launching SearchFragment for meal type: $mealType")

            val searchFragment = SearchFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("MEAL_PLAN_MODE", true)
                    putString("MEAL_TYPE", mealType)
                    putLong("SELECTED_DATE", selectedDate.timeInMillis)
                }
            }

            // Use requireActivity().supportFragmentManager for consistency
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, searchFragment)
                .addToBackStack("meal_plan_search")
                .commit()

        } catch (e: Exception) {
            Log.e("MealPlannerFragment", "Error launching SearchFragment", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateGroceryList() {
        val groceryList = GroceryListManager.generateFromMealPlan()

        if (groceryList.isNotEmpty()) {
            showGroceryListDialog(groceryList)
        } else {
            Toast.makeText(requireContext(), "No meals planned to generate grocery list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGroceryListDialog(groceryList: List<com.example.recipemate.data.model.GroceryItem>) {
        val items = groceryList.map { item ->
            "• ${item.amount} ${item.unit} ${item.name}"
        }.toTypedArray()

        val message = "Generated from your meal plan:\n\n${items.joinToString("\n")}"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Grocery List")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun removeMealFromPlan(mealPlan: MealPlan) {
        MealPlanManager.removeMeal(mealPlan)
        loadMealPlanForDate(selectedDate)
        Toast.makeText(requireContext(), "Removed from meal plan", Toast.LENGTH_SHORT).show()
    }

    private fun openRecipeDetails(mealPlan: MealPlan) {
        try {
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
                putExtra("RECIPE_ID", mealPlan.recipeId)
                putExtra("RECIPE_TITLE", mealPlan.recipeTitle)
                putExtra("RECIPE_IMAGE", mealPlan.recipeImage)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening recipe details", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Add this method to handle fragment results when returning from SearchFragment
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadMealPlanForDate(selectedDate)
    }
}