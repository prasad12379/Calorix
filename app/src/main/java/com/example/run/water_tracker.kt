package com.example.run

import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class water_tracker : AppCompatActivity() {

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var tvWaterGoal: TextView
    private lateinit var seekBarWaterGoal: SeekBar
    private lateinit var tvReminderTime: TextView
    private lateinit var ivClockHand: ImageView
    private lateinit var btn1Hour: CardView
    private lateinit var btn2Hours: CardView
    private lateinit var btn3Hours: CardView
    private lateinit var btnDrinkWater: CardView

    // Current values
    private var currentWaterGoal = 2.5 // Default 2.5 liters
    private var currentReminderHours = 2 // Default 2 hours

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_tracker)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvWaterGoal = findViewById(R.id.tvWaterGoal)
        seekBarWaterGoal = findViewById(R.id.seekBarWaterGoal)
        tvReminderTime = findViewById(R.id.tvReminderTime)
        ivClockHand = findViewById(R.id.ivClockHand)
        btn1Hour = findViewById(R.id.btn1Hour)
        btn2Hours = findViewById(R.id.btn2Hours)
        btn3Hours = findViewById(R.id.btn3Hours)
        btnDrinkWater = findViewById(R.id.btnDrinkWater)
    }

    private fun setupListeners() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Water goal slider
        seekBarWaterGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Convert progress (5-80) to liters (0.5-8.0)
                currentWaterGoal = (progress + 5) / 10.0
                tvWaterGoal.text = String.format("%.1f", currentWaterGoal)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional: Add animation or feedback when user starts dragging
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional: Save the goal to SharedPreferences
                Toast.makeText(
                    this@water_tracker,
                    "Goal set to ${String.format("%.1f", currentWaterGoal)} L",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Reminder time preset buttons
        btn1Hour.setOnClickListener {
            setReminderInterval(1)
        }

        btn2Hours.setOnClickListener {
            setReminderInterval(2)
        }

        btn3Hours.setOnClickListener {
            setReminderInterval(3)
        }

        // Drink water button
        btnDrinkWater.setOnClickListener {
            Toast.makeText(
                this,
                "Added 250ml to your intake!",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: Implement actual water intake tracking
        }

        // Clock hand touch interaction (optional advanced feature)
        setupClockHandInteraction()
    }

    private fun setReminderInterval(hours: Int) {
        currentReminderHours = hours

        // Update text
        tvReminderTime.text = if (hours == 1) "1 hr" else "$hours hrs"

        // Rotate clock hand
        // 360 degrees / 12 hours = 30 degrees per hour
        val rotation = (hours * 30).toFloat()
        ivClockHand.animate()
            .rotation(rotation)
            .setDuration(300)
            .start()

        // Provide feedback
        Toast.makeText(
            this,
            "Reminder set to every ${if (hours == 1) "hour" else "$hours hours"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupClockHandInteraction() {
        // Optional: Add touch listener to clock hand for drag-to-rotate
        // This would allow users to drag the clock hand to set custom intervals
        // For now, we're using the preset buttons

        ivClockHand.setOnClickListener {
            Toast.makeText(
                this,
                "Use the buttons below to set reminder interval",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: Save settings to SharedPreferences if needed
    }
}