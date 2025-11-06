package com.example.recipemate.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater

import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.recipemate.R
import com.example.recipemate.auth.AuthManager

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileDialog : DialogFragment() {

    private lateinit var authManager: AuthManager
    private var onProfileUpdated: (() -> Unit)? = null

    companion object {
        fun newInstance(onProfileUpdated: () -> Unit): EditProfileDialog {
            return EditProfileDialog().apply {
                this.onProfileUpdated = onProfileUpdated
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        authManager = AuthManager(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_edit_profile, null)

        val etDisplayName = view.findViewById<EditText>(R.id.etDisplayName)
        val ivProfilePicture = view.findViewById<ImageView>(R.id.ivProfilePicture)

        // Load current user data
        loadCurrentUserData(etDisplayName, ivProfilePicture)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Edit Profile")
            .setPositiveButton("Save") { dialog, _ ->
                val newDisplayName = etDisplayName.text.toString().trim()
                if (newDisplayName.isNotEmpty()) {
                    updateUserProfile(newDisplayName)
                } else {
                    Toast.makeText(requireContext(), "Please enter a display name", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun loadCurrentUserData(etDisplayName: EditText, ivProfilePicture: ImageView) {
        val currentUser = Firebase.auth.currentUser
        currentUser?.let { user ->
            etDisplayName.setText(user.displayName ?: "")

            // Load profile picture if available
            user.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfilePicture)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateUserProfile(displayName: String) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Update Firebase Auth profile
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                currentUser.updateProfile(profileUpdates).await()

                // Update Firestore profile
                val updates = mapOf(
                    "displayName" to displayName
                )
                authManager.updateUserProfile(currentUser.uid, updates)

                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                onProfileUpdated?.invoke()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}