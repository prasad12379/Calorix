package com.example.run

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlin.math.abs

class Water_take : AppCompatActivity(), SensorEventListener {

    private lateinit var image: ImageView
    private lateinit var prevImage: ImageView
    private lateinit var nextImage: ImageView
    private lateinit var name: TextView
    private lateinit var volume: TextView
    private lateinit var addButton: Button
    private lateinit var dotsLayout: LinearLayout
    private lateinit var card: CardView
    private lateinit var glowView: View
    private lateinit var waterGlassView: WaterGlassView
    private lateinit var mlLabel: TextView
    private lateinit var goalLabel: TextView

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 800
    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
    private var lastUpdate = 0L

    private var index       = 0
    private var totalMl     = 0
    private var goalMl      = 2000   // ✅ var — will be set from intent or SharedPreferences
    private var isAnimating = false

    private val names    = arrayOf("Glass", "Bottle", "Mug", "Steel Glass", "Jug")
    private val volumes  = arrayOf("250 ml", "500 ml", "350 ml", "300 ml", "1000 ml")
    private val mlValues = intArrayOf(250, 500, 350, 300, 1000)

    private val accentColors = intArrayOf(
        Color.parseColor("#00BCD4"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#607D8B"),
        Color.parseColor("#E91E63")
    )

    private val images = intArrayOf(
        R.drawable.glass,
        R.drawable.bottle,
        R.drawable.mug,
        R.drawable.steel_glass,
        R.drawable.jug
    )

    private var x1 = 0f
    private val MIN_DISTANCE = 80

    // ✅ SharedPreferences keys for Water_take
    companion object {
        private const val PREFS_NAME  = "water_take_prefs"
        private const val KEY_TOTAL_ML = "total_ml"
        private const val KEY_GOAL_ML  = "goal_ml"

        // ✅ Get today's date string to reset progress each day
        private fun todayKey(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            return sdf.format(java.util.Date())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_take)

        // ── findViewById ──────────────────────────────────────────────────────
        image          = findViewById(R.id.containerImage)
        prevImage      = findViewById(R.id.prevImage)
        nextImage      = findViewById(R.id.nextImage)
        name           = findViewById(R.id.containerName)
        volume         = findViewById(R.id.containerVolume)
        addButton      = findViewById(R.id.addButton)
        dotsLayout     = findViewById(R.id.dotsLayout)
        card           = findViewById(R.id.card)
        glowView       = findViewById(R.id.glowView)
        waterGlassView = findViewById(R.id.waterGlassView)
        mlLabel        = findViewById(R.id.mlLabel)
        goalLabel      = findViewById(R.id.goalLabel)

        // ── Sensor ────────────────────────────────────────────────────────────
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // ── Restore saved state ───────────────────────────────────────────────
        loadSavedState()

        // ── Read goal from notification intent (overrides saved if provided) ──
        // Intent sends GOAL_ML only when tapped from notification
        val intentGoalMl = intent.getIntExtra("GOAL_ML", -1)
        if (intentGoalMl != -1) {
            // ✅ Notification was tapped — use goal from water_tracker
            goalMl = intentGoalMl
            saveState()   // persist the new goal immediately
        }

        // ── Update UI with correct goal ───────────────────────────────────────
        refreshGoalLabel()       // ✅ sets "/ X ml" correctly
        mlLabel.text = "$totalMl ml"

        buildDots()
        updateContainer(fromRight = true, animate = false)

        // Restore water fill without animation on open
        waterGlassView.animateFill(
            (totalMl.toFloat() / goalMl).coerceIn(0f, 1f),
            accentColors[index]
        )

        // ── Listeners ─────────────────────────────────────────────────────────
        card.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { x1 = event.x }
                MotionEvent.ACTION_UP -> {
                    val delta = event.x - x1
                    if (abs(delta) > MIN_DISTANCE && !isAnimating) {
                        if (delta > 0) previousItem() else nextItem()
                    }
                }
            }
            true
        }

        prevImage.setOnClickListener { if (!isAnimating) previousItem() }
        nextImage.setOnClickListener { if (!isAnimating) nextItem() }

