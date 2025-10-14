package com.example.recipemate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
        return GroceryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        val groceryItem = getItem(position)
        holder.bind(groceryItem)
    }

    inner class GroceryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbItem: CheckBox = itemView.findViewById(R.id.cbItem)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(groceryItem: GroceryItem) {
            tvItemName.text = groceryItem.name
            tvAmount.text = "${groceryItem.amount} ${groceryItem.unit}"
            cbItem.isChecked = groceryItem.isChecked

            cbItem.setOnCheckedChangeListener { _, isChecked ->
                onItemCheck(groceryItem, isChecked)
            }

            btnRemove.setOnClickListener {
                onItemRemove(groceryItem)
            }

            // Strikethrough text when checked
            if (groceryItem.isChecked) {
                tvItemName.paintFlags = tvItemName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                tvAmount.paintFlags = tvAmount.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvItemName.paintFlags = tvItemName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvAmount.paintFlags = tvAmount.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
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