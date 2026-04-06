package com.example.run

import android.content.Intent
import android.media.RingtoneManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
//  THEME
// ─────────────────────────────────────────────
private val NeonGreen   = Color(0xFF00FF87)
private val NeonAmber   = Color(0xFFFFAA00)
private val DeepBlack   = Color(0xFF050A0E)
private val GlassWhite  = Color(0x22FFFFFF)
private val GlassBorder = Color(0x44FFFFFF)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextMuted   = Color(0xFF8899AA)
private val ErrorRed    = Color(0xFFFF4757)
private val CardBg      = Color(0xCC0D1117)

private val modeEmoji = mapOf("RUNNING" to "🏃", "WALKING" to "🚶", "CYCLING" to "🚴")

// ─────────────────────────────────────────────
//  ACTIVITY
// ─────────────────────────────────────────────
class CountActivity : ComponentActivity() {

    private var workoutMode = "RUNNING"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workoutMode = intent.getStringExtra("MODE") ?: "RUNNING"

        setContent {
            CountdownScreen(
                workoutMode = workoutMode,
                onFinish    = { launchWorkout() },
                onCancel    = { finish() },
                onBeep      = { playBeep() }
            )
        }
    }

    private fun launchWorkout() {
        val intent = Intent(this, WorkoutActivity::class.java).apply {
            putExtra("MODE", workoutMode)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun playBeep() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp  = MediaPlayer.create(this, uri)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        } catch (e: Exception) { e.printStackTrace() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { finish() }
}

// ─────────────────────────────────────────────
//  COUNTDOWN SCREEN
// ─────────────────────────────────────────────
@Composable
private fun CountdownScreen(
    workoutMode: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onBeep: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(10) }
    var isRunning   by remember { mutableStateOf(true) }
    var launched    by remember { mutableStateOf(false) }
    var visible     by remember { mutableStateOf(false) }

    // Entrance animation
    LaunchedEffect(Unit) { delay(80); visible = true }

    // ── FIXED countdown engine ──────────────────────────────────────────
    // Uses a single infinite loop that checks isRunning each tick.
    // +10s works because secondsLeft is just a state var — the loop reads
    // the latest value on every iteration automatically.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (!isRunning) continue          // paused — keep ticking time but don't decrement
            if (secondsLeft > 0) {
                secondsLeft--
                if (secondsLeft in 1..3) onBeep()
            }
            if (secondsLeft == 0 && !launched) {
                launched = true
                delay(700)                    // show "GO!" briefly
                onFinish()
                break
            }
        }
    }

    // ── Colour: green → amber → red ──────────────────────────────────────
    val ringColor by animateColorAsState(
        when {
            secondsLeft > 6 -> NeonGreen
            secondsLeft > 3 -> NeonAmber
            else            -> ErrorRed
        },
        tween(500), label = "rc"
    )

    // Ring sweep — clamp to base of 10 or current max if +10s was pressed
    val maxSeconds  = remember { mutableStateOf(10) }
    val sweepTarget = if (secondsLeft == 0) 0f
    else (secondsLeft.toFloat() / maxSeconds.value.toFloat()).coerceIn(0f, 1f)
    val sweepAnim   by animateFloatAsState(sweepTarget, tween(900, easing = LinearEasing), label = "sw")

    // Number scale pop on each tick
    val numScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = keyframes {
            durationMillis = 400
            1.4f at 0   with FastOutSlowInEasing
            1.0f at 400 with LinearEasing
        },
        label = "ns"
    )

    // Pulsing ambient glow
    val inf       = rememberInfiniteTransition(label = "glow")
    val glowAlpha by inf.animateFloat(
        0.12f, 0.42f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ga"
    )

    // ── Layout ───────────────────────────────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(DeepBlack),
        contentAlignment = Alignment.Center
    ) {

        // Ambient glow blob
        Box(
            modifier = Modifier
                .size(440.dp)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(
                                ringColor.copy(alpha = if (isRunning) glowAlpha else 0.06f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.85f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier            = Modifier.padding(horizontal = 24.dp)
            ) {

                // ── Mode chip ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(modeEmoji[workoutMode] ?: "🏃", fontSize = 18.sp)
                    Text(
                        workoutMode,
                        color         = TextPrimary,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 14.sp,
                        letterSpacing = 2.sp
                    )
                }

                // ── Animated ring + number ────────────────────────────
                Box(contentAlignment = Alignment.Center) {

                    // Track ring (dim)
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .drawBehind {
                                drawArc(
                                    color      = ringColor.copy(alpha = 0.12f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter  = false,
                                    style      = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                    )

                    // Progress ring
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .drawBehind {
                                drawArc(
                                    brush      = Brush.sweepGradient(
                                        listOf(ringColor.copy(0.35f), ringColor)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweepAnim * 360f,
                                    useCenter  = false,
                                    style      = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                    )

                    // Inner glass circle
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                            .border(1.dp, GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = secondsLeft,
                            transitionSpec = {
                                (scaleIn(tween(180)) + fadeIn(tween(180))) togetherWith
                                        (scaleOut(tween(120)) + fadeOut(tween(120)))
                            },
                            label = "num"
                        ) { s ->
                            if (s == 0) {
                                Text(
                                    "GO!",
                                    color         = NeonGreen,
                                    fontSize      = 50.sp,
                                    fontWeight    = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            } else {
                                Text(
                                    s.toString(),
                                    color      = ringColor,
                                    fontSize   = 80.sp,
                                    fontWeight = FontWeight.W200,
                                    modifier   = Modifier.scale(numScale)
                                )
                            }
                        }
                    }
                }

                // ── Status label ──────────────────────────────────────
                AnimatedContent(
                    targetState  = secondsLeft,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "lbl"
                ) { s ->
                    Text(
                        when {
                            s == 0  -> "Starting workout…"
                            s <= 3  -> "Get ready! 🔥"
                            else    -> "Get in position"
                        },
                        color         = TextMuted,
                        fontSize      = 14.sp,
                        letterSpacing = 0.5.sp,
                        textAlign     = TextAlign.Center
                    )
                }

                // ── +10s and STOP buttons ─────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ActionButton(
                        label    = "+10s",
                        sublabel = "MORE TIME",
                        color    = NeonGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        secondsLeft += 10
                        // Update max so ring doesn't go > 100%
                        if (secondsLeft > maxSeconds.value) {
                            maxSeconds.value = secondsLeft
                        }
                    }

                    ActionButton(
                        label    = "✕",
                        sublabel = "STOP",
                        color    = ErrorRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        isRunning = false
                        onFinish()
                    }
                }

                Text(
                    "Tap +10s to add more time",
                    color     = TextMuted.copy(alpha = 0.45f),
                    fontSize  = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  ACTION BUTTON
// ─────────────────────────────────────────────
@Composable
private fun ActionButton(
    label: String,
    sublabel: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "abs"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = ripple(color = color)
            ) {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label,    color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(sublabel, color = color, fontSize = 9.sp,  fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}