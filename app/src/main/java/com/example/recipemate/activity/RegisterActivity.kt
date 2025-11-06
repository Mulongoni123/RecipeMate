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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var authManager: AuthManager
    private val auth = Firebase.auth

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize AuthManager with context
        authManager = AuthManager(this)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInputs(fullName, email, password, confirmPassword)) {
                registerUser(fullName, email, password)
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            etFullName.requestFocus()
            return false
        }

        if (fullName.length < 2) {
            etFullName.error = "Full name must be at least 2 characters"
            etFullName.requestFocus()
            return false
        }

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

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Please confirm your password"
            etConfirmPassword.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }

        // Enhanced password validation
        if (!isPasswordStrong(password)) {
            etPassword.error = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
            etPassword.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords don't match"
            etConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun isPasswordStrong(password: String): Boolean {
        val pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$".toRegex()
        return pattern.matches(password)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun registerUser(fullName: String, email: String, password: String) {
        showLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = authManager.register(fullName, email, password)

                if (user != null) {
                    Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Auto-login after successful registration
                    val loginResult = authManager.login(email, password)
                    if (loginResult != null) {
                        navigateToMainActivity()
                    } else {
                        finish() // Go back to login if auto-login fails
                    }
                } else {
                    Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("email address is already in use") == true -> "This email is already registered. Please use a different email or try logging in."
                    e.message?.contains("network error") == true -> "Network error. Please check your internet connection."
                    else -> "Registration failed: ${e.message}"
                }
                Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        btnRegister.text = if (show) "Creating Account..." else "Register"
    }
}