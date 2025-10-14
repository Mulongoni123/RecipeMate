package com.example.recipemate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipemate.R
import com.example.recipemate.activity.RecipeDetailActivity
import com.example.recipemate.adapter.MealPlanAdapter
import com.example.recipemate.data.local.GroceryListManager
import com.example.recipemate.data.local.MealPlanManager
import com.example.recipemate.data.model.MealPlan
import java.util.*

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
        loadMealPlanForDate(selectedDate)

        // Set up fragment result listener
        setupFragmentResultListener()
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
                val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
                    putExtra("RECIPE_ID", mealPlan.recipeId)
                    putExtra("RECIPE_TITLE", mealPlan.recipeTitle)
                    putExtra("RECIPE_IMAGE", mealPlan.recipeImage)
                }
                startActivity(intent)
            }
        )

        rvMealPlan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealPlanAdapter
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

    private fun setupFragmentResultListener() {
        // FIX: Use parentFragmentManager instead of childFragmentManager
        parentFragmentManager.setFragmentResultListener("MEAL_PLAN_RESULT", this) { requestKey, bundle ->
            if (requestKey == "MEAL_PLAN_RESULT") {
                val recipeId = bundle.getInt("RECIPE_ID")
                val recipeTitle = bundle.getString("RECIPE_TITLE") ?: "Unknown Recipe"
                val recipeImage = bundle.getString("RECIPE_IMAGE")
                val mealType = bundle.getString("MEAL_TYPE", "Meal") // Get meal type from bundle

                // Add the recipe to meal plan
                val mealPlan = MealPlan(
                    recipeId = recipeId,
                    recipeTitle = recipeTitle,
                    recipeImage = recipeImage,
                    date = selectedDate.time,
                    mealType = mealType,
                    servings = 1
                )
                MealPlanManager.addMeal(mealPlan)
                loadMealPlanForDate(selectedDate)
                Toast.makeText(requireContext(), "Added $recipeTitle to $mealType", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedDateText() {
        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        tvSelectedDate.text = dateFormat.format(selectedDate.time)
    }

    private fun loadMealPlanForDate(date: Calendar) {
        val meals = MealPlanManager.getMealsForDate(date.time)
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
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Recipe to Meal Plan")
            .setMessage("Choose a meal type:")
            .setItems(arrayOf("Breakfast", "Lunch", "Dinner", "Snack")) { _, which ->
                val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
                val mealType = mealTypes[which]

                // Launch SearchFragment in meal plan mode
                launchSearchFragmentForMealPlan(mealType)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun launchSearchFragmentForMealPlan(mealType: String) {
        // Create SearchFragment with meal plan mode
        val searchFragment = SearchFragment.newInstance(mealPlanMode = true, mealType = mealType)

        // Replace current fragment with SearchFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, searchFragment)
            .addToBackStack("meal_plan_search")
            .commit()
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
            "${item.amount} ${item.unit} ${item.name}"
        }.toTypedArray()

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Grocery List")
            .setMessage("Generated from your meal plan")
            .setItems(items) { _, _ -> }
            .setPositiveButton("Close", null)
            .create()
        dialog.show()
    }

    private fun removeMealFromPlan(mealPlan: MealPlan) {
        MealPlanManager.removeMeal(mealPlan)
        loadMealPlanForDate(selectedDate)
        Toast.makeText(requireContext(), "Removed from meal plan", Toast.LENGTH_SHORT).show()
    }
}