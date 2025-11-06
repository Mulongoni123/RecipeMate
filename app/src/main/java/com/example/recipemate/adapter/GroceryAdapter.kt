package com.example.recipemate.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipemate.R
import com.example.recipemate.data.model.GroceryItem

class GroceryAdapter(
    private val onItemCheck: (GroceryItem, Boolean) -> Unit,
    private val onItemRemove: (GroceryItem) -> Unit
) : ListAdapter<GroceryItem, GroceryAdapter.GroceryViewHolder>(GroceryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroceryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grocery, parent, false)
        return GroceryViewHolder(view, onItemCheck, onItemRemove)
    }

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        val groceryItem = getItem(position)
        holder.bind(groceryItem)
    }

    class GroceryViewHolder(
        itemView: View,
        private val onItemCheck: (GroceryItem, Boolean) -> Unit,
        private val onItemRemove: (GroceryItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val cbItem: CheckBox = itemView.findViewById(R.id.cbItem)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(groceryItem: GroceryItem) {
            // Remove previous listener to avoid multiple callbacks
            cbItem.setOnCheckedChangeListener(null)

            // Set data
            tvItemName.text = groceryItem.name
            tvAmount.text = "${groceryItem.amount} ${groceryItem.unit}"
            cbItem.isChecked = groceryItem.isChecked

            // Update strikethrough
            updateStrikethrough(groceryItem.isChecked)

            // Set checkbox listener
            cbItem.setOnCheckedChangeListener { _, isChecked ->
                onItemCheck(groceryItem, isChecked)
            }

            // Set remove button listener
            btnRemove.setOnClickListener {
                onItemRemove(groceryItem)
            }
        }

        private fun updateStrikethrough(isChecked: Boolean) {
            if (isChecked) {
                tvItemName.paintFlags = tvItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvAmount.paintFlags = tvAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvItemName.paintFlags = tvItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvAmount.paintFlags = tvAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    class GroceryDiffCallback : DiffUtil.ItemCallback<GroceryItem>() {
        override fun areItemsTheSame(oldItem: GroceryItem, newItem: GroceryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroceryItem, newItem: GroceryItem): Boolean {
            return oldItem == newItem
        }
    }
}