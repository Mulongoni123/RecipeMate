package com.example.recipemate.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.recipemate.R
import com.example.recipemate.fragment.*
import com.example.recipemate.notifications.NotificationManager
import com.example.recipemate.utils.LanguageManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var notificationManager: NotificationManager

    // Notification permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - show welcome notification
            showWelcomeNotification()
            // Subscribe to topics for future notifications
            CoroutineScope(Dispatchers.IO).launch {
                notificationManager.subscribeToTopics()
            }
        } else {
            // Permission denied - handle accordingly
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageManager(this).applySavedLanguage()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Initialize NotificationManager
        notificationManager = NotificationManager(this)

        // Check if user is authenticated
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            // Redirect to login if not authenticated
            finish()
            return
        }

        initViews()
        setupBottomNavigation()

        // Request notification permission
        checkAndRequestNotificationPermission()

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Schedule daily meal reminder (at 8:00 AM)
        scheduleDailyMealReminder()
    }

    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }

                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }

                R.id.nav_favorites -> {
                    loadFragment(FavoritesFragment())
                    true
                }

                R.id.nav_meal_planner -> {
                    loadFragment(MealPlannerFragment())
                    true
                }

                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkAndRequestNotificationPermission() {
        // Only need to request permission on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // Permission already granted
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, show welcome notification
                    showWelcomeNotification()
                    // Subscribe to topics
                    CoroutineScope(Dispatchers.IO).launch {
                        notificationManager.subscribeToTopics()
                    }
                }

                // Should show explanation why permission is needed
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationaleDialog()
                }

                // First time requesting or permanently denied
                else -> {
                    // Request the permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Below Android 13, no permission needed - show welcome notification
            showWelcomeNotification()
            CoroutineScope(Dispatchers.IO).launch {
                notificationManager.subscribeToTopics()
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Enable Notifications")
            .setMessage("RecipeMate uses notifications to:\n• Remind you about meal times\n• Suggest new recipes\n• Alert you about cooking tips\n• Notify about app updates\n\nAllow notifications for the best cooking experience!")
            .setPositiveButton("Allow") { dialog, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                dialog.dismiss()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showWelcomeNotification() {
        // Show welcome notification when app starts
        notificationManager.showNotificationSafely(
            "Welcome to RecipeMate! 🍳",
            "Start exploring delicious recipes and plan your meals."
        )

        // Show a cooking tip after 10 seconds
        android.os.Handler(mainLooper).postDelayed({
            showRandomCookingTip()
        }, 10000)
    }

    private fun showRandomCookingTip() {
        val cookingTips = listOf(
            "💡 Tip: Always taste your food while cooking!",
            "👨‍🍳 Tip: Let meat rest before slicing for juicier results",
            "🥦 Tip: Don't overcrowd the pan when roasting vegetables",
            "🧂 Tip: Season in layers for better flavor development",
            "🌡️ Tip: Use a meat thermometer for perfect cooking",
            "🥘 Tip: Prep all ingredients before you start cooking"
        )

        val randomTip = cookingTips.random()
        notificationManager.showNotificationSafely(
            "Cooking Tip of the Day",
            randomTip
        )
    }

    private fun scheduleDailyMealReminder() {
        // This would use WorkManager for scheduled notifications
        // For now, we'll show a demo notification after 30 seconds
        android.os.Handler(mainLooper).postDelayed({
            showMealReminder()
        }, 30000)
    }

    private fun showMealReminder() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val mealType = when (currentHour) {
            in 6..10 -> "breakfast"
            in 11..14 -> "lunch"
            in 17..21 -> "dinner"
            else -> "meal"
        }

        notificationManager.showNotificationSafely(
            "Time to plan your $mealType! 🍽️",
            "Check out new recipes or review your meal plan."
        )
    }

    // You can call this from other parts of your app
    fun showNewRecipeNotification(recipeTitle: String) {
        notificationManager.showNotificationSafely(
            "New Recipe Added! 📝",
            "Check out: $recipeTitle"
        )
    }

    fun showMealPlanReminder() {
        notificationManager.showNotificationSafely(
            "Meal Plan Update 🔔",
            "Don't forget to plan your meals for tomorrow!"
        )
    }

    fun showFavoriteRecipeNotification(recipeTitle: String) {
        notificationManager.showNotificationSafely(
            "Recipe Saved! ❤️",
            "$recipeTitle added to your favorites"
        )
    }
}