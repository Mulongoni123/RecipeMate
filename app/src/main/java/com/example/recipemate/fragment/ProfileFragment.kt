package com.example.recipemate.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.recipemate.R
import com.example.recipemate.activity.LoginActivity
import com.example.recipemate.activity.MainActivity
import com.example.recipemate.auth.AuthManager
import com.example.recipemate.data.model.UserProfile
import com.example.recipemate.dialogs.EditProfileDialog
import com.example.recipemate.dialogs.LanguageDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.example.recipemate.utils.ThemeManager
import java.util.logging.Handler

class ProfileFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var tvRecipesTried: TextView
    private lateinit var tvCookingStreak: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchDataSaver: SwitchCompat
    private lateinit var themeManager: ThemeManager


    private lateinit var authManager: AuthManager
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AuthManager with proper context
        authManager = AuthManager(requireContext())
        themeManager = ThemeManager(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("recipe_mate_prefs", 0)

        initViews(view)
        setupClickListeners()
        loadUserData()
        loadUserPreferences()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initViews(view: View) {
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount)
        tvRecipesTried = view.findViewById(R.id.tvRecipesTried)
        tvCookingStreak = view.findViewById(R.id.tvCookingStreak)
        btnLogout = view.findViewById(R.id.btnLogout)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchDataSaver = view.findViewById(R.id.switchDataSaver)

        // Set up other buttons
        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog()
        }

        view.findViewById<View>(R.id.btnDietaryPreferences).setOnClickListener {
            showDietaryPreferencesDialog()
        }

        view.findViewById<View>(R.id.btnHelpSupport).setOnClickListener {
            showHelpSupport()
        }

        view.findViewById<View>(R.id.btnAbout).setOnClickListener {
            showAboutDialog()
        }

        view.findViewById<View>(R.id.btnPrivacySettings).setOnClickListener {
            showPrivacySettings()
        }
        view.findViewById<View>(R.id.btnLanguageSettings).setOnClickListener {
            showLanguageSettings()
        }
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference(isChecked)
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            saveDarkModePreference(isChecked)
            applyDarkModeImmediately(isChecked)
        }
        switchDataSaver.setOnCheckedChangeListener { _, isChecked ->
            saveDataSaverPreference(isChecked)
        }
    }
    private fun applyDarkModeImmediately(isEnabled: Boolean) {
        val theme = if (isEnabled) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
        themeManager.setTheme(theme)

        // Show restart recommendation for full theme application
        if (requireActivity() is MainActivity) {
            showRestartRecommendation()
        }
    }
    private fun showRestartRecommendation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Theme Changed")
            .setMessage("For the best experience, we recommend restarting the app to fully apply the theme changes.")
            .setPositiveButton("Restart Now") { dialog, _ ->
                restartApp()
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun restartApp() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, redirect to login
            redirectToLogin()
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
                tvRecipesTried.text = (favorites.size * 2).toString()
                tvCookingStreak.text = "7"

            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    // Update loadUserPreferences to set correct dark mode state
    private fun loadUserPreferences() {
        val currentTheme = themeManager.getCurrentTheme()
        val darkModeEnabled = currentTheme == ThemeManager.THEME_DARK

        switchNotifications.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        switchDarkMode.isChecked = darkModeEnabled
        switchDataSaver.isChecked = sharedPreferences.getBoolean("data_saver_enabled", false)
    }
    private fun saveNotificationPreference(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_enabled", isEnabled).apply()
        showPreferenceToast("Notifications", isEnabled)
    }

    private fun saveDarkModePreference(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode_enabled", isEnabled).apply()
        showPreferenceToast("Dark Mode", isEnabled)
        // Note: You would need to restart the app or implement theme switching
        Toast.makeText(requireContext(), "Restart app to see theme changes", Toast.LENGTH_LONG).show()
    }

    private fun saveDataSaverPreference(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("data_saver_enabled", isEnabled).apply()
        showPreferenceToast("Data Saver", isEnabled)
    }

    private fun showPreferenceToast(preferenceName: String, isEnabled: Boolean) {
        val message = "$preferenceName ${if (isEnabled) "enabled" else "disabled"}"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun showEditProfileDialog() {
        val editProfileDialog = EditProfileDialog.newInstance {
            // Refresh profile data after update
            loadUserData()
        }
        editProfileDialog.show(parentFragmentManager, "edit_profile_dialog")
    }

    private fun showDietaryPreferencesDialog() {
        val dietaryOptions = arrayOf("Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free", "Nut-Free", "Keto", "Paleo")
        val checkedItems = booleanArrayOf(false, false, false, false, false, false, false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dietary Preferences")
            .setMultiChoiceItems(dietaryOptions, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Save") { dialog, _ ->
                val selectedPreferences = dietaryOptions.filterIndexed { index, _ -> checkedItems[index] }
                if (selectedPreferences.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Saved: ${selectedPreferences.joinToString()}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No dietary preferences selected", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPrivacySettings() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Privacy Settings")
            .setMessage("""
                Privacy Settings:
                
                • Profile Visibility: Public
                • Recipe Sharing: Enabled
                • Data Collection: Minimal
                • Personalized Ads: Disabled
                
                These settings control how your data is used and shared within the app.
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showHelpSupport() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Help & Support")
            .setMessage("""
                Need help?
                
                • Email: support@recipemate.com
                • FAQ: recipemate.com/help
                • Community: forum.recipemate.com
                
                Our support team is here to help you with any issues or questions.
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About RecipeMate")
            .setMessage("""
                RecipeMate v1.0
                
                Your personal recipe assistant for discovering, saving, and planning meals.
                
                Features:
                • Recipe Search & Discovery
                • Meal Planning
                • Grocery Lists
                • Favorite Recipes
                • Personal Profile
                
                Developed with ❤️ for food lovers.
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    // In ProfileFragment, add this method:
    private fun showLanguageSettings() {
        val languageDialog = LanguageDialog.newInstance {
            // Show confirmation message
            Toast.makeText(
                requireContext(),
                "Language changed. Restarting app...",
                Toast.LENGTH_SHORT
            ).show()

            // Restart activity to apply language changes
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                requireActivity().recreate()
            }, 1000)
        }
        languageDialog.show(parentFragmentManager, "language_dialog")
    }
    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, _ ->
                logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible again
        loadUserData()
    }
}