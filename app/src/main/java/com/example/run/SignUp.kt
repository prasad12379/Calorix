package com.example.run

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class SignupActivity : AppCompatActivity() {

    private lateinit var ipUsername: EditText
    private lateinit var ipEmail: EditText
    private lateinit var ipPassword: EditText
    private lateinit var ipAge: EditText
    private lateinit var ipGender: AutoCompleteTextView
    private lateinit var ipHeight: EditText
    private lateinit var ipWeight: EditText
    private lateinit var ipGoal: AutoCompleteTextView
    private lateinit var cbTerms: CheckBox
    private lateinit var btnSignUp: CardView
    private lateinit var btnGoToSignIn: TextView

    private lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initViews()
        initRetrofit()
        setupDropdowns()   // ⭐ DROPDOWN FIX
        setupListeners()
    }

    // =========================
    // 🔗 Bind XML Views
    // =========================
    private fun initViews() {
        ipUsername = findViewById(R.id.ipUsername)
        ipEmail = findViewById(R.id.ipEmailR)
        ipPassword = findViewById(R.id.ipPasswordR)
        ipAge = findViewById(R.id.ipAge)
        ipGender = findViewById(R.id.ipGender)
        ipHeight = findViewById(R.id.ipHeight)
        ipWeight = findViewById(R.id.ipWeight)
        ipGoal = findViewById(R.id.ipGoal)
        cbTerms = findViewById(R.id.cbTerms)
        btnSignUp = findViewById(R.id.btnSignUpR)
        btnGoToSignIn = findViewById(R.id.btnGoToSignIn)
    }

    // =========================
    // 🌐 Retrofit Setup
    // =========================
    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    // =========================
    // ⭐ Dropdown Logic (FIXED)
    // =========================
    private fun setupDropdowns() {

        // Gender Options
        val genderList = listOf("Male", "Female", "Other")

        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderList
        )

        ipGender.setAdapter(genderAdapter)

        // Goal Options (UI Only)
        val goalList = listOf(
            "Fat Loss",
            "Muscle Gain",
            "Maintain Fitness",
            "Improve Stamina"
        )

        val goalAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            goalList
        )

        ipGoal.setAdapter(goalAdapter)

        // Force dropdown open on click
        ipGender.setOnClickListener { ipGender.showDropDown() }
        ipGoal.setOnClickListener { ipGoal.showDropDown() }
    }

    // =========================
    // 🖱️ Click Listeners
    // =========================
    private fun setupListeners() {

        btnSignUp.setOnClickListener {
            collectUserData()
        }

        btnGoToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    // =========================
    // 📥 Collect User Data
    // =========================
    private fun collectUserData() {

        val name = ipUsername.text.toString().trim()
        val email = ipEmail.text.toString().trim()
        val password = ipPassword.text.toString().trim()
        val ageText = ipAge.text.toString().trim()
        val gender = ipGender.text.toString().trim()
        val heightText = ipHeight.text.toString().trim()
        val weightText = ipWeight.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()
            || ageText.isEmpty() || gender.isEmpty()
            || heightText.isEmpty() || weightText.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Accept Terms & Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        val userData = UserSignupRequest(
            name = name,
            email = email,
            password = password,
            age = ageText.toInt(),
            gender = gender,
            height = heightText.toDouble(),
            weight = weightText.toDouble()
        )

        sendSignup(userData)
    }

    // =========================
    // 🚀 POST /signup API CALL
    // =========================
    private fun sendSignup(userData: UserSignupRequest) {

        val call = apiInterface.signupUser(userData)

        call.enqueue(object : Callback<SignupResponse> {

            override fun onResponse(
                call: Call<SignupResponse>,
                response: Response<SignupResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        this@SignupActivity,
                        response.body()!!.message,
                        Toast.LENGTH_LONG
                    ).show()

                    // 👉 Redirect to SignIn
                    startActivity(Intent(this@SignupActivity, SignInActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        "Signup failed: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Toast.makeText(
                    this@SignupActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
