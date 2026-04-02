package com.example.run

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class Water_take : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 800
    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
    private var lastUpdate = 0L

    // ✅ Shake callback to notify Compose UI
    var onShake: (() -> Unit)? = null

    companion object {
        private const val PREFS_NAME   = "water_take_prefs"
        private const val KEY_TOTAL_ML = "total_ml"
        private const val KEY_GOAL_ML  = "goal_ml"

        private fun todayKey(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun loadState(context: Context): Pair<Int, Int> {
            val prefs    = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedDay = prefs.getString("saved_day", "")
            return if (savedDay == todayKey()) {
                prefs.getInt(KEY_TOTAL_ML, 0) to prefs.getInt(KEY_GOAL_ML, 2000)
            } else {
                0 to prefs.getInt(KEY_GOAL_ML, 2000)
            }
        }

        fun saveState(context: Context, totalMl: Int, goalMl: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(KEY_TOTAL_ML, totalMl)
                .putInt(KEY_GOAL_ML, goalMl)
                .putString("saved_day", todayKey())
                .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val intentGoalMl = intent.getIntExtra("GOAL_ML", -1)

        setContent {
            WaterTakeTheme {
                WaterTakeScreen(
                    intentGoalMl = intentGoalMl,
                    onShakeRegister = { callback -> onShake = callback },
                    onBack = { finish() }
                )
            }
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
        if (speed > SHAKE_THRESHOLD && now - lastShakeTime > 1000) {
            lastShakeTime = now
            runOnUiThread { onShake?.invoke() }
        }
        lastX = x; lastY = y; lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun WaterTakeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0A1628),
            surface    = Color(0xFF111E30),
            primary    = Color(0xFF00BCD4)
        ),
        content = content
    )
}

