package com.example.run

// ══════════════════════════════════════════════════
//  TESTING MODE — reminders fire every MINUTE
//  To switch back to production (hours), just change:
//      TimeUnit.MINUTES  →  TimeUnit.HOURS
//  in the two places marked with  ← CHANGE THIS
// ══════════════════════════════════════════════════

import android.Manifest
import android.R.attr.repeatCount
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
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
    private lateinit var btnStopReminder: CardView
    private lateinit var cardReminderStatus: CardView
    private lateinit var tvStatusDetail: TextView
    private lateinit var tvReminderBadge: TextView
    private lateinit var viewPulseRing: View

    private var currentWaterGoal = 2.5
    private var currentReminderMinutes = 1   // ← default 1 for testing
    private var isReminderActive = false
    private var pulseAnimator: AnimatorSet? = null

    companion object {
        const val WORK_TAG             = "water_reminder_work"
        const val PREFS_NAME           = "water_tracker_prefs"
        const val KEY_REMINDER_MINUTES = "reminder_minutes"
        const val KEY_WATER_GOAL       = "water_goal"
        const val KEY_REMINDER_ACTIVE  = "reminder_active"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) scheduleReminder()
            else Toast.makeText(this, "Enable notifications in Settings.", Toast.LENGTH_LONG).show()
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
        currentWaterGoal       = prefs.getFloat(KEY_WATER_GOAL, 2.5f).toDouble()
        currentReminderMinutes = prefs.getInt(KEY_REMINDER_MINUTES, 1)  // ← default 1 min
        isReminderActive       = prefs.getBoolean(KEY_REMINDER_ACTIVE, false)

        val seekProgress = (currentWaterGoal * 10 - 5).toInt().coerceIn(0, seekBarWaterGoal.max)
        seekBarWaterGoal.progress = seekProgress
        tvWaterGoal.text = String.format("%.1f", currentWaterGoal)
        updateReminderLabel(currentReminderMinutes)
        ivClockHand.rotation = minutesToDegrees(currentReminderMinutes)
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

        // ── TEST presets: 1 min / 2 min / 3 min ──
        btn1Hour.setOnClickListener  { selectPreset(1); animatePresetButton(btn1Hour) }
        btn2Hours.setOnClickListener { selectPreset(2); animatePresetButton(btn2Hours) }
        btn3Hours.setOnClickListener { selectPreset(3); animatePresetButton(btn3Hours) }

        btnDrinkWater.setOnClickListener {
            bounceView(btnDrinkWater)
            requestNotificationPermissionAndStart()
        }

        btnStopReminder.setOnClickListener {
            bounceView(btnStopReminder)
            stopWaterReminders()
        }
    }

    private fun selectPreset(minutes: Int) {
        currentReminderMinutes = minutes
        savePreferences()
        updateReminderLabel(minutes)
        ivClockHand.animate()
            .rotation(minutesToDegrees(minutes))
            .setDuration(350)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
        if (isReminderActive) updateStatusCardText()
    }

    private fun updateReminderLabel(minutes: Int) {
        tvReminderTime.text = formatTime(minutes)
    }

    // ── Shows "1 min", "2 mins", "3 mins" in test mode ──
    private fun formatTime(minutes: Int): String =
        if (minutes == 1) "1 min" else "$minutes mins"

    private fun minutesToDegrees(minutes: Int): Float = minutes * 30f

    private fun requestNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                scheduleReminder()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleReminder()
        }
    }

    private fun scheduleReminder() {
        val goalInMl = (currentWaterGoal * 1000).toInt()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_MINUTES to currentReminderMinutes.toLong(),
                    ReminderWorker.KEY_GOAL_ML          to goalInMl
                )
            )
            .setInitialDelay(currentReminderMinutes.toLong(), TimeUnit.MINUTES)  // ← CHANGE THIS to TimeUnit.HOURS for production
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, request)

        isReminderActive = true
        savePreferences()
        updateReminderStatusUI()

        Toast.makeText(this, "✅ Reminder every ${formatTime(currentReminderMinutes)} (test mode)", Toast.LENGTH_LONG).show()
    }

    fun stopWaterReminders() {
        val wm = WorkManager.getInstance(applicationContext)
        wm.cancelUniqueWork(water_tracker.WORK_TAG)               // initial job
        wm.cancelUniqueWork(water_tracker.WORK_TAG + "_next")     // rescheduled jobs
        wm.cancelAllWorkByTag(water_tracker.WORK_TAG)             // catch any tagged stragglers
        isReminderActive = false
        savePreferences()
        updateReminderStatusUI()
        Toast.makeText(this, "🔕 Reminders stopped.", Toast.LENGTH_SHORT).show()
    }

    private fun updateReminderStatusUI() {
        if (isReminderActive) {
            cardReminderStatus.visibility = View.VISIBLE
            cardReminderStatus.alpha = 0f
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
        tvStatusDetail.text = "Every ${formatTime(currentReminderMinutes)} · ${
            String.format("%.1f", currentWaterGoal)
        } L goal"
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()

        val ring = viewPulseRing  // local reference avoids compiler confusion

        val scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.6f, 1f)
        val scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.6f, 1f)
        val alpha  = ObjectAnimator.ofFloat(ring, "alpha",  0.5f, 0f, 0.5f)

        // ✅ DO NOT use apply{} block — set properties directly on the instance
        //    to avoid Kotlin compiler confusing .repeatCount with android.R.attr.repeatCount
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY, alpha)
        set.duration = 1600
        set.start()

        // Repeat each individual animator instead of the set (AnimatorSet has no repeatCount)
        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatCount = ValueAnimator.INFINITE
        alpha.repeatCount  = ValueAnimator.INFINITE

        pulseAnimator = set
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        viewPulseRing.scaleX = 1f
        viewPulseRing.scaleY = 1f
        viewPulseRing.alpha  = 0.4f
    }

    private fun bounceView(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.93f, 1.05f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.93f, 1.05f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy); duration = 300
            interpolator = OvershootInterpolator(2f); start()
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
            .putInt(KEY_REMINDER_MINUTES, currentReminderMinutes)
            .putBoolean(KEY_REMINDER_ACTIVE, isReminderActive)
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel", "Water Reminder", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Periodic water intake reminders"; enableVibration(true) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}