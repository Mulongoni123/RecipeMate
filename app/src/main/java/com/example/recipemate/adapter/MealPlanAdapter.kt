package com.example.recipemate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipemate.R
import com.example.recipemate.data.model.MealPlan

class MealPlanAdapter(
    private val onRemoveClick: (MealPlan) -> Unit,
    private val onRecipeClick: (MealPlan) -> Unit
) : ListAdapter<MealPlan, MealPlanAdapter.MealPlanViewHolder>(MealPlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealPlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_plan, parent, false)
        return MealPlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealPlanViewHolder, position: Int) {
        val mealPlan = getItem(position)
        holder.bind(mealPlan)
    }

    inner class MealPlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        private val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        private val tvRecipeTitle: TextView = itemView.findViewById(R.id.tvRecipeTitle)
        private val tvServings: TextView = itemView.findViewById(R.id.tvServings)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(mealPlan: MealPlan) {
            tvMealType.text = mealPlan.mealType
            tvRecipeTitle.text = mealPlan.recipeTitle
            tvServings.text = "Servings: ${mealPlan.servings}"

            if (!mealPlan.recipeImage.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(mealPlan.recipeImage)
                    .placeholder(R.drawable.ic_recipe_placeholder)
                    .into(ivRecipeImage)
            } else {
                ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
            }

            btnRemove.setOnClickListener {
                onRemoveClick(mealPlan)
            }

            itemView.setOnClickListener {
                onRecipeClick(mealPlan)
            }
        }
    }

    class MealPlanDiffCallback : DiffUtil.ItemCallback<MealPlan>() {
        override fun areItemsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
            return oldItem == newItem
        }
    }
}