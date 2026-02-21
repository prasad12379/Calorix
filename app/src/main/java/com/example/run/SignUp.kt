package com.example.run

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    // ✅ LOADING VIEWS
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSignUpText: TextView
    private lateinit var ivArrow: ImageView

    private lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initViews()
        initRetrofit()
        setupDropdowns()
        setupListeners()
    }

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

        // ✅ BIND LOADING VIEWS
        progressBar = findViewById(R.id.progressBarSignup)
        tvSignUpText = findViewById(R.id.tvSignUpText)
        ivArrow = findViewById(R.id.ivArrowSignup)
    }

    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    private fun setupDropdowns() {
        // Gender Options
        val genderList = listOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderList
        )
        ipGender.setAdapter(genderAdapter)

        // Goal Options
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

    private fun setupListeners() {
        btnSignUp.setOnClickListener {
            collectUserData()
        }

        btnGoToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

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

    private fun sendSignup(userData: UserSignupRequest) {
        // ✅ SHOW LOADING
        showLoading(true)

        val call = apiInterface.signupUser(userData)

        call.enqueue(object : Callback<SignupResponse> {
            override fun onResponse(
                call: Call<SignupResponse>,
                response: Response<SignupResponse>
            ) {
                // ✅ HIDE LOADING
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@SignupActivity,
                        response.body()!!.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Redirect to SignIn
                    startActivity(Intent(this@SignupActivity, SignInActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        "Signup failed: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                // ✅ HIDE LOADING
                showLoading(false)

                Toast.makeText(
                    this@SignupActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ✅ SHOW/HIDE LOADING
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            tvSignUpText.visibility = View.GONE
            ivArrow.visibility = View.GONE
            btnSignUp.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            tvSignUpText.visibility = View.VISIBLE
            ivArrow.visibility = View.VISIBLE
            btnSignUp.isEnabled = true
        }
    }
}