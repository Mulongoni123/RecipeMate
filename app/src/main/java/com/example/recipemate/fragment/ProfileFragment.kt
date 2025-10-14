package com.example.recipemate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recipemate.R
import com.example.recipemate.activity.LoginActivity
import com.example.recipemate.auth.AuthManager
import com.example.recipemate.data.model.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var tvRecipesTried: TextView
    private lateinit var tvCookingStreak: TextView
    private lateinit var btnLogout: com.google.android.material.button.MaterialButton
    private lateinit var switchNotifications: androidx.appcompat.widget.SwitchCompat

    private val authManager = AuthManager()
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()
        loadUserData()
    }

    private fun initViews(view: View) {
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount)
        tvRecipesTried = view.findViewById(R.id.tvRecipesTried)
        tvCookingStreak = view.findViewById(R.id.tvCookingStreak)
        btnLogout = view.findViewById(R.id.btnLogout)
        switchNotifications = view.findViewById(R.id.switchNotifications)

        // Set up other buttons
        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            showEditProfile()
        }

        view.findViewById<View>(R.id.btnDietaryPreferences).setOnClickListener {
            showDietaryPreferences()
        }

        view.findViewById<View>(R.id.btnHelpSupport).setOnClickListener {
            showHelpSupport()
        }

        view.findViewById<View>(R.id.btnAbout).setOnClickListener {
            showAbout()
        }
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logoutUser()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference(isChecked)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, should not happen but handle gracefully
            return
        }

        // Set basic user info
        tvUserEmail.text = currentUser.email
        tvUserName.text = currentUser.displayName ?: "RecipeMate User"

        // Load user profile from Firestore
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userProfile = authManager.getUserProfile(currentUser.uid)
                if (userProfile != null) {
                    updateUIWithUserProfile(userProfile)
                } else {
                    // Create default profile if doesn't exist
                    createDefaultUserProfile(currentUser.uid)
                }

                // Load favorites count
                loadFavoritesCount(currentUser.uid)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIWithUserProfile(userProfile: UserProfile) {
        tvUserName.text = userProfile.displayName.ifEmpty { "RecipeMate User" }

        // Format member since date
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val memberSince = dateFormat.format(Date(userProfile.createdAt))
        tvMemberSince.text = "Member since $memberSince"

        // Load preferences
        loadUserPreferences()
    }

    private fun createDefaultUserProfile(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userProfile = UserProfile(
                    uid = userId,
                    email = auth.currentUser?.email ?: "",
                    displayName = auth.currentUser?.displayName ?: "",
                    createdAt = System.currentTimeMillis()
                )

                db.collection("users").document(userId).set(userProfile).await()
                updateUIWithUserProfile(userProfile)

            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun loadFavoritesCount(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val favorites = authManager.getFavorites(userId)
                tvFavoritesCount.text = favorites.size.toString()

                // For demo purposes, set some mock data
                tvRecipesTried.text = (favorites.size * 2).toString() // Mock data
                tvCookingStreak.text = "7" // Mock data

            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun loadUserPreferences() {
        // Load notification preference (you can store this in SharedPreferences or Firestore)
        val sharedPreferences = requireContext().getSharedPreferences("recipe_mate_prefs", 0)
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled
    }

    private fun saveNotificationPreference(isEnabled: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("recipe_mate_prefs", 0)
        sharedPreferences.edit().putBoolean("notifications_enabled", isEnabled).apply()

        Toast.makeText(
            requireContext(),
            if (isEnabled) "Notifications enabled" else "Notifications disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEditProfile() {
        Toast.makeText(requireContext(), "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show()
        // You can implement an EditProfileActivity or Dialog here
    }

    private fun showDietaryPreferences() {
        Toast.makeText(requireContext(), "Dietary Preferences - Coming Soon", Toast.LENGTH_SHORT).show()
        // You can implement a DietaryPreferencesActivity here
    }

    private fun showHelpSupport() {
        Toast.makeText(requireContext(), "Help & Support - Coming Soon", Toast.LENGTH_SHORT).show()
        // You can implement a HelpSupportActivity here
    }

    private fun showAbout() {
        Toast.makeText(requireContext(), "About RecipeMate - Coming Soon", Toast.LENGTH_SHORT).show()
        // You can implement an AboutActivity here
    }

    private fun logoutUser() {
        authManager.logout()

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate to LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible again
        loadUserData()
    }
}