        addButton.setOnClickListener {
            if (!isAnimating) {
                totalMl += mlValues[index]
                saveState()                      // ✅ save after every drink
                updateWaterGlass(animated = true)
                animateButton()
                waterGlassView.animate()
                    .scaleX(1.08f).scaleY(1.08f).setDuration(120)
                    .withEndAction {
                        waterGlassView.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(200)
                            .setInterpolator(OvershootInterpolator(2f))
                    }
            }
        }
    }

    // ── Load saved totalMl and goalMl from SharedPreferences ──────────────────
    private fun loadSavedState() {
        val prefs   = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedDay = prefs.getString("saved_day", "")

        if (savedDay == todayKey()) {
            // ✅ Same day — restore progress
            totalMl = prefs.getInt(KEY_TOTAL_ML, 0)
            goalMl  = prefs.getInt(KEY_GOAL_ML, 2000)
        } else {
            // ✅ New day — reset progress but keep goal
            totalMl = 0
            goalMl  = prefs.getInt(KEY_GOAL_ML, 2000)
            saveState()   // write new day into prefs
        }
    }

    // ── Save current totalMl, goalMl and today's date ─────────────────────────
    private fun saveState() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putInt(KEY_TOTAL_ML, totalMl)
            .putInt(KEY_GOAL_ML, goalMl)
            .putString("saved_day", todayKey())   // ✅ track day for auto-reset
            .apply()
    }

    // ── Always shows correct goal label ───────────────────────────────────────
    private fun refreshGoalLabel() {
        if (totalMl >= goalMl) {
            goalLabel.text = "✅ Goal reached!"
            goalLabel.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            goalLabel.text = "/ $goalMl ml"        // ✅ uses current goalMl
            goalLabel.setTextColor(Color.parseColor("#1E3550"))
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        saveState()   // ✅ also save when app goes to background
    }

    // ── Shake detection ───────────────────────────────────────────────────────
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val now = System.currentTimeMillis()
        if (now - lastUpdate < 100) return
        val diffTime = now - lastUpdate
        lastUpdate = now

        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
        if (speed > SHAKE_THRESHOLD) {
            if (now - lastShakeTime > 1000) {
                lastShakeTime = now
                onShakeDetected()
            }
        }
        lastX = x; lastY = y; lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onShakeDetected() {
        runOnUiThread {
            waterGlassView.shake()
            card.animate().rotationY(8f).setDuration(80)
                .withEndAction {
                    card.animate().rotationY(-8f).setDuration(80)
                        .withEndAction { card.animate().rotationY(0f).setDuration(80) }
                }
            Toast.makeText(this, "💧 Shake detected! Keep hydrating!", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Water glass update ────────────────────────────────────────────────────
    private fun updateWaterGlass(animated: Boolean) {
        val pct   = (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)
        val color = accentColors[index]

        waterGlassView.animateFill(pct, color)

        val start = (totalMl - mlValues[index]).coerceAtLeast(0)
        ValueAnimator.ofInt(start, totalMl.coerceAtMost(goalMl)).apply {
            duration = 700
            addUpdateListener { mlLabel.text = "${it.animatedValue} ml" }
            start()
        }

        // ✅ Use refreshGoalLabel() — single source of truth for goal display
        refreshGoalLabel()
    }

    // ── Dots ──────────────────────────────────────────────────────────────────
    private fun buildDots() {
        dotsLayout.removeAllViews()
        for (i in images.indices) {
            val dot = View(this)
            val w = if (i == index) dpToPx(22) else dpToPx(7)
            val params = LinearLayout.LayoutParams(w, dpToPx(7))
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0)
            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.dot_bg)
            dot.background.setTint(
                if (i == index) accentColors[index] else 0xFF1E3550.toInt()
            )
            dotsLayout.addView(dot)
        }
    }

    // ── Container update ──────────────────────────────────────────────────────
    private fun updateContainer(fromRight: Boolean = true, animate: Boolean = true) {
        val accent  = accentColors[index]
        val prevIdx = (index - 1 + images.size) % images.size
        val nextIdx = (index + 1) % images.size
        prevImage.setImageResource(images[prevIdx])
        nextImage.setImageResource(images[nextIdx])
        glowView.background.setTint(accent and 0x00FFFFFF or 0x22000000)

        if (!animate) {
            image.setImageResource(images[index])
            name.text   = names[index]
            volume.text = volumes[index]
            applyAccent(accent)
            return
        }

        isAnimating = true
        val slideOut = if (fromRight) -500f else 500f
        val slideIn  = if (fromRight)  500f else -500f

        image.animate()
            .translationX(slideOut).alpha(0f).scaleX(0.85f).scaleY(0.85f)
            .setDuration(200).setInterpolator(DecelerateInterpolator())
            .withEndAction {
                image.setImageResource(images[index])
                image.translationX = slideIn
                image.scaleX = 0.85f; image.scaleY = 0.85f
                image.animate()
                    .translationX(0f).alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(280).setInterpolator(OvershootInterpolator(1.1f))
                    .withEndAction { isAnimating = false }
            }

        prevImage.alpha = 0f; nextImage.alpha = 0f
        prevImage.animate().alpha(0.35f).setDuration(300)
        nextImage.animate().alpha(0.35f).setDuration(300)

        name.animate().alpha(0f).translationY(-20f).setDuration(150)
            .withEndAction {
                name.text = names[index]
                name.translationY = 30f
                name.animate().alpha(1f).translationY(0f).setDuration(250)
                    .setInterpolator(OvershootInterpolator(1.2f)).setStartDelay(80)
            }

        volume.animate().alpha(0f).translationY(-15f).setDuration(150)
            .withEndAction {
                volume.text = volumes[index]
                volume.translationY = 25f
                volume.animate().alpha(1f).translationY(0f).setDuration(250)
                    .setInterpolator(OvershootInterpolator(1.2f)).setStartDelay(130)
            }

        waterGlassView.animateFill(
            (totalMl.toFloat() / goalMl).coerceIn(0f, 1f),
            accent
        )

        applyAccent(accent)
        buildDots()
    }

    private fun applyAccent(accent: Int) {
        volume.setTextColor(accent)
        volume.background.setTint(accent and 0x00FFFFFF or 0x22000000)
        addButton.background.setTint(accent)
        mlLabel.setTextColor(accent)
    }

    private fun animateButton() {
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(addButton, "scaleX", 1f, 0.92f).setDuration(100),
                ObjectAnimator.ofFloat(addButton, "scaleX", 0.92f, 1f).setDuration(150)
            )
            start()
        }
        addButton.animate().scaleY(0.92f).setDuration(100)
            .withEndAction { addButton.animate().scaleY(1f).setDuration(150) }
    }

    private fun nextItem() {
        index = (index + 1) % images.size
        updateContainer(fromRight = true)
    }

    private fun previousItem() {
        index = (index - 1 + images.size) % images.size
        updateContainer(fromRight = false)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}