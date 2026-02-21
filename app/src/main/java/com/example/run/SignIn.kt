package com.example.run

import UserResponse
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class SignInActivity : AppCompatActivity() {

    private lateinit var ipEmail: EditText
    private lateinit var ipPassword: EditText
    private lateinit var btnSignIn: CardView

    // ✅ LOADING VIEWS
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSignInText: TextView
    private lateinit var ivArrow: ImageView

    private lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        ipEmail = findViewById(R.id.ipEmail)
        ipPassword = findViewById(R.id.ipPassword)
        btnSignIn = findViewById(R.id.btnSignIn)

        // ✅ BIND LOADING VIEWS
        progressBar = findViewById(R.id.progressBar)
        tvSignInText = findViewById(R.id.tvSignInText)
        ivArrow = findViewById(R.id.ivArrow)

        val btn = findViewById<TextView>(R.id.btnCreateAccount)

        btn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        initRetrofit()

        btnSignIn.setOnClickListener {
            loginUser()
        }
    }

    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    private fun loginUser() {
        val email = ipEmail.text.toString().trim()
        val password = ipPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ SHOW LOADING
        showLoading(true)

        val request = SigninRequest(email, password)
        val call = apiInterface.signinUser(request)

        call.enqueue(object : Callback<SigninResponse> {
            override fun onResponse(
                call: Call<SigninResponse>,
                response: Response<SigninResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val userEmail = response.body()!!.email

                    Toast.makeText(
                        this@SignInActivity,
                        response.body()!!.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    // SAVE EMAIL
                    val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                    sharedPref.edit().putString("email", userEmail).apply()

                    // FETCH AND CACHE USER PROFILE DATA (keeps loading visible)
                    fetchAndCacheUserProfile(userEmail)

                } else {
                    // ✅ HIDE LOADING ON ERROR
                    showLoading(false)

                    Toast.makeText(
                        this@SignInActivity,
                        "Invalid credentials",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<SigninResponse>, t: Throwable) {
                // ✅ HIDE LOADING ON FAILURE
                showLoading(false)

                Toast.makeText(
                    this@SignInActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun fetchAndCacheUserProfile(email: String) {
        val call = apiInterface.getUser(email)

        call.enqueue(object : Callback<UserResponse> {
            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                // ✅ HIDE LOADING
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.data

                    // SAVE ALL USER DATA TO SHARED PREFERENCES
                    val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("name", user.name)
                        putString("email", user.email)
                        putString("height", user.height.toString())
                        putString("weight", user.weight.toString())
                        putString("age", user.age.toString())
                        putString("created_at", user.created_at)
                        putBoolean("profile_cached", true)
                        apply()
                    }

                    // NOW OPEN MAIN ACTIVITY
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(
                        this@SignInActivity,
                        "Failed to load profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Still open MainActivity
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // ✅ HIDE LOADING
                showLoading(false)

                Toast.makeText(
                    this@SignInActivity,
                    "Profile fetch error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Still open MainActivity
                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }

    // ✅ SHOW/HIDE LOADING
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            tvSignInText.visibility = View.GONE
            ivArrow.visibility = View.GONE
            btnSignIn.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            tvSignInText.visibility = View.VISIBLE
            ivArrow.visibility = View.VISIBLE
            btnSignIn.isEnabled = true
        }
    }
}