package com.example.recipemate.data.local

import com.example.recipemate.data.model.GroceryItem
import com.example.recipemate.data.model.MealPlan
import java.util.*

object GroceryListManager {
    private val groceryList = mutableListOf<GroceryItem>()

    fun generateFromMealPlan(): List<GroceryItem> {
        val mealPlans = MealPlanManager.getAllMeals()
        val aggregatedItems = mutableMapOf<String, GroceryItem>()

        mealPlans.forEach { mealPlan ->
            // For demo, we'll create some sample ingredients
            // In real app, you'd fetch the actual recipe ingredients
            val sampleIngredients = listOf(
                GroceryItem(UUID.randomUUID().toString(), "Tomatoes", 2.0, "pieces", "Produce"),
                GroceryItem(UUID.randomUUID().toString(), "Olive Oil", 1.0, "cup", "Pantry"),
                GroceryItem(UUID.randomUUID().toString(), "Garlic", 3.0, "cloves", "Produce")
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
}