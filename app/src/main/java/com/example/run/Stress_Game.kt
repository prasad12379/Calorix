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

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX PALETTE
// ═══════════════════════════════════════════════════════════════════════════════
private val BgWhite      = Color(0xFFFAF9FF)
private val BgLavender   = Color(0xFFECE8F5)
private val DeepBlack    = Color(0xFF0A0A0A)
private val HeaderDark   = Color(0xFF1C1826)
private val PureWhite    = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF9B8FD4)
private val HoloPink     = Color(0xFFE8B4D8)
private val HoloMint     = Color(0xFFAEE8D8)
private val SubtleGrey   = Color(0xFFDDD8EE)
private val TextPrimary  = Color(0xFF0A0A0A)
private val TextSecondary= Color(0xFF7A7490)
private val SuccessGreen = Color(0xFF2E7D52)
private val WarningAmber = Color(0xFFF5C97A)
private val ErrorRed     = Color(0xFFE8574A)

// ═══════════════════════════════════════════════════════════════════════════════
//  GAME STATES — unchanged
// ═══════════════════════════════════════════════════════════════════════════════
enum class GameState  { INTRO, COUNTDOWN, PLAYING, RESULT }
enum class StressLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

data class Particle(
    val id: Int, val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color, val radius: Float
)

data class RoundResult(val reactionMs: Long, val correct: Boolean)

