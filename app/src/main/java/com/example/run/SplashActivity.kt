package com.example.run

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 5000L // 5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Navigate to next screen after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION)
    }

    private fun navigateToNextScreen() {
        // Check if user is logged in
        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getString("email", null) != null

        val intent = if (isLoggedIn) {
            // Go to MainActivity if logged in
            Intent(this, MainActivity::class.java)
        } else {
            // Go to SignInActivity if not logged in
            Intent(this, SignInActivity::class.java)
        }

        startActivity(intent)
        finish() // Close splash activity

        // Optional: Add smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}