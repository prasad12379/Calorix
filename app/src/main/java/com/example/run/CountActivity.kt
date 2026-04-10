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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX PALETTE
// ═══════════════════════════════════════════════════════════════════════════════
private val BgWhite      = Color(0xFFFAF9FF)
private val DeepBlack    = Color(0xFF0A0A0A)
private val HeaderDark   = Color(0xFF1C1826)
private val PureWhite    = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF9B8FD4)
private val HoloPink     = Color(0xFFE8B4D8)
private val HoloMint     = Color(0xFFAEE8D8)
private val SubtleGrey   = Color(0xFFDDD8EE)
private val TextPrimary  = Color(0xFF0A0A0A)
private val TextSecondary= Color(0xFF7A7490)
private val WarningAmber = Color(0xFFF5C97A)
private val ErrorRed     = Color(0xFFE8574A)

private val modeEmoji = mapOf("RUNNING" to "🏃", "WALKING" to "🚶", "CYCLING" to "🚴")

// ═══════════════════════════════════════════════════════════════════════════════
//  ACTIVITY  — logic 100% unchanged
// ═══════════════════════════════════════════════════════════════════════════════
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

// ═══════════════════════════════════════════════════════════════════════════════
//  COUNTDOWN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun CountdownScreen(
    workoutMode: String,
    onFinish:    () -> Unit,
    onCancel:    () -> Unit,
    onBeep:      () -> Unit
) {
    // ── STATE — all original logic untouched ──────────────────────────────
    var secondsLeft by remember { mutableStateOf(10) }
    var isRunning   by remember { mutableStateOf(true) }
    var launched    by remember { mutableStateOf(false) }
    var visible     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(80); visible = true }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (!isRunning) continue
            if (secondsLeft > 0) {
                secondsLeft--
                if (secondsLeft in 1..3) onBeep()
            }
            if (secondsLeft == 0 && !launched) {
                launched = true
                delay(700)
                onFinish()
                break
            }
        }
    }

    // ── Ring color: violet → amber → red  (matches CaloriX accent family)
    val ringColor by animateColorAsState(
        when {
            secondsLeft > 6 -> AccentViolet
            secondsLeft > 3 -> WarningAmber
            else            -> ErrorRed
        },
        tween(500), label = "rc"
    )

    val maxSeconds  = remember { mutableStateOf(10) }
    val sweepTarget = if (secondsLeft == 0) 0f
    else (secondsLeft.toFloat() / maxSeconds.value.toFloat()).coerceIn(0f, 1f)
    val sweepAnim   by animateFloatAsState(sweepTarget, tween(900, easing = LinearEasing), label = "sw")

    val numScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = keyframes {
            durationMillis = 400
            1.35f at 0   with FastOutSlowInEasing
            1.0f  at 400 with LinearEasing
        },
        label = "ns"
    )

    val inf       = rememberInfiniteTransition(label = "glow")
    val glowAlpha by inf.animateFloat(
        0.25f, 0.55f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ga"
    )
    // Blob drift
    val drift by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    // ── LAYOUT ────────────────────────────────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepBlack, HeaderDark))),
        contentAlignment = Alignment.Center
    ) {

        // ── Holographic blobs (HomeFragment style) ────────────────────────
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = (60 + drift * 8).dp, y = (-60 + drift * 10).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(listOf(HoloPink.copy(0.45f), HoloMint.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = (50 - drift * 12).dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(listOf(AccentViolet.copy(0.35f), Color.Transparent)),
                    CircleShape
                )
        )

        // ── Ambient ring glow ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(420.dp)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(
                                ringColor.copy(alpha = if (isRunning) glowAlpha * 0.4f else 0.06f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.88f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier            = Modifier.padding(horizontal = 28.dp)
            ) {

                // ── App name + mode chip row ──────────────────────────────
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "CaloriX",
                        color         = PureWhite,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(listOf(AccentViolet, HoloPink)),
                                RoundedCornerShape(1.dp)
                            )
                    )
                    Spacer(Modifier.height(12.dp))
                    // Mode chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PureWhite.copy(0.08f))
                            .border(1.dp, PureWhite.copy(0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(modeEmoji[workoutMode] ?: "🏃", fontSize = 16.sp)
                        Text(
                            workoutMode,
                            color         = PureWhite,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 13.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // ── Countdown ring ────────────────────────────────────────
                Box(contentAlignment = Alignment.Center) {

                    // Track ring
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .drawBehind {
                                drawArc(
                                    color      = ringColor.copy(alpha = 0.10f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter  = false,
                                    style      = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                    )

                    // Progress ring — gradient sweep
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .drawBehind {
                                drawArc(
                                    brush      = Brush.sweepGradient(
                                        listOf(ringColor.copy(0.3f), ringColor, HoloPink.copy(0.6f))
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweepAnim * 360f,
                                    useCenter  = false,
                                    style      = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                    )

                    // Inner glass circle — dark surface matching header
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(HeaderDark, DeepBlack),
                                    radius = 300f
                                )
                            )
                            .border(1.dp, PureWhite.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Shimmer line inside ring
                        Box(
                            Modifier
                                .size(200.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(PureWhite.copy(0.06f), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )
                        AnimatedContent(
                            targetState = secondsLeft,
                            transitionSpec = {
                                (scaleIn(tween(180)) + fadeIn(tween(180))) togetherWith
                                        (scaleOut(tween(120)) + fadeOut(tween(120)))
                            },
                            label = "num"
                        ) { s ->
                            if (s == 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "GO!",
                                        color      = AccentViolet,
                                        fontSize   = 46.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 3.sp
                                    )
                                    Text(
                                        "🔥",
                                        fontSize = 20.sp
                                    )
                                }
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

                // ── Status label ──────────────────────────────────────────
                AnimatedContent(
                    targetState  = secondsLeft,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "lbl"
                ) { s ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PureWhite.copy(0.06f)
                    ) {
                        Text(
                            when {
                                s == 0  -> "Starting your workout…"
                                s <= 3  -> "Get ready!"
                                else    -> "Get in position"
                            },
                            modifier      = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color         = PureWhite.copy(0.7f),
                            fontSize      = 13.sp,
                            letterSpacing = 0.5.sp,
                            textAlign     = TextAlign.Center
                        )
                    }
                }

                // ── +10s and CANCEL buttons ───────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // +10s — violet gradient
                    CountActionButton(
                        label    = "+10s",
                        sublabel = "MORE TIME",
                        gradient = listOf(AccentViolet, HoloPink),
                        modifier = Modifier.weight(1f)
                    ) {
                        secondsLeft += 10
                        if (secondsLeft > maxSeconds.value) {
                            maxSeconds.value = secondsLeft
                        }
                    }

                    // Cancel — subtle dark
                    CountActionButton(
                        label    = "✕",
                        sublabel = "CANCEL",
                        gradient = listOf(ErrorRed.copy(0.7f), ErrorRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        isRunning = false
                        onFinish()
                    }
                }

                Text(
                    "Tap +10s to add more time",
                    color     = PureWhite.copy(0.25f),
                    fontSize  = 10.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  COUNT ACTION BUTTON
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun CountActionButton(
    label:    String,
    sublabel: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "cabs"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient.map { it.copy(0.15f) }))
            .border(
                1.dp,
                Brush.linearGradient(listOf(gradient.first().copy(0.6f), gradient.last().copy(0.3f))),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = ripple(color = gradient.first())
            ) { pressed = true; onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label,    color = gradient.first(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(sublabel, color = gradient.first().copy(0.8f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}