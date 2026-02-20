package com.example.run

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import ActivityRequest

class WorkoutSummaryActivity : AppCompatActivity() {

    // ✅ MUST BE INSIDE CLASS
    private lateinit var apiInterface: ApiInterface

    private lateinit var tvWorkoutType: TextView
    private lateinit var tvWorkoutDate: TextView
    private lateinit var tvSummaryTime: TextView
    private lateinit var tvSummaryDistance: TextView
    private lateinit var tvSummaryCalories: TextView
    private lateinit var tvSummaryPace: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvSteps: TextView
    private lateinit var ivRouteMap: ImageView

    private lateinit var btnSave: CardView
    private lateinit var btnDiscard: CardView
    private lateinit var btnShare: CardView
    private lateinit var btnViewFullMap: CardView

    private var workoutMode = ""
    private var duration = ""
    private var distance = ""
    private var calories = ""
    private var pace = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_summary)

        // ✅ Initialize Retrofit FIRST
        initRetrofit()

        initViews()
        getDataFromIntent()
        displayData()
        setupListeners()
    }

    // ⭐ Retrofit Setup (OUTSIDE onCreate)
    private fun initRetrofit() {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    private fun initViews() {
        tvWorkoutType = findViewById(R.id.tvWorkoutType)
        tvWorkoutDate = findViewById(R.id.tvWorkoutDate)
        tvSummaryTime = findViewById(R.id.tvSummaryTime)
        tvSummaryDistance = findViewById(R.id.tvSummaryDistance)
        tvSummaryCalories = findViewById(R.id.tvSummaryCalories)
        tvSummaryPace = findViewById(R.id.tvSummaryPace)
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed)
        tvSteps = findViewById(R.id.tvSteps)
        ivRouteMap = findViewById(R.id.ivRouteMap)

        btnSave = findViewById(R.id.btnSave)
        btnDiscard = findViewById(R.id.btnDiscard)
        btnShare = findViewById(R.id.btnShare)
        btnViewFullMap = findViewById(R.id.btnViewFullMap)
    }

    private fun getDataFromIntent() {
        workoutMode = intent.getStringExtra("WORKOUT_MODE") ?: "Running"
        duration = intent.getStringExtra("DURATION") ?: "00:00:00"
        distance = intent.getStringExtra("DISTANCE") ?: "0.00"
        calories = intent.getStringExtra("CALORIES") ?: "0"
        pace = intent.getStringExtra("PACE") ?: "0:00"
    }

    private fun displayData() {

        tvWorkoutType.text = when(workoutMode) {
            "RUNNING" -> "Morning Run"
            "WALKING" -> "Walking Session"
            "CYCLING" -> "Cycling Ride"
            else -> "Workout"
        }

        val dateFormat = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault())
        tvWorkoutDate.text = dateFormat.format(Date())

        tvSummaryTime.text = duration
        tvSummaryDistance.text = distance
        tvSummaryCalories.text = calories
        tvSummaryPace.text = pace

        calculateBestPace()
        calculateEstimatedSteps()
    }

    private fun calculateBestPace() {
        try {
            val parts = pace.split(":")
            if (parts.size == 2) {
                val totalSeconds =
                    (parts[0].toInt() * 60 + parts[1].toInt()) * 0.9
                tvMaxSpeed.text =
                    "%d:%02d".format((totalSeconds/60).toInt(), (totalSeconds%60).toInt())
            }
        } catch (e: Exception) {
            tvMaxSpeed.text = "N/A"
        }
    }

    private fun calculateEstimatedSteps() {
        try {
            val steps = (distance.toDouble() * 1300).toInt()
            tvSteps.text = String.format("%,d", steps)
        } catch (e: Exception) {
            tvSteps.text = "0"
        }
    }

    private fun setupListeners() {

        btnSave.setOnClickListener { saveWorkout() }

        btnDiscard.setOnClickListener { showDiscardDialog() }

        btnShare.setOnClickListener { shareWorkout() }

        btnViewFullMap.setOnClickListener {
            Toast.makeText(this, "Map view coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWorkout() {

        val email = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
            .getString("email", null)

        if (email == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val activity = ActivityRequest(
            email,
            workoutMode,
            duration,
            distance,
            calories,
            pace,
            tvSteps.text.toString().replace(",", "").toIntOrNull() ?: 0,
            tvMaxSpeed.text.toString(),
            tvWorkoutDate.text.toString()
        )

        apiInterface.saveActivity(activity)
            .enqueue(object : Callback<SimpleResponse> {

                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {

                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@WorkoutSummaryActivity,
                            response.body()!!.message,
                            Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@WorkoutSummaryActivity,
                            "Failed to save workout",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    Toast.makeText(this@WorkoutSummaryActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDiscardDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Workout?")
            .setMessage("Are you sure you want to discard this workout?")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareWorkout() {
        val shareText = """
            🏃 Workout Summary
            Type: $workoutMode
            Time: $duration
            Distance: $distance km
            Calories: $calories kcal
            Avg Pace: $pace min/km
        """.trimIndent()

        startActivity(android.content.Intent.createChooser(
            android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            },
            "Share Workout"
        ))
    }
}
