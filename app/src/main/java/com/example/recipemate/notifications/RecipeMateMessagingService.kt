package com.example.recipemate.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RecipeMateMessagingService : FirebaseMessagingService() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManager(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "RecipeMate"
            val body = notification.body ?: "New update available"

            // Use safe notification showing
            notificationManager.showNotificationSafely(title, body)
        }

        // Check if message contains a data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
            // Handle data payload here
            handleDataPayload(remoteMessage.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        // Handle different types of data payloads
        when (data["type"]) {
            "new_recipe" -> {
                val title = data["title"] ?: "New Recipe Available"
                val message = data["message"] ?: "Check out the latest recipe!"
                notificationManager.showNotificationSafely(title, message)
            }
            "meal_reminder" -> {
                val title = data["title"] ?: "Meal Time!"
                val message = data["message"] ?: "Don't forget to plan your meals"
                notificationManager.showNotificationSafely(title, message)
            }
            // Add more types as needed
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
        // Send new token to your server if needed
    }
}