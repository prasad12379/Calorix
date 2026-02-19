package com.example.run

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
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

    private lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        ipEmail = findViewById(R.id.ipEmail)
        ipPassword = findViewById(R.id.ipPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        val btn=findViewById<TextView>(R.id.btnCreateAccount)
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

        val request = SigninRequest(email, password)

        val call = apiInterface.signinUser(request)

        call.enqueue(object : Callback<SigninResponse> {

            override fun onResponse(
                call: Call<SigninResponse>,
                response: Response<SigninResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        this@SignInActivity,
                        response.body()!!.message,
                        Toast.LENGTH_LONG
                    ).show()

                    // 🚀 START MAIN ACTIVITY AFTER SUCCESS LOGIN
                    val userEmail = response.body()!!.email

// ⭐ SAVE EMAIL GLOBALLY
                    val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("email", userEmail)
                    editor.apply()

// 🚀 OPEN MAIN ACTIVITY
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()


                } else {
                    Toast.makeText(
                        this@SignInActivity,
                        "Invalid credentials",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<SigninResponse>, t: Throwable) {
                Toast.makeText(
                    this@SignInActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}