package com.example.recipemate.data.local

import com.example.recipemate.data.model.MealPlan
import java.util.*

// In MealPlanManager.kt
object MealPlanManager {
    private val mealPlans = mutableListOf<MealPlan>()

    fun addMeal(mealPlan: MealPlan) {
        // Remove any existing meal for the same date and meal type
        mealPlans.removeAll { existing ->
            isSameDate(existing.date, mealPlan.date) && existing.mealType == mealPlan.mealType
        }
        mealPlans.add(mealPlan)
    }

    fun removeMeal(mealPlan: MealPlan) {
        mealPlans.removeAll { it.id == mealPlan.id }
    }

    fun getMealsForDate(date: Date): List<MealPlan> {
        return mealPlans.filter { mealPlan ->
            isSameDate(mealPlan.date, date)
        }.sortedBy { it.mealType }
    }

    fun getAllMeals(): List<MealPlan> {
        return mealPlans.toList()
    }

    private fun isSameDate(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}



