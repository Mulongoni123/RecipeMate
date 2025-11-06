package com.example.recipemate.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.recipemate.R
import com.example.recipemate.auth.AuthManager
import com.example.recipemate.auth.SSOManager
import com.facebook.login.LoginManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvSignUp: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnFacebookSignIn: MaterialButton

    private lateinit var authManager: AuthManager
    private lateinit var ssoManager: SSOManager
    private val auth = Firebase.auth

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize AuthManager with context
        authManager = AuthManager(this)
        ssoManager = SSOManager(this)

        initViews()
        setupSSO()
        setupClickListeners()

        // Check if user is already logged in
        checkCurrentUser()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        progressBar = findViewById(R.id.progressBar)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn)
    }

    private fun setupSSO() {
        ssoManager.initializeGoogleSignIn()
        ssoManager.initializeFacebookLogin()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Google Sign In
        btnGoogleSignIn.setOnClickListener {
            val signInIntent = ssoManager.getGoogleSignInIntent()
            startActivityForResult(signInIntent, ssoManager.getRequestCode())
        }

        // Facebook Sign In
        btnFacebookSignIn.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ssoManager.getRequestCode() -> {
                handleGoogleSignInResult(data)
            }
            else -> {
                // Handle Facebook login result
                if (!ssoManager.handleFacebookActivityResult(requestCode, resultCode, data)) {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        showLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = ssoManager.handleGoogleSignInResult(data)
                if (success) {
                    Toast.makeText(this@LoginActivity, "Google sign-in successful!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this@LoginActivity, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return false
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun loginUser(email: String, password: String) {
        showLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = authManager.login(email, password)

                if (user != null) {
                    // Login successful
                    Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, redirect to MainActivity
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnLogin.text = if (show) "Logging in..." else "Login"
        btnGoogleSignIn.isEnabled = !show
        btnFacebookSignIn.isEnabled = !show
    }
}