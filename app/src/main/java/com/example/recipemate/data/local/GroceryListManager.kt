package com.example.recipemate.data.local

import com.example.recipemate.data.model.GroceryItem
import com.example.recipemate.data.model.MealPlan
import java.util.*

// data/local/GroceryListManager.kt
object GroceryListManager {
    private val groceryList = mutableListOf<GroceryItem>()

    fun generateFromMealPlan(): List<GroceryItem> {
        val mealPlans = MealPlanManager.getAllMeals()
        val aggregatedItems = mutableMapOf<String, GroceryItem>()

        mealPlans.forEach { mealPlan ->
            // For demo, create sample ingredients
            val sampleIngredients = listOf(
                GroceryItem(
                    id = UUID.randomUUID().toString(),
                    name = "Tomatoes",
                    amount = 2.0,
                    unit = "pieces",
                    category = "Produce",
                    isChecked = false
                ),
                GroceryItem(
                    id = UUID.randomUUID().toString(),
                    name = "Olive Oil",
                    amount = 1.0,
                    unit = "cup",
                    category = "Pantry",
                    isChecked = false
                ),
                GroceryItem(
                    id = UUID.randomUUID().toString(),
                    name = "Garlic",
                    amount = 3.0,
                    unit = "cloves",
                    category = "Produce",
                    isChecked = false
                )
            )

            sampleIngredients.forEach { ingredient ->
                val key = "${ingredient.name}-${ingredient.unit}"
                if (aggregatedItems.containsKey(key)) {
                    val existing = aggregatedItems[key]!!
                    aggregatedItems[key] = existing.copy(amount = existing.amount + ingredient.amount)
                } else {
                    aggregatedItems[key] = ingredient
                }
            }
        }

        groceryList.clear()
        groceryList.addAll(aggregatedItems.values)
        return groceryList.toList()
    }

    fun getGroceryList(): List<GroceryItem> {
        return groceryList.toList()
    }

    fun updateGroceryItem(updatedItem: GroceryItem) {
        val index = groceryList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            groceryList[index] = updatedItem
        }
    }

    fun removeGroceryItem(groceryItem: GroceryItem) {
        groceryList.removeAll { it.id == groceryItem.id }
    }

    fun clearGroceryList() {
        groceryList.clear()
    }

    // New method to toggle item checked state
    fun toggleItemChecked(itemId: String, isChecked: Boolean) {
        val index = groceryList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = groceryList[index]
            groceryList[index] = item.copy(isChecked = isChecked)
        }
    }
}