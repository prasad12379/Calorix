package com.example.run

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX PALETTE — single source of truth for the entire file
// ═══════════════════════════════════════════════════════════════════════════════
private val BgWhite       = Color(0xFFFAF9FF)
private val BgLavender    = Color(0xFFECE8F5)
private val DeepBlack     = Color(0xFF0A0A0A)
private val PureWhite     = Color(0xFFFFFFFF)
private val AccentViolet  = Color(0xFF9B8FD4)
private val HoloPink      = Color(0xFFE8B4D8)
private val HoloMint      = Color(0xFFAEE8D8)
private val SubtleGrey    = Color(0xFFDDD8EE)
private val TextPrimary   = Color(0xFF0A0A0A)
private val TextSecondary = Color(0xFF7A7490)

// ═══════════════════════════════════════════════════════════════════════════════
//  FRAGMENT
// ═══════════════════════════════════════════════════════════════════════════════
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    CaloriXHomeScreen(
                        onStartWorkout  = { mode ->
                            startActivity(
                                Intent(requireContext(), CountActivity::class.java)
                                    .putExtra("MODE", mode)
                            )
                        },
                        onWaterTracker  = { startActivity(Intent(requireContext(), water_tracker::class.java)) },
                        onStressGame    = { startActivity(Intent(requireContext(), Stress_Game::class.java)) },
                        onBhagwatGita   = { startActivity(Intent(requireContext(), bhagwat_gita::class.java)) },
                        onNotifications = { startActivity(Intent(requireContext(), NotificationsActivity::class.java)) },
                        onMili          = { startActivity(Intent(requireContext(), ChatbotFragment::class.java)) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CaloriXHomeScreen(
    onStartWorkout:  (String) -> Unit,
    onWaterTracker:  () -> Unit,
    onStressGame:    () -> Unit,
    onBhagwatGita:   () -> Unit,
    onNotifications: () -> Unit,
    onMili:          () -> Unit
) {
    var selectedMode by remember { mutableStateOf("RUNNING") }
    var visible      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(80); visible = true }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        HolographicBlobs()

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── HEADER ────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 }
            ) {
                PremiumHeader(onNotifications = onNotifications)
            }

            Spacer(Modifier.height(20.dp))

            // ── FEATURE SHOWCASE ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { 30 }
            ) {
                FeatureShowcaseStrip(
                    onWaterTracker = onWaterTracker,
                    onStressGame   = onStressGame,
                    onBhagwatGita  = onBhagwatGita,
                    onMili         = onMili
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── 3D MAP ────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(600, 200)) + scaleIn(tween(600, 200), 0.95f)
            ) {
                MapTilerCard()
            }

            Spacer(Modifier.height(24.dp))

            // ── WORKOUT LAUNCH ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(500, 300)) + slideInVertically(tween(500, 300)) { 40 }
            ) {
                WorkoutLaunchCard(
                    selectedMode = selectedMode,
                    onModeChange = { selectedMode = it },
                    onStart      = { onStartWorkout(selectedMode) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── QUICK ACCESS ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(500, 400)) + slideInVertically(tween(500, 400)) { 40 }
            ) {

            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HOLO BLOBS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun HolographicBlobs() {
    val inf   = rememberInfiniteTransition(label = "blobs")
    val drift by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (160 + drift * 8).dp, y = (-60 + drift * 10).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(listOf(HoloPink.copy(0.55f), HoloMint.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (520 - drift * 12).dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(listOf(AccentViolet.copy(0.45f), Color.Transparent)),
                    CircleShape
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PREMIUM HEADER  (CaloriX title + underline + health pillars ticker)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun PremiumHeader(onNotifications: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826))))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Holo shimmer blob
        Box(
            Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "CaloriX",
                    color         = PureWhite,
                    fontSize      = 30.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(4.dp))
                // Gradient underline
                Box(
                    Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(AccentViolet, HoloPink)),
                            RoundedCornerShape(2.dp)
                        )
                )
                // Health pillars ticker sits right below
                HealthPillarsTicker()
            }

            Spacer(Modifier.width(12.dp))

            // Notification bell
            Box(
                Modifier
                    .size(46.dp)
                    .background(PureWhite.copy(0.08f), CircleShape)
                    .border(1.dp, SubtleGrey.copy(0.3f), CircleShape)
                    .clickable { onNotifications() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint               = PureWhite,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEALTH PILLARS TICKER
// ═══════════════════════════════════════════════════════════════════════════════
private data class HealthPillar(
    val label:        String,
    val dotColor:     Color,
    val pillGradient: List<Color>
)

private val healthPillars = listOf(
    HealthPillar("Workout for 30 min",  HoloMint, listOf(Color(0xFF1A2A1A), Color(0xFF0F1F2A))),
    HealthPillar("Hydration 5 ltr", Color(0xFF6EC6F5), listOf(Color(0xFF0F1F2A), Color(0xFF1A1A2E))),
    HealthPillar("Spirituality 10 min ", HoloPink, listOf(Color(0xFF2A1A2E), Color(0xFF1C1826))),
    HealthPillar("Eat healthy", Color(0xFF8BC34A), listOf(Color(0xFF1B2A1A), Color(0xFF0F1F0F))) // ✅ added
)

@Composable
fun HealthPillarsTicker() {

    // ❌ Removed animation logic (visibleFlags, shimmer, dot)

    Spacer(Modifier.height(12.dp))

    Text(
        "Your daily unstoppable formula",
        fontSize      = 10.sp,
        color         = TextSecondary.copy(0.70f),
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.8.sp
    )

    Spacer(Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            healthPillars.take(2).forEachIndexed { idx, pillar ->
                PillarPill(pillar, true, 0f, 1f) // ✅ static values
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            healthPillars.drop(2).forEachIndexed { idx, pillar ->
                PillarPill(pillar, true, 0f, 1f) // ✅ static values
            }
        }
    }
}

@Composable
private fun PillarPill(
    pillar:        HealthPillar,
    visible:       Boolean,
    shimmerOffset: Float,
    dotScale:      Float
) {
    val alpha   by animateFloatAsState(if (visible) 1f else 0f, tween(420), label = "pa")
    val offsetY by animateDpAsState(if (visible) 0.dp else 10.dp, tween(420),  label = "py")

    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .graphicsLayer(alpha = alpha)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    pillar.pillGradient.map { it.copy(alpha = 0.18f) }, // 🔥 make gradient transparent
                    Offset.Zero,
                    Offset(200f, 60f)
                )
            )
            .background(
                Color.White.copy(alpha = 0.08f) // 🔥 main glass layer
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f), // top light reflection
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(200f, 80f)
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.25f), // subtle glass border
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size((5f * dotScale).dp).background(pillar.dotColor, CircleShape))
            Text(pillar.label, fontSize = 11.sp, color = PureWhite.copy(0.92f),
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FEATURE DATA MODEL
// ═══════════════════════════════════════════════════════════════════════════════
data class AppFeature(
    val imageRes:     Int,
    val heading:      String,
    val tag:          String,
    val description:  String,
    val accentColors: List<Color>,
    val onNavigate:   () -> Unit
)

// ═══════════════════════════════════════════════════════════════════════════════
//  FEATURE SHOWCASE STRIP  — flip cards + fixed auto-slide
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeatureShowcaseStrip(
    onWaterTracker: () -> Unit,
    onStressGame:   () -> Unit,
    onBhagwatGita:  () -> Unit,
    onMili:         () -> Unit
) {
    val features = listOf(
        AppFeature(
            imageRes     = R.drawable.feature_hydration,
            heading      = "Hydration",
            tag          = "Daily Habit",
            description  = "Smart water reminders keep you perfectly hydrated throughout the day. " +
                    "Set your daily goal, track every sip, and let CaloriX nudge you " +
                    "at the right moments so you never fall short.",
            accentColors = listOf(Color(0xFF6EC6F5), HoloMint),
            onNavigate   = onWaterTracker
        ),
        AppFeature(
            imageRes     = R.drawable.feature_gita,
            heading      = "Mental Health",
            tag          = "Mindfulness",
            description  = "Begin each day with a timeless shloka from the Bhagavad Gita. " +
                    "Curated verses paired with calm reflections help you build " +
                    "emotional resilience and inner clarity — one thought at a time.",
            accentColors = listOf(Color(0xFFF5C97A), HoloPink),
            onNavigate   = onBhagwatGita
        ),
        AppFeature(
            imageRes     = R.drawable.feature_stress,
            heading      = "Stress Check",
            tag          = "Mental Fitness",
            description  = "Our interactive stress-relief game uses proven micro-exercises to " +
                    "lower cortisol levels in minutes. Play, breathe, and reset — " +
                    "your mind deserves a workout too.",
            accentColors = listOf(AccentViolet, HoloPink),
            onNavigate   = onStressGame
        ),
        AppFeature(
            imageRes     = R.drawable.feature_mili,
            heading      = "Mili — Your Coach",
            tag          = "AI Powered",
            description  = "Mili is your personalised fitness chatbot. Ask about nutrition, " +
                    "training plans, recovery, or anything health-related. " +
                    "Mili learns your goals and delivers advice that actually fits your life.",
            accentColors = listOf(DeepBlack, Color(0xFF2A1F4A)),
            onNavigate   = onMili
        )
    )

    val pagerState    = rememberPagerState(pageCount = { features.size })
    val scope         = rememberCoroutineScope()
    val flippedStates = remember { mutableStateListOf(*Array(features.size) { false }) }

    // ── FIXED AUTO-SLIDE ──────────────────────────────────────────────────────
    // Keys on pagerState (stable), listens to settledPage (fires only when card
    // is 100 % at rest), animateScrollToPage suspends until fully done.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            delay(3_000)
            if (!flippedStates[settledPage]) {
                pagerState.animateScrollToPage((settledPage + 1) % features.size)
            }
        }
    }

    // Auto-reset flipped card after 4 s
    flippedStates.forEachIndexed { index, isFlipped ->
        LaunchedEffect(isFlipped) {
            if (isFlipped) { delay(4_000); flippedStates[index] = false }
        }
    }

    Column(Modifier.fillMaxWidth()) {

        // Section header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Features",                  fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
                Text("Everything CaloriX offers", fontSize = 11.sp, color = TextSecondary, letterSpacing = 0.2.sp)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = BgLavender) {
                Text(
                    "${pagerState.currentPage + 1} / ${features.size}",
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize   = 11.sp,
                    color      = AccentViolet,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        HorizontalPager(
            state          = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing    = 16.dp,
            modifier       = Modifier.fillMaxWidth()
        ) { page ->
            FeatureFlipCard(
                feature   = features[page],
                isFlipped = flippedStates[page],
                onFlip    = { flippedStates[page] = !flippedStates[page] },
                onExplore = {
                    flippedStates[page] = false
                    features[page].onNavigate()
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Dot indicator
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(features.size) { i ->
                val selected = pagerState.currentPage == i
                val width by animateDpAsState(if (selected) 24.dp else 6.dp, tween(300), label = "dw$i")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (selected) AccentViolet else SubtleGrey)
                        .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FLIP CARD CONTAINER
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun FeatureFlipCard(
    feature:   AppFeature,
    isFlipped: Boolean,
    onFlip:    () -> Unit,
    onExplore: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue   = if (isFlipped) 180f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label         = "flip"
    )
    val isFrontVisible = rotation <= 90f

    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer {
                rotationY      = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() }
    ) {
        if (isFrontVisible) {
            CardFront(feature)
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                CardBack(feature, onExplore)
            }
        }
    }
}

// ── FRONT FACE ────────────────────────────────────────────────────────────────
@Composable
private fun CardFront(feature: AppFeature) {
    Surface(
        modifier        = Modifier.fillMaxSize(),
        shape           = RoundedCornerShape(24.dp),
        color           = PureWhite,
        border          = BorderStroke(1.dp, SubtleGrey),
        shadowElevation = 10.dp
    ) {
        Column(Modifier.fillMaxSize()) {

            Box(Modifier.fillMaxWidth().weight(0.70f)) {
                Image(
                    painter            = painterResource(id = feature.imageRes),
                    contentDescription = feature.heading,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                )
                // Bottom scrim
                Box(
                    Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, PureWhite.copy(0.85f))))
                )
                // Tag pill
                Surface(
                    modifier = Modifier.padding(14.dp).align(Alignment.TopStart),
                    shape    = RoundedCornerShape(20.dp),
                    color    = DeepBlack.copy(0.72f)
                ) {
                    Text(feature.tag,
                        modifier      = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize      = 10.sp, color = PureWhite,
                        fontWeight    = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                }
                // Tap-to-flip hint
                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.BottomEnd),
                    shape    = RoundedCornerShape(20.dp),
                    color    = AccentViolet.copy(0.18f),
                    border   = BorderStroke(0.8.dp, AccentViolet.copy(0.35f))
                ) {
                    Text("Tap to flip",
                        modifier      = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize      = 9.sp, color = AccentViolet,
                        fontWeight    = FontWeight.Medium, letterSpacing = 0.5.sp)
                }
            }

            Row(
                Modifier.fillMaxWidth().weight(0.30f).padding(horizontal = 18.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(feature.heading, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, letterSpacing = (-0.4).sp)
                Box(
                    Modifier.size(36.dp).background(BgLavender, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("→", fontSize = 15.sp, color = AccentViolet, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── BACK FACE ─────────────────────────────────────────────────────────────────
@Composable
private fun CardBack(feature: AppFeature, onExplore: () -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxSize(),
        shape           = RoundedCornerShape(24.dp),
        color           = DeepBlack,
        shadowElevation = 10.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            // Accent strip
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .background(Brush.horizontalGradient(feature.accentColors),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            )

            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(shape = RoundedCornerShape(20.dp), color = PureWhite.copy(0.08f),
                        border = BorderStroke(0.8.dp, SubtleGrey.copy(0.25f))) {
                        Text(feature.tag.uppercase(),
                            modifier      = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize      = 9.sp, color = TextSecondary.copy(0.85f),
                            fontWeight    = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(feature.heading, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = PureWhite, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(48.dp).height(3.dp)
                        .background(Brush.horizontalGradient(feature.accentColors), CircleShape))
                    Spacer(Modifier.height(14.dp))
                    Text(feature.description, fontSize = 13.sp, color = PureWhite.copy(0.75f),
                        lineHeight = 20.sp, textAlign = TextAlign.Start)
                }

                // Explore button
                Box(
                    Modifier.fillMaxWidth().height(50.dp)
                        .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(16.dp))
                        .clickable { onExplore() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Explore", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = 0.5.sp)
                        Text("→",       fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MAPTILER 3D CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MapTilerCard() {
    val mapHtml = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script src="https://cdn.maptiler.com/maptiler-sdk-js/v2.4.1/maptiler-sdk.umd.min.js"></script>
<link href="https://cdn.maptiler.com/maptiler-sdk-js/v2.4.1/maptiler-sdk.css" rel="stylesheet"/>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { width:100vw; height:100vh; overflow:hidden; background:#0A0A0A; }
  #map { width:100%; height:100%; }
  .compass-overlay {
    position:absolute; top:12px; right:12px;
    background:rgba(10,10,10,0.75); backdrop-filter:blur(8px);
    border:1px solid rgba(155,143,212,0.4); border-radius:50%;
    width:36px; height:36px; display:flex; align-items:center;
    justify-content:center; color:#9B8FD4; font-size:16px; pointer-events:none;
  }
  .mode-badge {
    position:absolute; bottom:14px; left:14px;
    background:linear-gradient(135deg,#9B8FD4,#E8B4D8); border-radius:20px;
    padding:5px 14px; color:#fff; font-size:11px; font-weight:600;
    letter-spacing:0.5px; font-family:sans-serif;
  }
</style>
</head>
<body>
<div id="map"></div>
<div class="compass-overlay">&#x1F9ED;</div>
<div class="mode-badge">Live Location</div>
<script>
  maptilersdk.config.apiKey = 'oGsc8v2qhePidbWmiKVt';
  const map = new maptilersdk.Map({
    container:'map',
    style:'https://api.maptiler.com/maps/streets-v4/style.json?key=oGsc8v2qhePidbWmiKVt',
    center:[73.8567,18.5204], zoom:15, pitch:52, bearing:-20, antialias:true
  });
  map.addControl(new maptilersdk.NavigationControl({showCompass:false}),'top-left');
  if(navigator.geolocation){
    navigator.geolocation.getCurrentPosition(pos=>{
      const{longitude,latitude}=pos.coords;
      map.flyTo({center:[longitude,latitude],zoom:16,pitch:52,bearing:-15,speed:1.4,curve:1.2});
      map.addSource('user-loc',{type:'geojson',data:{type:'Feature',geometry:{type:'Point',coordinates:[longitude,latitude]}}});
      map.on('load',()=>{
        map.addLayer({id:'user-dot',type:'circle',source:'user-loc',
          paint:{'circle-radius':10,'circle-color':'#9B8FD4','circle-opacity':0.9,'circle-stroke-width':3,'circle-stroke-color':'#FAF9FF'}});
        map.addLayer({id:'user-pulse',type:'circle',source:'user-loc',
          paint:{'circle-radius':22,'circle-color':'#9B8FD4','circle-opacity':0.25}});
      });
    },()=>{},{enableHighAccuracy:true});
  }
</script>
</body>
</html>
    """.trimIndent()

    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Live Map", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(shape = RoundedCornerShape(20.dp), color = BgLavender) {
                Text("3D View", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp, color = AccentViolet, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().height(240.dp)
                .background(Brush.linearGradient(listOf(AccentViolet.copy(0.5f), HoloPink.copy(0.4f), HoloMint.copy(0.3f))), RoundedCornerShape(24.dp))
                .padding(1.5.dp)
        ) {
            Surface(Modifier.fillMaxSize(), RoundedCornerShape(23.dp), DeepBlack, shadowElevation = 12.dp) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled    = true
                                domStorageEnabled    = true
                                loadWithOverviewMode = true
                                useWideViewPort      = true
                                mixedContentMode     = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode            = WebSettings.LOAD_NO_CACHE
                                allowContentAccess   = true
                                allowFileAccess      = true
                                setSupportZoom(true)
                                builtInZoomControls  = false
                                displayZoomControls  = false
                            }
                            webViewClient = WebViewClient()
                            setBackgroundColor(android.graphics.Color.parseColor("#0A0A0A"))
                            loadDataWithBaseURL("https://api.maptiler.com", mapHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(23.dp))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  WORKOUT LAUNCH CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun WorkoutLaunchCard(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    onStart:      () -> Unit
) {
    val modes = listOf(Triple("RUNNING","🏃","Run"), Triple("WALKING","🚶","Walk"), Triple("CYCLING","🚴","Cycle"))
    val inf        = rememberInfiniteTransition(label = "glow")
    val glowAlpha  by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "ga")

    Surface(
        modifier        = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape           = RoundedCornerShape(24.dp),
        color           = PureWhite,
        border          = BorderStroke(1.dp, SubtleGrey),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Start Workout",              fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Choose your activity mode",  fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                modes.forEach { (mode, emoji, label) ->
                    val selected = selectedMode == mode
                    Surface(
                        onClick         = { onModeChange(mode) },
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(16.dp),
                        color           = if (selected) AccentViolet else BgLavender,
                        border          = BorderStroke(1.5.dp, if (selected) AccentViolet else SubtleGrey),
                        shadowElevation = if (selected) 6.dp else 1.dp
                    ) {
                        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color      = if (selected) PureWhite else TextSecondary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier.fillMaxWidth().height(56.dp)
                    .shadow((16 * glowAlpha).dp, RoundedCornerShape(18.dp), ambientColor = AccentViolet, spotColor = AccentViolet)
                    .background(Brush.horizontalGradient(listOf(DeepBlack, Color(0xFF2A1F4A))), RoundedCornerShape(18.dp))
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val dotScale by inf.animateFloat(0.7f, 1.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot")
                    Box(Modifier.size((8 * dotScale).dp).background(HoloMint, CircleShape))
                    Text("START  →", color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
    }
}



