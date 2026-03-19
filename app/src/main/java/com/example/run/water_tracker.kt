package com.example.run

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class water_tracker : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvWaterGoal: TextView
    private lateinit var seekBarWaterGoal: SeekBar
    private lateinit var tvReminderTime: TextView
    private lateinit var ivClockHand: ImageView
    private lateinit var btn1Hour: CardView
    private lateinit var btn2Hours: CardView
    private lateinit var btn3Hours: CardView
    private lateinit var btnDrinkWater: CardView

    private var currentWaterGoal = 2.5
    private var currentReminderMinutes = 2

    companion object {
        const val WORK_TAG = "water_reminder_work"
        const val PREFS_NAME = "water_tracker_prefs"           // ✅ public so BootReceiver can use
        const val KEY_REMINDER_MINUTES = "reminder_minutes"    // ✅ public so BootReceiver can use
        const val KEY_WATER_GOAL = "water_goal"                // ✅ public so ReminderWorker can read
        private const val KEY_WATER_GOAL_ML = "water_goal_ml"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted! Starting reminders...", Toast.LENGTH_SHORT).show()
                scheduleReminder()
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_tracker)

        createNotificationChannel()
        initViews()
        loadSavedPreferences()
        setupListeners()
    }

    private fun initViews() {
        btnBack       = findViewById(R.id.btnBack)
        tvWaterGoal   = findViewById(R.id.tvWaterGoal)
        seekBarWaterGoal = findViewById(R.id.seekBarWaterGoal)
        tvReminderTime = findViewById(R.id.tvReminderTime)
        ivClockHand   = findViewById(R.id.ivClockHand)
        btn1Hour      = findViewById(R.id.btn1Hour)
        btn2Hours     = findViewById(R.id.btn2Hours)
        btn3Hours     = findViewById(R.id.btn3Hours)
        btnDrinkWater = findViewById(R.id.btnDrinkWater)
    }

    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentWaterGoal       = prefs.getFloat(KEY_WATER_GOAL, 2.5f).toDouble()
        currentReminderMinutes = prefs.getInt(KEY_REMINDER_MINUTES, 2)

        val seekProgress = (currentWaterGoal * 10 - 5).toInt().coerceIn(0, seekBarWaterGoal.max)
        seekBarWaterGoal.progress = seekProgress
        tvWaterGoal.text = String.format("%.1f", currentWaterGoal)

        tvReminderTime.text = if (currentReminderMinutes == 1) "1 min" else "$currentReminderMinutes mins"
        ivClockHand.rotation = currentReminderMinutes * 30f
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        seekBarWaterGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentWaterGoal = (progress + 5) / 10.0
                tvWaterGoal.text = String.format("%.1f", currentWaterGoal)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                savePreferences()
                Toast.makeText(
                    this@water_tracker,
                    "Goal set to ${String.format("%.1f", currentWaterGoal)} L",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        btn1Hour.setOnClickListener { updateReminderUI(1) }
        btn2Hours.setOnClickListener { updateReminderUI(2) }
        btn3Hours.setOnClickListener { updateReminderUI(3) }

        btnDrinkWater.setOnClickListener {
            requestNotificationPermissionAndStart()
        }

        ivClockHand.setOnClickListener {
            Toast.makeText(this, "Use buttons to set reminder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateReminderUI(minutes: Int) {
        currentReminderMinutes = minutes
        savePreferences()
        tvReminderTime.text = if (minutes == 1) "1 min" else "$minutes mins"
        ivClockHand.animate().rotation(minutes * 30f).setDuration(300).start()
        Toast.makeText(this, "Reminder every $minutes min(s)", Toast.LENGTH_SHORT).show()
    }

    private fun requestNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                scheduleReminder()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleReminder()
        }
    }

    private fun scheduleReminder() {
        Toast.makeText(this, "Reminder started every $currentReminderMinutes min(s)", Toast.LENGTH_SHORT).show()

        // ✅ Convert goal to ml (e.g. 2.5L → 2500 ml) and pass to worker
        val goalInMl = (currentWaterGoal * 1000).toInt()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_MINUTES to currentReminderMinutes.toLong(),
                    ReminderWorker.KEY_GOAL_ML to goalInMl          // ✅ pass goal to worker
                )
            )
            .setInitialDelay(currentReminderMinutes.toLong(), TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, request)
    }

    fun stopWaterReminders() {
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(WORK_TAG)
    }

    fun savePreferences() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putFloat(KEY_WATER_GOAL, currentWaterGoal.toFloat())
            .putInt(KEY_REMINDER_MINUTES, currentReminderMinutes)
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Water Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}