// ── Container data ────────────────────────────────────────────────────────────
data class ContainerItem(
    val name: String,
    val volume: String,
    val ml: Int,
    val imageRes: Int,
    val color: Color
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@Composable
fun WaterTakeScreen(
    intentGoalMl: Int,
    onShakeRegister: ((() -> Unit)) -> Unit,
    onBack: () -> Unit
) {
    val context  = LocalContext.current
    val haptic   = LocalHapticFeedback.current
    val scope    = rememberCoroutineScope()

    val containers = remember {
        listOf(
            ContainerItem("Glass",       "250 ml",  250,  R.drawable.glass,       Color(0xFF00BCD4)),
            ContainerItem("Bottle",      "500 ml",  500,  R.drawable.bottle,      Color(0xFF4CAF50)),
            ContainerItem("Mug",         "350 ml",  350,  R.drawable.mug,         Color(0xFFFF9800)),
            ContainerItem("Steel Glass", "300 ml",  300,  R.drawable.steel_glass, Color(0xFF607D8B)),
            ContainerItem("Jug",         "1000 ml", 1000, R.drawable.jug,         Color(0xFFE91E63))
        )
    }

    // ── State ─────────────────────────────────────────────────────────────────
    var index      by remember { mutableIntStateOf(0) }
    var totalMl    by remember { mutableIntStateOf(0) }
    var goalMl     by remember { mutableIntStateOf(2000) }
    var shakeAnim  by remember { mutableStateOf(false) }
    var addPulse   by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isSwapping by remember { mutableStateOf(false) }

    // Load saved state
    LaunchedEffect(Unit) {
        val (saved, goal) = Water_take.loadState(context)
        totalMl = saved
        goalMl  = if (intentGoalMl != -1) intentGoalMl else goal
        Water_take.saveState(context, totalMl, goalMl)
    }

    // Register shake callback
    LaunchedEffect(Unit) {
        onShakeRegister {
            shakeAnim = true
            Toast.makeText(context, "💧 Shake detected! Keep hydrating!", Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(600)
                shakeAnim = false
            }
        }
    }

    val current   = containers[index]
    val prevIndex = (index - 1 + containers.size) % containers.size
    val nextIndex = (index + 1) % containers.size

    fun addWater() {
        totalMl += current.ml
        addPulse = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Water_take.saveState(context, totalMl, goalMl)
        scope.launch { delay(300); addPulse = false }
    }

    fun swipeTo(newIndex: Int) {
        if (!isSwapping) {
            isSwapping = true
            index = newIndex
            scope.launch { delay(400); isSwapping = false }
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────
    val fillPercent = (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)

    val animatedFill by animateFloatAsState(
        targetValue   = fillPercent,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "fill"
    )

    val shakeOffset by animateFloatAsState(
        targetValue   = if (shakeAnim) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label         = "shake"
    )

    val addScale by animateFloatAsState(
        targetValue   = if (addPulse) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "addScale"
    )

    // Infinite background wave
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "wavePhase"
    )
    val bgPulse by infiniteTransition.animateFloat(
        initialValue  = 0.97f,
        targetValue   = 1.03f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label         = "bgPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628))
    ) {
        // ── Animated background ───────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle radial glow behind glass
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(
                        current.color.copy(alpha = 0.08f * bgPulse),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height * 0.72f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width / 2, size.height * 0.72f)
            )
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Text(
                "💧 Pick Your Container",
                color      = Color(0xFF7EC8E3),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Select to log water intake",
                color    = Color(0xFF2A4560),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Carousel ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .pointerInput(index) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -80) swipeTo(nextIndex)
                                else if (dragOffset > 80) swipeTo(prevIndex)
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, delta -> dragOffset += delta }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Prev peek
                Image(
                    painter            = painterResource(containers[prevIndex].imageRes),
                    contentDescription = null,
                    modifier           = Modifier
                        .size(90.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-18).dp)
                        .alpha(0.35f)
                        .clickable { swipeTo(prevIndex) }
                )

                // Next peek
                Image(
                    painter            = painterResource(containers[nextIndex].imageRes),
                    contentDescription = null,
                    modifier           = Modifier
                        .size(90.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 18.dp)
                        .alpha(0.35f)
                        .clickable { swipeTo(nextIndex) }
                )

                // Main card
                AnimatedContent(
                    targetState   = index,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * dir } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally { -it * dir } + fadeOut(tween(200)))
                    },
                    label = "cardAnim"
                ) { idx ->
                    val item = containers[idx]
                    Box(
                        modifier         = Modifier
                            .size(width = 210.dp, height = 260.dp)
                            .graphicsLayer {
                                // Shake effect
                                translationX = sin(shakeOffset * PI.toFloat() * 8) * 18f * (1f - shakeOffset)
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        item.color.copy(alpha = 0.12f),
                                        Color(0xFF0D1F35).copy(alpha = 0.9f)
                                    )
                                ),
                                RoundedCornerShape(36.dp)
                            )
                            .border(
                                1.5.dp,
                                item.color.copy(alpha = 0.3f),
                                RoundedCornerShape(36.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow ring behind image
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            item.color.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        Image(
                            painter            = painterResource(item.imageRes),
                            contentDescription = item.name,
                            modifier           = Modifier.size(150.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Container name ────────────────────────────────────────────────
            AnimatedContent(
                targetState   = current.name,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "nameAnim"
            ) { name ->
                Text(
                    name,
                    color      = Color(0xFFE8F4FD),
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Volume badge ──────────────────────────────────────────────────
            AnimatedContent(
                targetState   = current.volume,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label         = "volAnim"
            ) { vol ->
                Box(
                    modifier = Modifier
                        .background(
                            current.color.copy(alpha = 0.15f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, current.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(vol, color = current.color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Dot indicators ────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                containers.forEachIndexed { i, item ->
                    val isSelected = i == index
                    val dotWidth by animateDpAsState(
                        targetValue   = if (isSelected) 22.dp else 7.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label         = "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = dotWidth, height = 7.dp)
                            .background(
                                if (isSelected) item.color else Color(0xFF1E3550),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { swipeTo(i) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Add button ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp)
                    .scale(addScale)
                    .background(
                        Brush.horizontalGradient(
                            listOf(current.color, current.color.copy(alpha = 0.7f))
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable { addWater() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💧", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "+ Add Water",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Circular water glass ──────────────────────────────────────────
            WaterCircleSection(
                fillPercent  = animatedFill,
                totalMl      = totalMl,
                goalMl       = goalMl,
                accentColor  = current.color,
                wavePhase    = wavePhase,
                shakeAnim    = shakeAnim,
                addPulse     = addPulse
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Swipe hint ────────────────────────────────────────────────────
            Text(
                "← SWIPE TO CHANGE →",
                color         = Color(0xFF1E3550),
                fontSize      = 9.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Water Circle Section ──────────────────────────────────────────────────────
@Composable
fun WaterCircleSection(
    fillPercent: Float,
    totalMl: Int,
    goalMl: Int,
    accentColor: Color,
    wavePhase: Float,
    shakeAnim: Boolean,
    addPulse: Boolean
) {
    val goalReached = totalMl >= goalMl

    // Pulse ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label         = "ringAlpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.35f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label         = "ringScale"
    )

    // Bubble positions
    val bubbles = remember {
        List(6) {
            Triple(
                Random().nextFloat() * 0.6f + 0.2f,
                Random().nextFloat() * 0.5f + 0.3f,
                Random().nextFloat() * 3f + 2f
            )
        }
    }
    val bubbleAnim by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label         = "bubbles"
    )

    val shakeX by animateFloatAsState(
        targetValue   = if (shakeAnim) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "shakeX"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Pulsing ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ringScale)
                    .border(2.dp, accentColor.copy(alpha = ringAlpha), CircleShape)
            )

            // Main water circle
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (addPulse) 1.08f else 1f)
                    .graphicsLayer {
                        translationX = sin(shakeX * PI.toFloat() * 6) * 14f * (1f - shakeX)
                    }
            ) {
                val w = size.width
                val h = size.height
                val r = w / 2f

                // Dark background circle
                drawCircle(color = Color(0xFF0D1F35), radius = r)

                // Water fill with wave
                if (fillPercent > 0f) {
                    val waterTop = h - (fillPercent * h)
                    val path     = Path()
                    path.moveTo(-10f, waterTop)

                    val waveAmp = r * 0.06f
                    var x = -10f
                    while (x < w + 10f) {
                        val y = waterTop +
                                waveAmp * sin((x / (w * 0.7f) * 2f * PI + wavePhase).toDouble()).toFloat() +
                                waveAmp * 0.5f * sin((x / (w * 0.7f) * 4f * PI + wavePhase * 1.3f).toDouble()).toFloat()
                        path.lineTo(x, y)
                        x += 3f
                    }
                    path.lineTo(w + 10f, h + 10f)
                    path.lineTo(-10f, h + 10f)
                    path.close()

                    // Clip to circle
                    val clipPath = Path()
                    clipPath.addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
                    clipPath.op(path, Path(), PathOperation.Intersect)

                    drawPath(
                        path  = clipPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(accentColor.red, accentColor.green, accentColor.blue, 0.9f),
                                accentColor,
                                accentColor.copy(alpha = 0.7f)
                            )
                        )
                    )

                    // Bubbles
                    bubbles.forEach { (bx, by, br) ->
                        val bubY = ((by - bubbleAnim * 0.3f) % 1f).let {
                            if (it < 0f) it + 1f else it
                        }
                        val actualY = waterTop + (h - waterTop) * bubY
                        if (actualY > waterTop) {
                            drawCircle(
                                color  = Color.White.copy(alpha = 0.25f),
                                radius = br,
                                center = Offset(w * bx, actualY)
                            )
                        }
                    }

                    // Shine
                    drawOval(
                        color    = Color.White.copy(alpha = 0.15f),
                        topLeft  = Offset(w * 0.25f, waterTop + 3f),
                        size     = Size(w * 0.25f, 5f)
                    )
                }

                // Outer accent ring
                drawCircle(
                    color  = accentColor.copy(alpha = if (fillPercent > 0f) 0.6f else 0.2f),
                    radius = r - 1f,
                    style  = Stroke(width = 2.5f)
                )

                // Percentage text
                val pct = (fillPercent * 100).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    "$pct%",
                    w / 2f,
                    h / 2f + 6f,
                    android.graphics.Paint().apply {
                        color     = android.graphics.Color.WHITE
                        textSize  = r * 0.38f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface  = android.graphics.Typeface.DEFAULT_BOLD
                        alpha     = 200
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── ml counter with animation ─────────────────────────────────────────
        val animMl by animateIntAsState(
            targetValue   = totalMl.coerceAtMost(goalMl),
            animationSpec = tween(700),
            label         = "mlCount"
        )

        Text(
            "$animMl ml",
            color      = accentColor,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        // ── Goal label ────────────────────────────────────────────────────────
        AnimatedContent(
            targetState   = if (totalMl >= goalMl) "✅ Goal reached!" else "/ $goalMl ml",
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label         = "goalAnim"
        ) { label ->
            Text(
                label,
                color    = if (totalMl >= goalMl) Color(0xFF4CAF50) else Color(0xFF1E3550),
                fontSize = 11.sp
            )
        }
    }
}