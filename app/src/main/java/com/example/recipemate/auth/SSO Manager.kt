package com.example.recipemate.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SSOManager @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    // Callback interface for Facebook login results
    interface FacebookLoginCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001

        // Development Web Client ID - Replace with your actual one
        // To get this: Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client IDs
        private const val WEB_CLIENT_ID = "871087515687-q9ngv7ug8ogldiejp0sbt74ki4lghqim.apps.googleusercontent.com"
    }

    fun initializeGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d("SSOManager", "Google Sign-In initialized successfully")
        } catch (e: Exception) {
            Log.e("SSOManager", "Failed to initialize Google Sign-In: ${e.message}")
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Boolean {
        try {
            if (data == null) {
                Log.e("SSOManager", "Google Sign-In result data is null")
                return false
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            if (account == null) {
                Log.e("SSOManager", "Google Sign-In account is null")
                return false
            }

            // Debug logging
            Log.d("SSOManager", "Google account: ${account.email}, ID Token: ${account.idToken != null}")

            if (account.idToken == null) {
                Log.e("SSOManager", "Google ID Token is null")
                return false
            }

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()

            // Create or update user profile in Firestore
            result.user?.let { user ->
                saveSSOUserProfile(user)
            }

            Log.d("SSOManager", "Google Sign-In successful: ${result.user != null}")
            return result.user != null

        } catch (e: ApiException) {
            // More detailed error handling for Google Sign-In errors
            val errorMessage = when (e.statusCode) {
                10 -> "DEVELOPMENT_ERROR: Check SHA1 fingerprint and package name in Google Cloud Console"
                7 -> "NETWORK_ERROR: Check internet connection"
                8 -> "INTERNAL_ERROR: Internal Google Sign-In error"
                13 -> "AUTH_ERROR: Authentication failed"
                16 -> "CANCELED: User canceled sign-in"
                12501 -> "SIGN_IN_CANCELLED: User canceled sign-in"
                12502 -> "SIGN_IN_CURRENTLY_IN_PROGRESS: Sign-in already in progress"
                12503 -> "SIGN_IN_FAILED: Sign-in failed"
                else -> "UNKNOWN_ERROR: ${e.statusCode} - ${e.message}"
            }
            Log.e("SSOManager", "Google Sign-In ApiException: $errorMessage")
            return false
        } catch (e: Exception) {
            Log.e("SSOManager", "Google Sign-In general error: ${e.message}")
            return false
        }
    }

    fun initializeFacebookLogin(loginCallback: FacebookLoginCallback? = null) {
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d("SSOManager", "Facebook login successful")
                    handleFacebookAccessToken(loginResult.accessToken, loginCallback)
                }

                override fun onCancel() {
                    Log.d("SSOManager", "Facebook login canceled")
                    loginCallback?.onFailure("Facebook login canceled")
                }

                override fun onError(error: FacebookException) {
                    Log.e("SSOManager", "Facebook login error", error)
                    loginCallback?.onFailure(error.message ?: "Facebook login failed")
                }
            })
    }

    fun handleFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(
        token: AccessToken,
        loginCallback: FacebookLoginCallback? = null
    ) {
        val credential = FacebookAuthProvider.getCredential(token.token)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SSOManager", "Firebase Facebook auth successful")

                    // Use coroutine to save user profile
                    task.result?.user?.let { user ->
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                saveSSOUserProfile(user)
                                loginCallback?.onSuccess()
                            } catch (e: Exception) {
                                Log.e("SSOManager", "Error saving Facebook user profile", e)
                                loginCallback?.onFailure("Failed to save user profile")
                            }
                        }
                    }
                } else {
                    Log.e("SSOManager", "Firebase Facebook auth failed", task.exception)
                    loginCallback?.onFailure(task.exception?.message ?: "Facebook authentication failed")
                }
            }
    }

    private suspend fun saveSSOUserProfile(user: com.google.firebase.auth.FirebaseUser) {
        try {
            val profile = com.example.recipemate.data.model.UserProfile(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "User",
                createdAt = System.currentTimeMillis(),
                isSSOUser = true,
                ssoProvider = when {
                    user.providerData.any { it.providerId == "google.com" } -> "google"
                    user.providerData.any { it.providerId == "facebook.com" } -> "facebook"
                    else -> "unknown"
                }
            )

            firestore.collection("users")
                .document(user.uid)
                .set(profile)
                .await()

            Log.d("SSOManager", "SSO user profile saved successfully for ${user.uid}")
        } catch (e: Exception) {
            Log.e("SSOManager", "Error saving SSO user profile", e)
            throw e
        }
    }

    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("SSOManager", "Google sign-out completed")
        }
        LoginManager.getInstance().logOut()
        auth.signOut()
    }

    fun getRequestCode(): Int {
        return RC_GOOGLE_SIGN_IN
    }

    // Debug method to check Google Sign-In configuration
    fun debugGoogleSignInSetup() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build()

            val client = GoogleSignIn.getClient(context, gso)
            Log.d("SSOManager", "✅ Google Sign-In configuration is valid")
            Log.d("SSOManager", "📱 Package: ${context.packageName}")
            Log.d("SSOManager", "🔑 Web Client ID: $WEB_CLIENT_ID")
        } catch (e: Exception) {
            Log.e("SSOManager", "❌ Google Sign-In configuration failed: ${e.message}")
        }
    }
}