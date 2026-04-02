package com.example.run

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

class Stress_Game : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StressGameTheme {
                StressGameScreen(onBack = { finish() })
            }
        }
    }
}

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun StressGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0A0E1A),
            surface    = Color(0xFF111827),
            primary    = Color(0xFF6C63FF)
        ),
        content = content
    )
}

// ── Game States ───────────────────────────────────────────────────────────────
enum class GameState { INTRO, COUNTDOWN, PLAYING, RESULT }
enum class StressLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float
)

data class RoundResult(
    val reactionMs: Long,
    val correct: Boolean
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@Composable
fun StressGameScreen(onBack: () -> Unit) {
    var gameState    by remember { mutableStateOf(GameState.INTRO) }
    var countdown    by remember { mutableIntStateOf(3) }
    var roundResults by remember { mutableStateOf(listOf<RoundResult>()) }
    var stressLevel  by remember { mutableStateOf(StressLevel.LOW) }
    var stressScore  by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    fun startGame() {
        roundResults = emptyList()
        scope.launch {
            gameState = GameState.COUNTDOWN
            for (i in 3 downTo 1) {
                countdown = i
                delay(1000)
            }
            gameState = GameState.PLAYING
        }
    }

    fun onGameComplete(results: List<RoundResult>) {
        roundResults = results
        val avgReaction = results.map { it.reactionMs }.average()
        val accuracy    = results.count { it.correct }.toFloat() / results.size
        val variance    = results.map { it.reactionMs }
            .let { times ->
                val mean = times.average()
                sqrt(times.map { t -> (t - mean).pow(2.0) }.average())
            }

        stressScore = ((avgReaction / 10.0) * (1.0 - accuracy) + variance / 50.0)
            .toInt().coerceIn(0, 100)

        stressLevel = when {
            stressScore < 25 -> StressLevel.LOW
            stressScore < 50 -> StressLevel.MEDIUM
            stressScore < 75 -> StressLevel.HIGH
            else             -> StressLevel.VERY_HIGH
        }

        gameState = GameState.RESULT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        BackgroundParticles()

        when (gameState) {
            GameState.INTRO     -> IntroScreen(onStart = { startGame() }, onBack = onBack)
            GameState.COUNTDOWN -> CountdownScreen(countdown = countdown)
            GameState.PLAYING   -> GamePlayScreen(onComplete = { onGameComplete(it) })
            GameState.RESULT    -> ResultScreen(
                stressLevel = stressLevel,
                stressScore = stressScore,
                results     = roundResults,
                onPlayAgain = { startGame() },
                onBack      = onBack
            )
        }
    }
}

// ── Background Particles ──────────────────────────────────────────────────────
@Composable
fun BackgroundParticles() {
    val particles = remember {
        List(20) {
            Particle(
                id     = it,
                x      = Random.nextFloat(),
                y      = Random.nextFloat(),
                vx     = (Random.nextFloat() - 0.5f) * 0.0002f,
                vy     = (Random.nextFloat() - 0.5f) * 0.0002f,
                color  = listOf(
                    Color(0xFF6C63FF),
                    Color(0xFF00BCD4),
                    Color(0xFFE91E63),
                    Color(0xFF4CAF50)
                ).random(),
                radius = Random.nextFloat() * 3f + 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val time by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label         = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val px = ((p.x + p.vx * time * 50000f) % 1f).let {
                if (it < 0f) it + 1f else it
            } * size.width
            val py = ((p.y + p.vy * time * 50000f) % 1f).let {
                if (it < 0f) it + 1f else it
            } * size.height
            drawCircle(
                color  = p.color.copy(alpha = 0.3f),
                radius = p.radius * density,
                center = Offset(px, py)
            )
        }
    }
}

// ── Intro Screen ──────────────────────────────────────────────────────────────
@Composable
fun IntroScreen(onStart: () -> Unit, onBack: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseAnim.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAnim by pulseAnim.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size((120 + i * 30).dp)
                        .scale(pulse)
                        .background(
                            Color(0xFF6C63FF).copy(alpha = glowAnim * (0.15f - i * 0.04f)),
                            CircleShape
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulse)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F3A8A))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 48.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text       = "Stress Detection",
            fontSize   = 32.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text       = "A quick cognitive game to\nmeasure your stress level",
            fontSize   = 16.sp,
            color      = Color.White.copy(alpha = 0.6f),
            textAlign  = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            "⚡" to "10 quick rounds",
            "🎯" to "Tap the matching color",
            "📊" to "Get your stress score"
        ).forEach { (emoji, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .scale(pulse)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF00BCD4))),
                    RoundedCornerShape(30.dp)
                )
                .clickable { onStart() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Start Game",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("← Back", color = Color.White.copy(alpha = 0.5f))
        }
    }
}

