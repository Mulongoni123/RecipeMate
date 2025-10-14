package com.example.recipemate.auth



import com.example.recipemate.data.model.UserProfile
import com.example.recipemate.data.remote.RecipeSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthManager @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    // ✅ Register new user and save profile in Firestore
    suspend fun register(displayName: String, email: String, password: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user

        user?.let {
            val profile = UserProfile(
                uid = it.uid,
                email = email,
                displayName = displayName,
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(it.uid)
                .set(profile)
                .await()
        }

        return user
    }

    // ✅ Login existing user
    suspend fun login(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user
    }

    // ✅ Logout user
    fun logout() {
        auth.signOut()
    }

    // ✅ Fetch user profile
    suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    // ✅ Update user profile (e.g., name or preferences)
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        firestore.collection("users").document(uid).update(updates).await()
    }

    // ✅ Favorites feature
    suspend fun addFavorite(uid: String, recipe: RecipeSummary) {
        val favorite = hashMapOf(
            "id" to recipe.id,
            "title" to recipe.title,
            "image" to recipe.image,
            "addedAt" to System.currentTimeMillis()
        )
        firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .document(recipe.id.toString())
            .set(favorite)
            .await()
    }

    suspend fun removeFavorite(uid: String, recipeId: Int) {
        firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .document(recipeId.toString())
            .delete()
            .await()
    }

    suspend fun getFavorites(uid: String): List<RecipeSummary> {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val id = doc.getLong("id")?.toInt()
            val title = doc.getString("title")
            val image = doc.getString("image")
            if (id != null && title != null && image != null) {
                RecipeSummary(id, title, image)
            } else null
        }
    }

    // Check if recipe is in favorites
    suspend fun isFavorite(uid: String, recipeId: Int): Boolean {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .document(recipeId.toString())
            .get()
            .await()
        return snapshot.exists()
    }
}