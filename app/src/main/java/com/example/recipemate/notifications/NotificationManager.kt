package com.example.recipemate.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.recipemate.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "recipe_mate_notifications"
        const val CHANNEL_NAME = "Recipe Mate Notifications"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new recipes, meal reminders, and cooking tips"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Check if notification permission is granted
    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires explicit permission
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13, notifications are enabled by default
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    // Show notification with permission check
    fun showNotification(title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        if (!areNotificationsEnabled()) {
            // Notifications are not enabled, you might want to log this or request permission
            return
        }

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))

            with(NotificationManagerCompat.from(context)) {
                // Double-check permission before showing notification
                if (areNotificationsEnabled()) {
                    notify(notificationId, builder.build())
                }
            }
        } catch (e: SecurityException) {
            // Handle the case where permission was revoked between check and show
            // You might want to request permission again
        }
    }

    // Safe version that returns success status
    fun showNotificationSafely(title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()): Boolean {
        return try {
            if (!areNotificationsEnabled()) {
                false
            } else {
                showNotification(title, message, notificationId)
                true
            }
        } catch (e: SecurityException) {
            false
        }
    }

    suspend fun subscribeToTopics() {
        try {
            // Subscribe to general topics
            FirebaseMessaging.getInstance().subscribeToTopic("all_users").await()
            FirebaseMessaging.getInstance().subscribeToTopic("new_recipes").await()
            FirebaseMessaging.getInstance().subscribeToTopic("cooking_tips").await()
        } catch (e: Exception) {
            // Handle subscription error
        }
    }

    suspend fun unsubscribeFromTopics() {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users").await()
        } catch (e: Exception) {
            // Handle unsubscription error
        }
    }
}