package com.example.recipemate.auth

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.recipemate.data.model.UserProfile
import com.example.recipemate.data.remote.RecipeSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

class AuthManager @Inject constructor(
    private val context: Context
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "RecipeMate_Encryption_Key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val HASH_ALGORITHM = "SHA-256"
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    // Password hashing for additional security layer
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    // Android Keystore for secure key storage
    @RequiresApi(Build.VERSION_CODES.P)
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            createSecretKey()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setIsStrongBoxBacked(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    // Encrypt sensitive data
    @RequiresApi(Build.VERSION_CODES.P)
    fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Combine IV + encrypted data
            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("AuthManager", "Encryption error", e)
            throw RuntimeException("Encryption failed")
        }
    }

    // Decrypt sensitive data
    @RequiresApi(Build.VERSION_CODES.P)
    fun decrypt(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = combined.copyOfRange(0, 12) // GCM IV is 12 bytes
            val encrypted = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("AuthManager", "Decryption error", e)
            throw RuntimeException("Decryption failed")
        }
    }

    // ✅ Enhanced Register with encrypted profile data
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun register(displayName: String, email: String, password: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user

        user?.let {
            // Encrypt sensitive user data
            val encryptedDisplayName = encrypt(displayName)
            val passwordHash = hashPassword(password) // Store hash for additional verification

            val profile = UserProfile(
                uid = it.uid,
                email = email,
                displayName = encryptedDisplayName, // Store encrypted
                passwordHash = passwordHash, // Store hashed password
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(it.uid)
                .set(profile)
                .await()
        }

        return user
    }

    // ✅ Enhanced Login with additional security checks
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun login(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()

        // Additional security: Verify password hash matches stored hash
        result.user?.let { user ->
            try {
                val userProfile = getUserProfile(user.uid)
                userProfile?.let { profile ->
                    val inputPasswordHash = hashPassword(password)
                    if (profile.passwordHash != inputPasswordHash) {
                        // This indicates potential security issue
                        Log.w("AuthManager", "Password hash mismatch for user: ${user.uid}")
                        // You might want to force re-authentication here
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Error verifying password hash", e)
            }
        }

        return result.user
    }

    // ✅ Enhanced Get User Profile with decryption
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val profile = snapshot.toObject(UserProfile::class.java)

        // Decrypt display name
        profile?.let {
            try {
                it.displayName = decrypt(it.displayName)
            } catch (e: Exception) {
                Log.e("AuthManager", "Error decrypting user profile", e)
            }
        }

        return profile
    }

    // ✅ Enhanced Update User Profile with encryption
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        val encryptedUpdates = mutableMapOf<String, Any>()

        updates.forEach { (key, value) ->
            encryptedUpdates[key] = when (key) {
                "displayName" -> encrypt(value.toString())
                else -> value
            }
        }

        firestore.collection("users").document(uid).update(encryptedUpdates).await()
    }

    // ... rest of your existing methods (favorites) remain the same
    suspend fun addFavorite(uid: String, recipe: RecipeSummary) {
        try {
            Log.d("AuthManager", "=== ADD FAVORITE START ===")
            Log.d("AuthManager", "User: $uid")
            Log.d("AuthManager", "Recipe: ${recipe.title} (ID: ${recipe.id})")

            val favoriteData = hashMapOf(
                "id" to recipe.id,
                "title" to recipe.title,
                "image" to recipe.image,
                "addedAt" to System.currentTimeMillis()
            )

            Log.d("AuthManager", "Favorite data: $favoriteData")

            // Create the document reference
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .document(recipe.id.toString())

            Log.d("AuthManager", "Document path: ${docRef.path}")

            // Set the data
            docRef.set(favoriteData).await()

            Log.d("AuthManager", "✅ FAVORITE ADDED SUCCESSFULLY!")
            Log.d("AuthManager", "=== ADD FAVORITE COMPLETE ===")

        } catch (e: Exception) {
            Log.e("AuthManager", "❌ FAVORITE ADD FAILED!", e)
            Log.e("AuthManager", "Error: ${e.message}")
            Log.e("AuthManager", "Error type: ${e.javaClass.simpleName}")
            throw e
        }
    }

    suspend fun getFavorites(uid: String): List<RecipeSummary> {
        try {
            Log.d("AuthManager", "📖 Loading favorites from subcollection for user: $uid")

            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("favorites") // Access the subcollection
                .get()
                .await()

            Log.d("AuthManager", "Found ${snapshot.documents.size} favorites in subcollection")

            snapshot.documents.forEach { doc ->
                Log.d("AuthManager", "Favorite doc: ${doc.id} - ${doc.data}")
            }

            val favorites = snapshot.documents.mapNotNull { doc ->
                val id = doc.getLong("id")?.toInt()
                val title = doc.getString("title")
                val image = doc.getString("image")
                if (id != null && title != null && image != null) {
                    RecipeSummary(id, title, image)
                } else {
                    Log.w("AuthManager", "⚠️ Invalid favorite document: ${doc.id}")
                    null
                }
            }

            return favorites

        } catch (e: Exception) {
            Log.e("AuthManager", "❌ ERROR loading favorites: ${e.message}", e)
            return emptyList()
        }
    }
    suspend fun removeFavorite(uid: String, recipeId: Int) {
        try {
            Log.d("AuthManager", "=== REMOVE FAVORITE START ===")
            Log.d("AuthManager", "Removing recipe ID: $recipeId from user: $uid")

            val docRef = firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .document(recipeId.toString())

            docRef.delete().await()

            Log.d("AuthManager", "✅ FAVORITE REMOVED SUCCESSFULLY!")
            Log.d("AuthManager", "=== REMOVE FAVORITE COMPLETE ===")

        } catch (e: Exception) {
            Log.e("AuthManager", "❌ FAVORITE REMOVE FAILED!", e)
            throw e
        }
    }


    suspend fun isFavorite(uid: String, recipeId: Int): Boolean {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .document(recipeId.toString())
            .get()
            .await()
        return snapshot.exists()
    }

    // ✅ Logout user
    fun logout() {
        auth.signOut()
    }
}