package com.example.run

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
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
    private lateinit var btnStopReminder: CardView
    private lateinit var cardReminderStatus: CardView
    private lateinit var tvStatusDetail: TextView
    private lateinit var tvReminderBadge: TextView
    private lateinit var viewPulseRing: View

    private var currentWaterGoal     = 2.5
    private var currentReminderHours = 1
    private var isReminderActive     = false
    private var pulseAnimator: AnimatorSet? = null

    companion object {
        const val WORK_TAG            = "water_reminder_work"
        const val PREFS_NAME          = "water_tracker_prefs"
        const val KEY_REMINDER_HOURS  = "reminder_hours"
        const val KEY_WATER_GOAL      = "water_goal"
        const val KEY_REMINDER_ACTIVE = "reminder_active"

        // WorkManager minimum is 15 minutes — enforce this
        const val MIN_INTERVAL_MINUTES = 15L
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) scheduleReminder()
            else Toast.makeText(
                this,
                "Please enable notifications in Settings to receive water reminders.",
                Toast.LENGTH_LONG
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_tracker)
        createNotificationChannel()
        initViews()
        loadSavedPreferences()
        setupListeners()
        updateReminderStatusUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimator?.cancel()
    }

    private fun initViews() {
        btnBack            = findViewById(R.id.btnBack)
        tvWaterGoal        = findViewById(R.id.tvWaterGoal)
        seekBarWaterGoal   = findViewById(R.id.seekBarWaterGoal)
        tvReminderTime     = findViewById(R.id.tvReminderTime)
        ivClockHand        = findViewById(R.id.ivClockHand)
        btn1Hour           = findViewById(R.id.btn1Hour)
        btn2Hours          = findViewById(R.id.btn2Hours)
        btn3Hours          = findViewById(R.id.btn3Hours)
        btnDrinkWater      = findViewById(R.id.btnDrinkWater)
        btnStopReminder    = findViewById(R.id.btnStopReminder)
        cardReminderStatus = findViewById(R.id.cardReminderStatus)
        tvStatusDetail     = findViewById(R.id.tvStatusDetail)
        tvReminderBadge    = findViewById(R.id.tvReminderBadge)
        viewPulseRing      = findViewById(R.id.viewPulseRing)
    }

    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentWaterGoal     = prefs.getFloat(KEY_WATER_GOAL, 2.5f).toDouble()
        currentReminderHours = prefs.getInt(KEY_REMINDER_HOURS, 1)
        isReminderActive     = prefs.getBoolean(KEY_REMINDER_ACTIVE, false)

        val seekProgress = (currentWaterGoal * 10 - 5).toInt().coerceIn(0, seekBarWaterGoal.max)
        seekBarWaterGoal.progress = seekProgress
        tvWaterGoal.text          = String.format("%.1f", currentWaterGoal)
        updateReminderLabel(currentReminderHours)
        ivClockHand.rotation      = hoursToDegrees(currentReminderHours)
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
                if (isReminderActive) updateStatusCardText()
            }
        })

        btn1Hour.setOnClickListener  { selectPreset(1);  animatePresetButton(btn1Hour)  }
        btn2Hours.setOnClickListener { selectPreset(2);  animatePresetButton(btn2Hours) }
        btn3Hours.setOnClickListener { selectPreset(3);  animatePresetButton(btn3Hours) }

        btnDrinkWater.setOnClickListener {
            bounceView(btnDrinkWater)
            requestBatteryOptimizationExemption()   // ask MIUI to whitelist BEFORE scheduling
            requestNotificationPermissionAndStart()
        }

        btnStopReminder.setOnClickListener {
            bounceView(btnStopReminder)
            stopWaterReminders()
        }
    }

    private fun selectPreset(hours: Int) {
        currentReminderHours = hours
        savePreferences()
        updateReminderLabel(hours)
        ivClockHand.animate()
            .rotation(hoursToDegrees(hours))
            .setDuration(350)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
        if (isReminderActive) {
            // Re-schedule with new interval
            scheduleReminder()
        }
    }

    private fun updateReminderLabel(hours: Int) {
        tvReminderTime.text = formatTime(hours)
    }

    private fun formatTime(hours: Int): String =
        if (hours == 1) "1 hour" else "$hours hours"

    private fun hoursToDegrees(hours: Int): Float = hours * 30f

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

    // ── KEY FIX: Open MIUI battery whitelist screen ───────────────────────
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    // This opens the exact battery exemption dialog
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Please tap 'Allow' to receive notifications when app is closed",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    // Fallback: open general battery settings
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    private fun scheduleReminder() {
        val goalInMl    = (currentWaterGoal * 1000).toInt()

        // ── KEY FIX: WorkManager minimum is 15 min ────────────────────────
        // Convert hours to minutes and enforce the 15-min minimum
        val intervalMinutes = maxOf(
            currentReminderHours * 60L,
            MIN_INTERVAL_MINUTES
        )

        // Constraints — no network needed, run even on low battery
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_HOURS to currentReminderHours.toLong(),
                    ReminderWorker.KEY_GOAL_ML        to goalInMl
                )
            )
            .addTag(WORK_TAG)
            .build()

        // UPDATE replaces the old schedule if interval changed
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        isReminderActive = true
        savePreferences()
        updateReminderStatusUI()

        Toast.makeText(
            this,
            "✅ Reminder set every ${formatTime(currentReminderHours)}",
            Toast.LENGTH_LONG
        ).show()
    }

    fun stopWaterReminders() {
        WorkManager.getInstance(applicationContext).cancelUniqueWork(WORK_TAG)
        isReminderActive = false
        savePreferences()
        updateReminderStatusUI()
        Toast.makeText(this, "🔕 Reminders stopped.", Toast.LENGTH_SHORT).show()
    }

    private fun updateReminderStatusUI() {
        if (isReminderActive) {
            cardReminderStatus.visibility = View.VISIBLE
            cardReminderStatus.alpha      = 0f
            cardReminderStatus.translationY = -20f
            cardReminderStatus.animate()
                .alpha(1f).translationY(0f).setDuration(350)
                .setInterpolator(OvershootInterpolator(1.2f)).start()
            updateStatusCardText()
            startPulseAnimation()
            tvReminderBadge.text = "ACTIVE"
        } else {
            cardReminderStatus.animate()
                .alpha(0f).translationY(-20f).setDuration(250)
                .withEndAction { cardReminderStatus.visibility = View.GONE }.start()
            stopPulseAnimation()
            tvReminderBadge.text = "OFF"
        }
    }

    private fun updateStatusCardText() {
        tvStatusDetail.text = "Every ${formatTime(currentReminderHours)} · ${
            String.format("%.1f", currentWaterGoal)
        } L goal"
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        val ring  = viewPulseRing
        val scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.6f, 1f)
        val scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.6f, 1f)
        val alpha  = ObjectAnimator.ofFloat(ring, "alpha",  0.5f, 0f, 0.5f)
        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatCount = ValueAnimator.INFINITE
        alpha.repeatCount  = ValueAnimator.INFINITE
        scaleX.duration    = 1600
        scaleY.duration    = 1600
        alpha.duration     = 1600
        pulseAnimator = AnimatorSet().apply { playTogether(scaleX, scaleY, alpha); start() }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator        = null
        viewPulseRing.scaleX = 1f
        viewPulseRing.scaleY = 1f
        viewPulseRing.alpha  = 0.4f
    }

    private fun bounceView(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.93f, 1.05f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.93f, 1.05f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration     = 300
            interpolator = OvershootInterpolator(2f)
            start()
        }
    }

    private fun animatePresetButton(button: CardView) {
        button.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100)
            .withEndAction {
                button.animate().scaleX(1f).scaleY(1f)
                    .setDuration(200).setInterpolator(OvershootInterpolator(2f)).start()
            }.start()
    }

    fun savePreferences() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putFloat(KEY_WATER_GOAL, currentWaterGoal.toFloat())
            .putInt(KEY_REMINDER_HOURS, currentReminderHours)
            .putBoolean(KEY_REMINDER_ACTIVE, isReminderActive)
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Water Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description      = "Periodic water intake reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}