package com.example.run

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class CountdownActivity : AppCompatActivity() {

    private lateinit var tvCountdown: TextView
    private lateinit var tvCountdownMode: TextView
    private lateinit var tvCountdownMessage: TextView
    private lateinit var btnCancelCountdown: CardView

    private var workoutMode = "RUNNING"
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        // Get workout mode from intent
        workoutMode = intent.getStringExtra("MODE") ?: "RUNNING"

        initViews()
        setupListeners()
        startCountdown()
    }

    private fun initViews() {
        tvCountdown = findViewById(R.id.tvCountdown)
        tvCountdownMode = findViewById(R.id.tvCountdownMode)
        tvCountdownMessage = findViewById(R.id.tvCountdownMessage)
        btnCancelCountdown = findViewById(R.id.btnCancelCountdown)

        tvCountdownMode.text = workoutMode
    }

    private fun setupListeners() {
        btnCancelCountdown.setOnClickListener {
            cancelCountdown()
        }
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                tvCountdown.text = secondsLeft.toString()

                // ✅ ANIMATE COUNTDOWN NUMBER
                animateCountdownNumber()

                // ✅ PLAY BEEP SOUND (optional)
                if (secondsLeft <= 3) {
                    playBeep()
                }

                // ✅ UPDATE MESSAGE
                if (secondsLeft == 1) {
                    tvCountdownMessage.text = "GO!"
                }
            }

            override fun onFinish() {
                // ✅ START WORKOUT ACTIVITY
                val intent = Intent(this@CountdownActivity, WorkoutActivity::class.java)
                intent.putExtra("MODE", workoutMode)
                startActivity(intent)
                finish()

                // ✅ SMOOTH TRANSITION
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }.start()
    }

    // ✅ ANIMATE NUMBER SCALING
    private fun animateCountdownNumber() {
        // Scale animation
        val scaleX = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 1.3f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 1.3f, 1.0f)

        scaleX.duration = 300
        scaleY.duration = 300

        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()

        scaleX.start()
        scaleY.start()

        // Alpha animation
        val alpha = ObjectAnimator.ofFloat(tvCountdown, "alpha", 0.5f, 1.0f)
        alpha.duration = 300
        alpha.start()
    }

    // ✅ PLAY BEEP SOUND (optional)
    private fun playBeep() {
        try {
            val mediaPlayer = MediaPlayer.create(this, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // ✅ PREVENT BACK BUTTON DURING COUNTDOWN
    override fun onBackPressed() {
        cancelCountdown()
    }
}