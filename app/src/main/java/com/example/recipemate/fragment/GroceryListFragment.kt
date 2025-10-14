package com.example.recipemate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipemate.R
import com.example.recipemate.adapter.GroceryAdapter
import com.example.recipemate.data.local.GroceryListManager
import com.example.recipemate.data.model.GroceryItem

class GroceryListFragment : Fragment() {

    private lateinit var rvGroceryList: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnGenerate: Button
    private lateinit var btnClear: Button
    private lateinit var emptyStateText: TextView

    private lateinit var groceryAdapter: GroceryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_grocery_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadGroceryList()
    }

    private fun initViews(view: View) {
        rvGroceryList = view.findViewById(R.id.rvGroceryList)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
        btnGenerate = view.findViewById(R.id.btnGenerate)
        btnClear = view.findViewById(R.id.btnClear)
        emptyStateText = view.findViewById(R.id.emptyStateText)
    }

    private fun setupRecyclerView() {
        groceryAdapter = GroceryAdapter(
            onItemCheck = { groceryItem, isChecked ->
                updateGroceryItem(groceryItem, isChecked)
            },
            onItemRemove = { groceryItem ->
                removeGroceryItem(groceryItem)
            }
        )

        rvGroceryList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groceryAdapter
        }
    }

    private fun setupClickListeners() {
        btnGenerate.setOnClickListener {
            generateGroceryList()
        }

        btnClear.setOnClickListener {
            clearGroceryList()
        }
    }

    private fun generateGroceryList() {
        showLoading(true)

        val groceryList = GroceryListManager.generateFromMealPlan()

        if (groceryList.isNotEmpty()) {
            groceryAdapter.submitList(groceryList)
            showResults()
            Toast.makeText(requireContext(), "Grocery list generated!", Toast.LENGTH_SHORT).show()
        } else {
            showEmptyState("No meal plan found. Add recipes to your meal plan first.")
        }

        showLoading(false)
    }

    private fun loadGroceryList() {
        val groceryList = GroceryListManager.getGroceryList()
        if (groceryList.isNotEmpty()) {
            groceryAdapter.submitList(groceryList)
            showResults()
        } else {
            showEmptyState("Generate a grocery list from your meal plan")
        }
    }

    private fun updateGroceryItem(groceryItem: GroceryItem, isChecked: Boolean) {
        val updatedItem = groceryItem.copy(isChecked = isChecked)
        GroceryListManager.updateGroceryItem(updatedItem)
    }

    private fun removeGroceryItem(groceryItem: GroceryItem) {
        GroceryListManager.removeGroceryItem(groceryItem)
        loadGroceryList()
    }

    private fun clearGroceryList() {
        GroceryListManager.clearGroceryList()
        loadGroceryList()
        Toast.makeText(requireContext(), "Grocery list cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showResults() {
        rvGroceryList.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        rvGroceryList.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyStateText.text = message
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}