package com.example.recipemate.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipemate.R
import com.example.recipemate.activity.RecipeDetailActivity
import com.example.recipemate.data.local.FavoritesManager
import com.example.recipemate.data.remote.Recipe

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    private var onFavoriteClick: ((Recipe) -> Unit)? = null

    fun setOnFavoriteClickListener(listener: (Recipe) -> Unit) {
        onFavoriteClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view, onRecipeClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        holder.bind(recipe)
    }

    fun updateRecipe(updatedRecipe: Recipe) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedRecipe.id }
        if (index != -1) {
            currentList[index] = updatedRecipe
            submitList(currentList)
        }
    }

    inner class RecipeViewHolder(
        itemView: View,
        private val onRecipeClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        private val tvRecipeTitle: TextView = itemView.findViewById(R.id.tvRecipeTitle)
        private val tvReadyTime: TextView = itemView.findViewById(R.id.tvReadyTime)
        private val tvServings: TextView = itemView.findViewById(R.id.tvServings)
        private val ibFavorite: ImageButton = itemView.findViewById(R.id.ibFavorite)

        fun bind(recipe: Recipe) {
            // Load image with Glide
            if (!recipe.image.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(recipe.image)
                    .placeholder(R.drawable.ic_recipe_placeholder)
                    .error(R.drawable.ic_recipe_placeholder)
                    .into(ivRecipeImage)
            } else {
                ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
            }

            tvRecipeTitle.text = recipe.title
            tvReadyTime.text = "Ready in ${recipe.readyInMinutes} mins"
            tvServings.text = "Serves ${recipe.servings}"

            // Update favorite button based on FavoritesManager
            val isFavorite = FavoritesManager.isFavorite(recipe.id)
            updateFavoriteButton(isFavorite)

            // Set click listeners
            itemView.setOnClickListener {
                onRecipeClick(recipe)

                // Navigate to RecipeDetailActivity with all available data
                val intent = Intent(itemView.context, RecipeDetailActivity::class.java).apply {
                    putExtra("RECIPE_ID", recipe.id)
                    putExtra("RECIPE_TITLE", recipe.title)
                    putExtra("RECIPE_IMAGE", recipe.image)
                    putExtra("RECIPE_TIME", recipe.readyInMinutes)
                    putExtra("RECIPE_SERVINGS", recipe.servings)
                }
                itemView.context.startActivity(intent)
            }

            ibFavorite.setOnClickListener {
                val isCurrentlyFavorite = FavoritesManager.isFavorite(recipe.id)
                if (isCurrentlyFavorite) {
                    // Remove from favorites
                    FavoritesManager.removeFromFavorites(recipe.id)
                    updateFavoriteButton(false)
                    onFavoriteClick?.invoke(recipe.copy(isFavorite = false))
                } else {
                    // Add to favorites
                    FavoritesManager.addToFavorites(recipe)
                    updateFavoriteButton(true)
                    onFavoriteClick?.invoke(recipe.copy(isFavorite = true))
                }
            }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) {
            if (isFavorite) {
                ibFavorite.setImageResource(R.drawable.ic_favorite_filled)
                ibFavorite.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.red)
                )
            } else {
                ibFavorite.setImageResource(R.drawable.ic_favorite_border)
                ibFavorite.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.gray_medium)
                )
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}