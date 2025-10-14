package com.example.recipemate.data.local

import com.example.recipemate.data.model.MealPlan
import java.util.*

object MealPlanManager {
    private val mealPlans = mutableListOf<MealPlan>()

    fun addMeal(mealPlan: MealPlan) {
        mealPlans.add(mealPlan)
    }

    fun removeMeal(mealPlan: MealPlan) {
        mealPlans.removeAll { it.id == mealPlan.id }
    }

    fun getMealsForDate(date: Date): List<MealPlan> {
        val calendar = Calendar.getInstance().apply { time = date }
        return mealPlans.filter { mealPlan ->
            val mealCalendar = Calendar.getInstance().apply { time = mealPlan.date }
            calendar.get(Calendar.YEAR) == mealCalendar.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == mealCalendar.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == mealCalendar.get(Calendar.DAY_OF_MONTH)
        }.sortedBy { it.mealType }
    }

    fun getAllMeals(): List<MealPlan> {
        return mealPlans.toList()
    }
}