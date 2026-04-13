package com.example.run

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── Palette (matches HomeFragment exactly) ───────────────────────────────────
private val BgBlack       = Color(0xFF0A0A0A)
private val DeepBlack     = Color(0xFF0A0A0A)
private val PureWhite     = Color(0xFFFFFFFF)
private val AccentViolet  = Color(0xFF9B8FD4)
private val HoloPink      = Color(0xFFE8B4D8)
private val HoloMint      = Color(0xFFAEE8D8)
private val TextSecondary = Color(0xFF7A7490)

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Logic unchanged ──────────────────────────────────────────────────
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION)

        // ── Animated UI ──────────────────────────────────────────────────────
        setContent {
            MaterialTheme {
                CaloriXSplashScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getString("email", null) != null

        val intent = if (isLoggedIn) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, SignIn::class.java)
        }

        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SPLASH SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CaloriXSplashScreen() {
    // ── Staggered entry visibility flags ─────────────────────────────────────
    var showLogo    by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showPillars by remember { mutableStateOf(false) }
    var showBar     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200);  showLogo    = true
        delay(600);  showTagline = true
        delay(300);  showPillars = true
        delay(300);  showBar     = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(DeepBlack),
        contentAlignment = Alignment.Center
    ) {
        // ── Ambient blobs ─────────────────────────────────────────────────────
        SplashBlobs()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            // ── App icon + brand name ─────────────────────────────────────────
            AnimatedVisibility(
                visible = showLogo,
                enter   = fadeIn(tween(700)) + slideInVertically(tween(700)) { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon()
                    Spacer(Modifier.height(16.dp))
                    BrandName()
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Tagline ───────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showTagline,
                enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { 20 }
            ) {
                Text(
                    text          = "YOUR DAILY HEALTH PARTNER",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = TextSecondary.copy(0.75f),
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Health pillars ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showPillars,
                enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { 30 }
            ) {
                SplashPillars()
            }

            Spacer(Modifier.height(48.dp))

            // ── Progress bar ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showBar,
                enter   = fadeIn(tween(400))
            ) {
                SplashProgressBar()
            }

            Spacer(Modifier.height(16.dp))

            // ── Bouncing dots ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showBar,
                enter   = fadeIn(tween(400, 200))
            ) {
                BouncingDots()
            }
        }

        // ── "Powered by" footer ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = showBar,
            enter   = fadeIn(tween(600, 400)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
        ) {
            Text(
                "POWERED BY CALORIX AI",
                fontSize      = 9.sp,
                color         = PureWhite.copy(0.18f),
                letterSpacing = 1.5.sp,
                fontWeight    = FontWeight.Medium
            )
        }
    }
}

// ── Ambient blobs (same engine as HomeFragment) ───────────────────────────────
@Composable
private fun SplashBlobs() {
    val inf   = rememberInfiniteTransition(label = "splashBlobs")
    val drift by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (140 + drift * 8).dp, y = (-60 + drift * 10).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(listOf(HoloPink.copy(0.55f), HoloMint.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (420 - drift * 12).dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(listOf(AccentViolet.copy(0.45f), Color.Transparent)),
                    CircleShape
                )
        )
    }
}

// ── Glowing app icon ──────────────────────────────────────────────────────────
@Composable
private fun AppIcon() {
    val pulse  = rememberInfiniteTransition(label = "iconPulse")
    val glow   by pulse.animateFloat(
        0.3f, 0.65f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        Modifier
            .size(90.dp)
            .background(
                Brush.radialGradient(listOf(AccentViolet.copy(glow), Color.Transparent)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(AccentViolet, HoloPink), Offset.Zero, Offset(80f, 80f)))
                .border(1.dp, PureWhite.copy(0.2f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Shimmer overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(PureWhite.copy(0.18f), Color.Transparent)))
            )
            Text(
                "CX",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = PureWhite,
                letterSpacing = (-1).sp
            )
        }
    }
}

// ── Brand name with gradient underbar ─────────────────────────────────────────
@Composable
private fun BrandName() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "CaloriX",
            fontSize      = 42.sp,
            fontWeight    = FontWeight.Bold,
            color         = PureWhite,
            letterSpacing = (-1.5).sp
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(52.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(AccentViolet, HoloPink)),
                    RoundedCornerShape(2.dp)
                )
        )
    }
}

// ── Health pillar pills (reuses PillarPill pattern) ───────────────────────────
private data class SplashPillar(val label: String, val dot: Color)

private val splashPillars = listOf(
    SplashPillar("Workout",      HoloMint),
    SplashPillar("Hydration",    Color(0xFF6EC6F5)),
    SplashPillar("Spirituality", HoloPink),
    SplashPillar("Nutrition",    Color(0xFF8BC34A))
)

@Composable
private fun SplashPillars() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            splashPillars.take(2).forEach { SplashPill(it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            splashPillars.drop(2).forEach { SplashPill(it) }
        }
    }
}

@Composable
private fun SplashPill(p: SplashPillar) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PureWhite.copy(0.07f))
            .border(1.dp, PureWhite.copy(0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment       = Alignment.CenterVertically,
            horizontalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(5.dp).background(p.dot, CircleShape))
            Text(p.label, fontSize = 11.sp, color = PureWhite.copy(0.85f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
        }
    }
}

// ── Animated gradient progress bar ────────────────────────────────────────────
@Composable
private fun SplashProgressBar() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(4000, easing = LinearEasing))
    }
    Box(
        Modifier
            .width(160.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(PureWhite.copy(0.1f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(2.dp))
        )
    }
}

// ── Bouncing loading dots ──────────────────────────────────────────────────────
@Composable
private fun BouncingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 150, 300).forEach { delayMs ->
            val scale by inf.animateFloat(
                0.6f, 1.1f,
                infiniteRepeatable(tween(600, delayMs, FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot$delayMs"
            )
            Box(
                Modifier
                    .size((5 * scale).dp)
                    .background(AccentViolet.copy(0.5f + 0.5f * scale), CircleShape)
            )
        }
    }
}