// ═══════════════════════════════════════════════════════════════════════════════
//  ACTIVITY
// ═══════════════════════════════════════════════════════════════════════════════
class Stress_Game : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StressGameScreen(onBack = { finish() })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN SCREEN — logic unchanged
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StressGameScreen(onBack: () -> Unit) {
    var gameState    by remember { mutableStateOf(GameState.INTRO) }
    var countdown    by remember { mutableIntStateOf(3) }
    var roundResults by remember { mutableStateOf(listOf<RoundResult>()) }
    var stressLevel  by remember { mutableStateOf(StressLevel.LOW) }
    var stressScore  by remember { mutableIntStateOf(0) }
    val scope        = rememberCoroutineScope()

    fun startGame() {
        roundResults = emptyList()
        scope.launch {
            gameState = GameState.COUNTDOWN
            for (i in 3 downTo 1) { countdown = i; delay(1000) }
            gameState = GameState.PLAYING
        }
    }

    fun onGameComplete(results: List<RoundResult>) {
        roundResults = results
        val avgReaction = results.map { it.reactionMs }.average()
        val accuracy    = results.count { it.correct }.toFloat() / results.size
        val variance    = results.map { it.reactionMs }.let { times ->
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

    // Full-screen dark base with CaloriX blobs
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepBlack, HeaderDark, DeepBlack)))
    ) {
        CaloriXBlobs()

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

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX AMBIENT BLOBS  — replaces BackgroundParticles
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CaloriXBlobs() {
    val inf   = rememberInfiniteTransition(label = "blobs")
    val drift by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.size(300.dp)
                .offset(x = (160 + drift * 8).dp, y = (-60 + drift * 10).dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(listOf(HoloPink.copy(0.40f), HoloMint.copy(0.15f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier.size(260.dp)
                .offset(x = (-60).dp, y = (500 - drift * 12).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(listOf(AccentViolet.copy(0.35f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier.size(200.dp)
                .offset(x = (100 + drift * 6).dp, y = (300 + drift * 8).dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(listOf(HoloMint.copy(0.20f), Color.Transparent)),
                    CircleShape
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  INTRO SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun IntroScreen(onStart: () -> Unit, onBack: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "intro")
    val glow  by inf.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Dark header banner ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(DeepBlack, DeepBlack.copy(0f)))
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Corner glow
            Box(
                Modifier.size(160.dp).align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-30).dp).blur(60.dp)
                    .background(
                        Brush.radialGradient(listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.2f), Color.Transparent)),
                        CircleShape
                    )
            )
            Column {
                Text(
                    "CaloriX",
                    color = PureWhite, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.width(32.dp).height(2.dp)
                        .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Stress Detection",
                    color = PureWhite, fontSize = 30.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "A quick cognitive game to measure\nyour real-time stress level",
                    color = PureWhite.copy(0.55f), fontSize = 14.sp, lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Brain icon with glow ring ─────────────────────────────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
            // Outer glow rings
            repeat(3) { i ->
                Box(
                    Modifier.size((110 + i * 28).dp)
                        .background(AccentViolet.copy(glow * (0.12f - i * 0.03f)), CircleShape)
                )
            }
            // Icon circle
            Box(
                Modifier.size(100.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentViolet, HoloPink), Offset.Zero, Offset(100f, 100f)),
                        CircleShape
                    )
                    .border(1.dp, PureWhite.copy(0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Shimmer
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(PureWhite.copy(0.18f), Color.Transparent)), CircleShape)
                )
                Text("MIND", color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── How it works pills ────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                Triple(AccentViolet, "10 quick rounds",       "React as fast as you can"),
                Triple(HoloPink,     "Tap the matching color", "Match the text color, not the word"),
                Triple(HoloMint,     "Get your stress score",  "Instant cognitive analysis")
            ).forEach { (dotColor, title, subtitle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PureWhite.copy(0.06f))
                        .border(1.dp, PureWhite.copy(0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(title,    color = PureWhite,        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = PureWhite.copy(0.45f), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Start button ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(AccentViolet, HoloPink)))
                .border(1.dp, PureWhite.copy(0.2f), RoundedCornerShape(18.dp))
                .clickable { onStart() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(PureWhite.copy(0.12f), Color.Transparent)), RoundedCornerShape(18.dp))
            )
            Text("Begin Assessment", color = PureWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        Spacer(Modifier.height(14.dp))

        TextButton(onClick = onBack) {
            Text("Back to Home", color = PureWhite.copy(0.4f), fontSize = 13.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  COUNTDOWN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CountdownScreen(countdown: Int) {
    val scale by animateFloatAsState(
        targetValue   = if (countdown > 0) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "countScale"
    )
    val inf = rememberInfiniteTransition(label = "cdPulse")
    val ringScale by inf.animateFloat(
        0.7f, 1.4f,
        infiniteRepeatable(tween(700), RepeatMode.Restart), label = "ring"
    )
    val ringAlpha by inf.animateFloat(
        0.5f, 0f,
        infiniteRepeatable(tween(700), RepeatMode.Restart), label = "ringAlpha"
    )
    val glowAlpha by inf.animateFloat(
        0.2f, 0.5f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ga"
    )

    Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {

        // Ambient glow
        Box(
            Modifier.size(380.dp)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(AccentViolet.copy(glowAlpha), Color.Transparent)))
                }
        )

        // Expanding ring
        Box(
            Modifier.size(220.dp).scale(ringScale)
                .border(1.5.dp, AccentViolet.copy(ringAlpha), CircleShape)
        )

        // Inner ring
        Box(
            Modifier.size(180.dp).scale(scale)
                .background(
                    Brush.radialGradient(listOf(HeaderDark, DeepBlack), radius = 300f),
                    CircleShape
                )
                .border(1.dp, AccentViolet.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(PureWhite.copy(0.08f), Color.Transparent)), CircleShape)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (countdown > 0) "$countdown" else "GO",
                    color      = if (countdown > 0) PureWhite else AccentViolet,
                    fontSize   = 64.sp,
                    fontWeight = FontWeight.W200,
                    letterSpacing = 2.sp
                )
            }
        }

        // Label
        Column(
            modifier = Modifier.offset(y = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CaloriX", color = PureWhite.copy(0.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            Text("Get in position", color = PureWhite.copy(0.5f), fontSize = 14.sp, letterSpacing = 0.5.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GAMEPLAY SCREEN — logic 100% unchanged
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun GamePlayScreen(onComplete: (List<RoundResult>) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope  = rememberCoroutineScope()

    var results        by remember { mutableStateOf(listOf<RoundResult>()) }
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
            "PURPLE" to AccentViolet
        )
    }

    var targetWord   by remember { mutableStateOf("") }
    var targetColor  by remember { mutableStateOf(Color.White) }
    var options      by remember { mutableStateOf(listOf<Pair<String, Color>>()) }
    var correctIndex by remember { mutableIntStateOf(0) }
    var showBurst    by remember { mutableStateOf(false) }
    var burstColor   by remember { mutableStateOf(SuccessGreen) }
    var roundJob     by remember { mutableStateOf<Job?>(null) }

    val progressAnim by animateFloatAsState(
        targetValue   = currentRound.toFloat() / totalRounds,
        animationSpec = tween(300), label = "progress"
    )

    fun setupRound() {
        val correct   = colorOptions.random()
        val word      = colorOptions.random().first
        val wrongOpts = colorOptions.filter { it.first != correct.first }.shuffled().take(3)
        val allOpts   = (wrongOpts + correct).shuffled()
        targetWord   = word; targetColor = correct.second
        correctIndex = allOpts.indexOf(correct); options = allOpts
        isWaiting = false; showTarget = true; roundStartTime = System.currentTimeMillis()
    }

    fun onOptionTapped(index: Int) {
        if (isWaiting || !showTarget) return
        isWaiting = true; showTarget = false
        val reactionMs  = System.currentTimeMillis() - roundStartTime
        val correct     = index == correctIndex
        val newResults  = results + RoundResult(reactionMs, correct)
        results         = newResults
        burstColor      = if (correct) SuccessGreen else ErrorRed
        showBurst       = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        roundJob?.cancel()
        roundJob = scope.launch {
            delay(800); showBurst = false
            if (newResults.size >= totalRounds) {
                onComplete(newResults)
            } else {
                currentRound++
                delay(400L + Random.nextLong(600L))
                setupRound()
            }
        }
    }

    LaunchedEffect(Unit) { delay(500); setupRound(); currentRound = 1 }
    DisposableEffect(Unit) { onDispose { roundJob?.cancel() } }

    Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
        if (showBurst) BurstEffect(color = burstColor)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Header ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("CaloriX", color = PureWhite.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Stress Test", color = PureWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = PureWhite.copy(0.08f)) {
                    Text(
                        "Round $currentRound / $totalRounds",
                        modifier  = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color     = AccentViolet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Progress bar ──────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PureWhite.copy(0.08f))
            ) {
                Box(
                    Modifier.fillMaxWidth(progressAnim).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(2.dp))
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Instruction ───────────────────────────────────────────────
            Surface(shape = RoundedCornerShape(14.dp), color = PureWhite.copy(0.06f)) {
                Text(
                    "Tap the color that matches the TEXT COLOR, not the word",
                    modifier  = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color     = PureWhite.copy(0.55f), fontSize = 12.sp,
                    textAlign = TextAlign.Center, lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Target word card ──────────────────────────────────────────
            AnimatedVisibility(
                visible = showTarget,
                enter   = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PureWhite.copy(0.06f))
                        .border(1.5.dp, targetColor.copy(0.5f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(Brush.verticalGradient(listOf(targetColor.copy(0.08f), Color.Transparent)), RoundedCornerShape(24.dp))
                    )
                    Text(
                        targetWord,
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = targetColor
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Color option buttons ──────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.chunked(2).forEachIndexed { rowIdx, rowOptions ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    color:   Color,
    label:   String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (enabled) 1f else 0.95f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "btnScale"
    )
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, tween(200), label = "btnAlpha")

    Box(
        modifier = modifier
            .height(70.dp).scale(scale).alpha(alpha)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(color.copy(0.22f), color.copy(0.08f))))
            .border(1.dp, color.copy(0.55f), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Shimmer top
        Box(
            Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, PureWhite.copy(0.15f), Color.Transparent)))
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Text(label, color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  BURST EFFECT — unchanged logic, CaloriX colors handled by caller
// ═══════════════════════════════════════════════════════════════════════════════
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
        val startTime = System.currentTimeMillis(); val duration = 800L
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            if (progress >= 1f) break; delay(16)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height / 2f
        particles.forEach { (vx, vy, r) ->
            drawCircle(
                color  = color.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                radius = r * (1f - progress * 0.5f),
                center = Offset(cx + vx * progress * 40f, cy + vy * progress * 40f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  RESULT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ResultScreen(
    stressLevel: StressLevel,
    stressScore: Int,
    results:     List<RoundResult>,
    onPlayAgain: () -> Unit,
    onBack:      () -> Unit
) {
    data class LevelInfo(val color: Color, val label: String, val title: String, val message: String)

    val info = when (stressLevel) {
        StressLevel.LOW       -> LevelInfo(HoloMint,      "LOW",       "Low Stress",       "You are calm and focused. Keep it up!")
        StressLevel.MEDIUM    -> LevelInfo(WarningAmber,  "MODERATE",  "Moderate Stress",  "Mild stress detected. Take a short break.")
        StressLevel.HIGH      -> LevelInfo(HoloPink,      "HIGH",      "High Stress",      "You seem stressed. Try deep breathing.")
        StressLevel.VERY_HIGH -> LevelInfo(ErrorRed,      "VERY HIGH", "Very High Stress", "High stress detected. Rest and hydrate now.")
    }

    val animScore by animateIntAsState(stressScore, tween(1500, easing = FastOutSlowInEasing), label = "scoreAnim")
    val sweep     by animateFloatAsState(stressScore / 100f * 360f, tween(1500, easing = FastOutSlowInEasing), label = "sweep")
    val accuracy  = if (results.isNotEmpty()) results.count { it.correct }.toFloat() / results.size * 100f else 0f
    val avgMs     = if (results.isNotEmpty()) results.map { it.reactionMs }.average().toInt() else 0

    val context = LocalContext.current

    val inf      = rememberInfiniteTransition(label = "resultGlow")
    val gitaGlow by inf.animateFloat(
        0.3f, 0.9f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gitaGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Dark header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepBlack, DeepBlack.copy(0f))))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Box(
                Modifier.size(140.dp).align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-30).dp).blur(55.dp)
                    .background(
                        Brush.radialGradient(listOf(AccentViolet.copy(0.35f), HoloPink.copy(0.15f), Color.Transparent)),
                        CircleShape
                    )
            )
            Column {
                Text("CaloriX", color = PureWhite.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Text("Your Results", color = PureWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.width(40.dp).height(2.dp)
                        .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(1.dp))
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Animated stress ring ──────────────────────────────────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
            Canvas(modifier = Modifier.size(210.dp)) {
                drawArc(
                    color = PureWhite.copy(0.08f), startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(HoloMint, WarningAmber, ErrorRed)),
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    style = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Inner glass circle
            Box(
                Modifier.size(168.dp)
                    .background(Brush.radialGradient(listOf(HeaderDark, DeepBlack), radius = 300f), CircleShape)
                    .border(1.dp, PureWhite.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(PureWhite.copy(0.06f), Color.Transparent)), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$animScore",
                        fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        color = info.color, letterSpacing = (-1).sp
                    )
                    Text("/ 100", fontSize = 12.sp, color = PureWhite.copy(0.3f))
                }
            }
        }

        // ── Stress level badge ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(info.color.copy(0.15f))
                .border(1.dp, info.color.copy(0.5f), RoundedCornerShape(50.dp))
                .padding(horizontal = 24.dp, vertical = 9.dp)
        ) {
            Text(info.label, color = info.color, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(8.dp))

        Text(info.title, color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(4.dp))
        Text(info.message, color = PureWhite.copy(0.55f), fontSize = 13.sp, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))

        // ── Stats row ─────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ResultStatCard(Modifier.weight(1f), "Accuracy",  "${accuracy.toInt()}%", AccentViolet)
            ResultStatCard(Modifier.weight(1f), "Avg Speed", "${avgMs}ms",           HoloPink)
            ResultStatCard(Modifier.weight(1f), "Rounds",    "${results.size}",      HoloMint)
        }

        Spacer(Modifier.height(24.dp))

        // ── Bhagavad Gita card ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFFF5C97A).copy(0.08f), HoloPink.copy(0.08f)))
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(WarningAmber.copy(gitaGlow), HoloPink.copy(gitaGlow))),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    // Om glow ring
                    Box(
                        Modifier.size(52.dp)
                            .background(
                                Brush.radialGradient(listOf(WarningAmber.copy(gitaGlow * 0.35f), Color.Transparent)),
                                CircleShape
                            )
                            .border(1.dp, WarningAmber.copy(gitaGlow * 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OM", color = WarningAmber, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            when (stressLevel) {
                                StressLevel.LOW       -> "Stay Peaceful"
                                StressLevel.MEDIUM    -> "Find Your Calm"
                                StressLevel.HIGH      -> "Seek Inner Peace"
                                StressLevel.VERY_HIGH -> "You Need Peace Now"
                            },
                            color = WarningAmber, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            when (stressLevel) {
                                StressLevel.LOW       -> "Read Gita shlokas to\nmaintain your inner peace"
                                StressLevel.MEDIUM    -> "Gita shlokas can help\nrestore your calm"
                                StressLevel.HIGH      -> "Read sacred shlokas for\nstress relief and clarity"
                                StressLevel.VERY_HIGH -> "Read Gita shlokas for\nimmediate relief"
                            },
                            color = PureWhite.copy(0.55f), fontSize = 12.sp, lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Gradient divider
                Box(
                    Modifier.fillMaxWidth().height(1.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, WarningAmber.copy(0.4f), Color.Transparent))
                        )
                )

                Spacer(Modifier.height(14.dp))

                // Shloka preview
                Text(
                    "\"  कर्मण्येवाधिकारस्ते मा फलेषु कदाचन  \"",
                    color     = WarningAmber.copy(0.7f), fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
                Text("Chapter 2, Verse 47", color = PureWhite.copy(0.25f), fontSize = 10.sp, textAlign = TextAlign.Center)

                Spacer(Modifier.height(16.dp))

                // Open Gita button
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(WarningAmber, HoloPink)))
                        .border(1.dp, PureWhite.copy(0.15f), RoundedCornerShape(16.dp))
                        .clickable { context.startActivity(Intent(context, bhagwat_gita::class.java)) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(Brush.verticalGradient(listOf(PureWhite.copy(0.12f), Color.Transparent)), RoundedCornerShape(16.dp))
                    )
                    Text(
                        "Open Bhagavad Gita",
                        color = PureWhite, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Round breakdown ───────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Round Breakdown", color = PureWhite.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Surface(shape = RoundedCornerShape(20.dp), color = PureWhite.copy(0.06f)) {
                    Text(
                        "${results.count { it.correct }} / ${results.size} correct",
                        modifier  = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color     = HoloMint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            results.forEachIndexed { i, result ->
                RoundRow(round = i + 1, result = result)
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Play again ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp).fillMaxWidth().height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(AccentViolet, HoloPink)))
                .border(1.dp, PureWhite.copy(0.18f), RoundedCornerShape(18.dp))
                .clickable { onPlayAgain() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(PureWhite.copy(0.12f), Color.Transparent)), RoundedCornerShape(18.dp))
            )
            Text("Play Again", color = PureWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Back to Home", color = PureWhite.copy(0.4f), fontSize = 13.sp)
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  RESULT STAT CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ResultStatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(label, color = PureWhite.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ROUND ROW
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun RoundRow(round: Int, result: RoundResult) {
    val color = if (result.correct) HoloMint else ErrorRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureWhite.copy(0.04f))
            .border(1.dp, color.copy(0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Text("Round $round", color = PureWhite.copy(0.7f), fontSize = 13.sp)
        }
        Text(
            if (result.correct) "Correct" else "Wrong",
            color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
        Surface(shape = RoundedCornerShape(8.dp), color = PureWhite.copy(0.06f)) {
            Text(
                "${result.reactionMs}ms",
                modifier  = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                color     = PureWhite.copy(0.45f), fontSize = 11.sp
            )
        }
    }
}