package com.example.recipemate.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.recipemate.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initViews()
        startAnimations()
        navigateToNextScreen()
    }

    private fun initViews() {
        logo = findViewById(R.id.logo)
    }

    private fun startAnimations() {
        logo.scaleX = 0.7f
        logo.scaleY = 0.7f
        logo.alpha = 0f

        logo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(1200)
            .setStartDelay(300)
            .start()
    }

    private fun navigateToNextScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser

            val nextIntent = if (currentUser != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(nextIntent)
            finish()
        }, 2000)
    }
}