// ── Countdown Screen ──────────────────────────────────────────────────────────
@Composable
fun CountdownScreen(countdown: Int) {
    val scale by animateFloatAsState(
        targetValue   = if (countdown > 0) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "countScale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val pulse = rememberInfiniteTransition(label = "cdPulse")
        val ringScale by pulse.animateFloat(
            initialValue  = 0.8f,
            targetValue   = 1.3f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Restart),
            label         = "ring"
        )
        val ringAlpha by pulse.animateFloat(
            initialValue  = 0.6f,
            targetValue   = 0f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Restart),
            label         = "ringAlpha"
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(ringScale)
                .border(3.dp, Color(0xFF6C63FF).copy(alpha = ringAlpha), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F3A8A))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = if (countdown > 0) "$countdown" else "GO!",
                fontSize   = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
        }

        Text(
            text     = "Get Ready...",
            fontSize = 18.sp,
            color    = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.offset(y = 120.dp)
        )
    }
}

// ── Gameplay Screen ───────────────────────────────────────────────────────────
@Composable
fun GamePlayScreen(onComplete: (List<RoundResult>) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope  = rememberCoroutineScope()

    var results by remember { mutableStateOf(listOf<RoundResult>()) }

    val totalRounds    = 10
    var currentRound   by remember { mutableIntStateOf(0) }
    var roundStartTime by remember { mutableLongStateOf(0L) }
    var showTarget     by remember { mutableStateOf(false) }
    var isWaiting      by remember { mutableStateOf(false) }

    val colorOptions = remember {
        listOf(
            "RED"    to Color(0xFFE53935),
            "BLUE"   to Color(0xFF1E88E5),
            "GREEN"  to Color(0xFF43A047),
            "YELLOW" to Color(0xFFFDD835),
            "PURPLE" to Color(0xFF8E24AA)
        )
    }

    var targetWord   by remember { mutableStateOf("") }
    var targetColor  by remember { mutableStateOf(Color.White) }
    var options      by remember { mutableStateOf(listOf<Pair<String, Color>>()) }
    var correctIndex by remember { mutableIntStateOf(0) }
    var showBurst    by remember { mutableStateOf(false) }
    var burstColor   by remember { mutableStateOf(Color.Green) }
    var roundJob     by remember { mutableStateOf<Job?>(null) }

    val progressAnim by animateFloatAsState(
        targetValue   = currentRound.toFloat() / totalRounds,
        animationSpec = tween(300),
        label         = "progress"
    )

    fun setupRound() {
        val correct   = colorOptions.random()
        val word      = colorOptions.random().first
        val wrongOpts = colorOptions.filter { it.first != correct.first }.shuffled().take(3)
        val allOpts   = (wrongOpts + correct).shuffled()

        targetWord   = word
        targetColor  = correct.second
        correctIndex = allOpts.indexOf(correct)
        options      = allOpts

        isWaiting      = false
        showTarget     = true
        roundStartTime = System.currentTimeMillis()
    }

    fun onOptionTapped(index: Int) {
        if (isWaiting || !showTarget) return
        isWaiting  = true
        showTarget = false

        val reactionMs = System.currentTimeMillis() - roundStartTime
        val correct    = index == correctIndex
        val newResults = results + RoundResult(reactionMs, correct)
        results        = newResults

        burstColor = if (correct) Color(0xFF4CAF50) else Color(0xFFE53935)
        showBurst  = true

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        roundJob?.cancel()
        roundJob = scope.launch {
            delay(800)
            showBurst = false
            if (newResults.size >= totalRounds) {
                onComplete(newResults)
            } else {
                currentRound++
                delay(400L + Random.nextLong(600L))
                setupRound()
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        setupRound()
        currentRound = 1
    }

    DisposableEffect(Unit) {
        onDispose { roundJob?.cancel() }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showBurst) BurstEffect(color = burstColor)

        Column(
            modifier            = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text     = "Round $currentRound / $totalRounds",
                color    = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF6C63FF), Color(0xFF00BCD4))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text       = "Tap the color that matches\nthe TEXT COLOR (not the word)",
                color      = Color.White.copy(alpha = 0.5f),
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedVisibility(
                visible = showTarget,
                enter   = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .border(2.dp, targetColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = targetWord,
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = targetColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                options.chunked(2).forEachIndexed { rowIdx, rowOptions ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        rowOptions.forEachIndexed { colIdx, option ->
                            val globalIdx = rowIdx * 2 + colIdx
                            ColorOptionButton(
                                color    = option.second,
                                label    = option.first,
                                enabled  = showTarget && !isWaiting,
                                modifier = Modifier.weight(1f),
                                onClick  = { onOptionTapped(globalIdx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOptionButton(
    color: Color,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (enabled) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "btnScale"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (enabled) 1f else 0.4f,
        animationSpec = tween(200),
        label         = "btnAlpha"
    )

    Box(
        modifier = modifier
            .height(70.dp)
            .scale(scale)
            .alpha(alpha)
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(14.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Particle Burst Effect ─────────────────────────────────────────────────────
@Composable
fun BurstEffect(color: Color) {
    val particles = remember(color) {
        List(20) {
            val angle = (it / 20f) * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 8f + 4f
            Triple(cos(angle) * speed, sin(angle) * speed, Random.nextFloat() * 6f + 3f)
        }
    }

    var progress by remember(color) { mutableFloatStateOf(0f) }

    LaunchedEffect(color) {
        val startTime = System.currentTimeMillis()
        val duration  = 800L
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            if (progress >= 1f) break
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        particles.forEach { (vx, vy, r) ->
            val px    = cx + vx * progress * 40f
            val py    = cy + vy * progress * 40f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            drawCircle(
                color  = color.copy(alpha = alpha),
                radius = r * (1f - progress * 0.5f),
                center = Offset(px, py)
            )
        }
    }
}

// ── Result Screen ─────────────────────────────────────────────────────────────
@Composable
fun ResultScreen(
    stressLevel: StressLevel,
    stressScore: Int,
    results: List<RoundResult>,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    data class LevelInfo(val color: Color, val emoji: String, val title: String, val message: String)

    val info = when (stressLevel) {
        StressLevel.LOW       -> LevelInfo(Color(0xFF4CAF50), "😌", "Low Stress",       "You're calm and focused. Keep it up!")
        StressLevel.MEDIUM    -> LevelInfo(Color(0xFFFFC107), "😐", "Moderate Stress",  "Mild stress detected. Take a short break.")
        StressLevel.HIGH      -> LevelInfo(Color(0xFFFF9800), "😰", "High Stress",      "You seem stressed. Try deep breathing.")
        StressLevel.VERY_HIGH -> LevelInfo(Color(0xFFE53935), "😫", "Very High Stress", "High stress! Rest and hydrate now.")
    }

    val animScore by animateIntAsState(
        targetValue   = stressScore,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label         = "scoreAnim"
    )
    val sweep by animateFloatAsState(
        targetValue   = stressScore / 100f * 360f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label         = "sweep"
    )

    val accuracy = if (results.isNotEmpty()) results.count { it.correct }.toFloat() / results.size * 100f else 0f
    val avgMs    = if (results.isNotEmpty()) results.map { it.reactionMs }.average().toInt() else 0

    // ✅ For launching Bhagwat_Gita activity
    val context = LocalContext.current

    // ✅ Gita card glow animation
    val gitaPulse = rememberInfiniteTransition(label = "gitaPulse")
    val gitaGlow by gitaPulse.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "gitaGlow"
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Results",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Animated stress ring ──────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawArc(
                    color      = Color.White.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    style      = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush      = Brush.sweepGradient(
                        listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFE53935))
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter  = false,
                    style      = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(info.emoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$animScore", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = info.color)
                Text("/ 100", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Stress level badge ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .background(info.color.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                .border(1.dp, info.color.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(info.title, color = info.color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(info.message, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        // ── Stats ─────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(modifier = Modifier.weight(1f), label = "Accuracy",  value = "${accuracy.toInt()}%", color = Color(0xFF00BCD4))
            StatCard(modifier = Modifier.weight(1f), label = "Avg Speed", value = "${avgMs}ms",           color = Color(0xFF6C63FF))
            StatCard(modifier = Modifier.weight(1f), label = "Rounds",    value = "${results.size}",      color = Color(0xFF4CAF50))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── ✅ Bhagavad Gita Recommendation Card (shown for all stress levels) ─
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFF9800).copy(alpha = 0.12f),
                            Color(0xFFE91E63).copy(alpha = 0.12f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFF9800).copy(alpha = gitaGlow),
                            Color(0xFFE91E63).copy(alpha = gitaGlow)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Header row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    // Om icon with glow
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFF9800).copy(alpha = gitaGlow * 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕉️", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        // ✅ Show different message based on stress level
                        Text(
                            text = when (stressLevel) {
                                StressLevel.LOW       -> "Stay Peaceful 🌿"
                                StressLevel.MEDIUM    -> "Find Your Calm 🌊"
                                StressLevel.HIGH      -> "Seek Inner Peace 🙏"
                                StressLevel.VERY_HIGH -> "You Need Peace Now 💫"
                            },
                            color      = Color(0xFFFF9800),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (stressLevel) {
                                StressLevel.LOW       -> "Read Bhagavad Gita shlokas\nto maintain your inner peace"
                                StressLevel.MEDIUM    -> "Bhagavad Gita shlokas can\nhelp restore your calm"
                                StressLevel.HIGH      -> "Read sacred shlokas for\nstress relief and clarity"
                                StressLevel.VERY_HIGH -> "Urgent! Read Gita shlokas\nfor immediate relief"
                            },
                            color      = Color.White.copy(alpha = 0.65f),
                            fontSize   = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFFF9800).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Random shloka preview teaser
                Text(
                    text      = "\" कर्मण्येवाधिकारस्ते मा फलेषु कदाचन \"",
                    color     = Color(0xFFFF9800).copy(alpha = 0.7f),
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Text(
                    text      = "Chapter 2, Verse 47",
                    color     = Color.White.copy(alpha = 0.3f),
                    fontSize  = 10.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Open Bhagwat_Gita button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF9800), Color(0xFFE91E63))
                            ),
                            RoundedCornerShape(25.dp)
                        )
                        .clickable {
                            context.startActivity(
                                Intent(context, bhagwat_gita::class.java)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.Center
                    ) {
                        Text("🕉️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Open Bhagavad Gita",
                            color      = Color.White,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Round breakdown ───────────────────────────────────────────────────
        Text(
            "Round Breakdown",
            color      = Color.White.copy(alpha = 0.5f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        results.forEachIndexed { i, result ->
            RoundRow(round = i + 1, result = result)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Play again button ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF00BCD4))),
                    RoundedCornerShape(28.dp)
                )
                .clickable { onPlayAgain() },
            contentAlignment = Alignment.Center
        ) {
            Text("Play Again", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("← Back to Home", color = Color.White.copy(alpha = 0.5f))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Column(
        modifier            = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

// ── Round Row ─────────────────────────────────────────────────────────────────
@Composable
fun RoundRow(round: Int, result: RoundResult) {
    val color = if (result.correct) Color(0xFF4CAF50) else Color(0xFFE53935)
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Round $round",                                  color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        Text(if (result.correct) "✓ Correct" else "✗ Wrong", color = color,                          fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("${result.reactionMs}ms",                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}