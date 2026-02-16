package com.example.run

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*

class WorkoutSummaryActivity : AppCompatActivity() {

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

    // Workout data
    private var workoutMode = ""
    private var duration = ""
    private var distance = ""
    private var calories = ""
    private var pace = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_summary)

        initViews()
        getDataFromIntent()
        displayData()
        setupListeners()
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
        // Set workout type
        tvWorkoutType.text = when(workoutMode) {
            "RUNNING" -> "Morning Run"
            "WALKING" -> "Walking Session"
            "CYCLING" -> "Cycling Ride"
            else -> "Workout"
        }

        // Set current date and time
        val dateFormat = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())
        tvWorkoutDate.text = currentDateTime

        // Set main stats
        tvSummaryTime.text = duration
        tvSummaryDistance.text = distance
        tvSummaryCalories.text = calories
        tvSummaryPace.text = pace

        // Calculate best pace (approximately 10% faster than average)
        calculateBestPace()

        // Calculate estimated steps
        calculateEstimatedSteps()
    }

    private fun calculateBestPace() {
        try {
            val parts = pace.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toInt()
                val seconds = parts[1].toInt()
                val totalSeconds = (minutes * 60 + seconds) * 0.9 // 10% faster
                val bestMin = (totalSeconds / 60).toInt()
                val bestSec = (totalSeconds % 60).toInt()
                tvMaxSpeed.text = String.format("%d:%02d", bestMin, bestSec)
            }
        } catch (e: Exception) {
            tvMaxSpeed.text = "N/A"
        }
    }

    private fun calculateEstimatedSteps() {
        try {
            val distKm = distance.toDouble()
            // Average: 1,300 steps per km
            val steps = (distKm * 1300).toInt()
            tvSteps.text = String.format("%,d", steps)
        } catch (e: Exception) {
            tvSteps.text = "0"
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveWorkout()
        }

        btnDiscard.setOnClickListener {
            showDiscardDialog()
        }

        btnShare.setOnClickListener {
            shareWorkout()
        }

        btnViewFullMap.setOnClickListener {
            // TODO: Open full map view
            Toast.makeText(this, "Map view coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWorkout() {
        // TODO: Save to database
        Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show()

        // Return to home
        finish()
    }

    private fun showDiscardDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Workout?")
            .setMessage("Are you sure you want to discard this workout? This action cannot be undone.")
            .setPositiveButton("Discard") { _, _ ->
                Toast.makeText(this, "Workout discarded", Toast.LENGTH_SHORT).show()
                finish()
            }
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
            
            #RunTracker #Fitness
        """.trimIndent()

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }

        startActivity(android.content.Intent.createChooser(shareIntent, "Share Workout"))
    }

    // Prevent back button


}