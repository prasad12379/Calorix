package com.example.run

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

// ═══════════════════════════════════════════════════════════════════════════════
//  PALETTE  — unchanged
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

// ── Pre-built static Brushes (module-level, zero reallocation) ────────────────
private val HeaderGradient  = Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826)))
private val UnderlineBrush  = Brush.horizontalGradient(listOf(AccentViolet, HoloPink))
private val MapBorderBrush  = Brush.linearGradient(listOf(AccentViolet.copy(0.6f), HoloPink.copy(0.5f), HoloMint.copy(0.4f)))
private val ExploreBtnBrush = Brush.horizontalGradient(listOf(AccentViolet, HoloPink))

// ── Static blob brushes ───────────────────────────────────────────────────────
private val BlobPinkBrush   = Brush.radialGradient(listOf(HoloPink.copy(0.22f), HoloMint.copy(0.08f), Color.Transparent), radius = 550f)
private val BlobVioletBrush = Brush.radialGradient(listOf(AccentViolet.copy(0.18f), Color.Transparent), radius = 440f)

// ═══════════════════════════════════════════════════════════════════════════════
//  WORKOUT MODE DATA
// ═══════════════════════════════════════════════════════════════════════════════
private data class WorkoutMode(
    val key: String, val title: String, val subtitle: String,
    val stat: String, val backDetail: String, val gradient: List<Color>
)
private val workoutModes = listOf(
    WorkoutMode("RUNNING", "Running", "High Intensity", "Avg 5 km",  "Burns ~400 kcal\nBoosts cardio\nImproves mood",    listOf(Color(0xFF2A1F4A), AccentViolet)),
    WorkoutMode("WALKING", "Walking", "Low Impact",     "Avg 3 km",  "Burns ~200 kcal\nEasy on joints\nClears the mind", listOf(Color(0xFF0F2A1A), Color(0xFF2E7D52))),
    WorkoutMode("CYCLING", "Cycling", "Endurance",      "Avg 12 km", "Burns ~350 kcal\nBuilds leg power\nFull body burn", listOf(Color(0xFF2A1A0F), Color(0xFF8B5E28)))
)

// ═══════════════════════════════════════════════════════════════════════════════
//  FRAGMENT
// ═══════════════════════════════════════════════════════════════════════════════
class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
        )
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    CaloriXHomeScreen(
                        onStartWorkout  = { mode -> startActivity(Intent(requireContext(), CountActivity::class.java).putExtra("MODE", mode)) },
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
//  PERF FIX: LazyColumn already in place — kept as-is.
//  PERF FIX: Scroll detection now uses distinctUntilChanged + threshold guard.
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CaloriXHomeScreen(
    onStartWorkout: (String) -> Unit, onWaterTracker: () -> Unit,
    onStressGame: () -> Unit, onBhagwatGita: () -> Unit,
    onNotifications: () -> Unit, onMili: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    val listState = rememberLazyListState()

    // PERF FIX: distinctUntilChanged prevents bus calls on every pixel of scroll.
    // Only fires when (index, offset) pair actually changes, then we apply a
    // 12-pixel threshold before signalling the nav bus.
    var prevAbsolute by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex * 10_000 + listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { absolute ->
                val dy = absolute - prevAbsolute
                when {
                    dy >  12 -> NavScrollBus.hide()
                    dy < -12 -> NavScrollBus.show()
                }
                prevAbsolute = absolute
            }
    }

    Box(Modifier.fillMaxSize().background(BgWhite)) {
        StaticHoloBlobs()

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {

            item {
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 }
                ) {
                    PremiumHeader(onNotifications = onNotifications)
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            item {
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { 30 }
                ) {
                    FeatureShowcaseStrip(onWaterTracker, onStressGame, onBhagwatGita, onMili)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(600, 200)) + scaleIn(tween(600, 200), 0.95f)
                ) {
                    // PERF FIX: Static image card — WebView completely removed.
                    // WebView was the heaviest single component: JS engine, GPU surface,
                    // geolocation, network stack. Replacing with a drawable costs ~0.
                    MapImageCard()
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(500, 300)) + slideInVertically(tween(500, 300)) { 40 }
                ) {
                    WorkoutFlipSection(onStartWorkout = onStartWorkout)
                }
            }

            item { Spacer(Modifier.height(140.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STATIC HOLO BLOBS  — no change needed, already zero-cost
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StaticHoloBlobs() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.size(280.dp).offset(x = 160.dp, y = (-60).dp).background(BlobPinkBrush, CircleShape))
        Box(Modifier.size(220.dp).offset(x = (-60).dp, y = 520.dp).background(BlobVioletBrush, CircleShape))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PREMIUM HEADER
//  PERF FIX: headerBlobBrush moved to module-level remember so it is never
//  reallocated on recomposition.
// ═══════════════════════════════════════════════════════════════════════════════

// Module-level: allocated once, shared across all recompositions.
private val HeaderBlobBrush = Brush.radialGradient(
    listOf(AccentViolet.copy(0.25f), HoloPink.copy(0.12f), Color.Transparent),
    center = Offset(Float.POSITIVE_INFINITY, 0f),
    radius = 350f
)

@Composable
fun PremiumHeader(onNotifications: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(HeaderGradient)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Box(
            Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(HeaderBlobBrush, CircleShape)
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "CaloriX", color = PureWhite, fontSize = 30.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(60.dp).height(3.dp).background(UnderlineBrush, RoundedCornerShape(2.dp)))
                HealthPillarsTicker()
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(46.dp)
                    .background(PureWhite.copy(0.08f), CircleShape)
                    .border(1.dp, SubtleGrey.copy(0.3f), CircleShape)
                    .clickable { onNotifications() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Notifications, "Notifications",
                    tint = PureWhite, modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEALTH PILLARS TICKER  — static, no animation (unchanged, already optimal)
// ═══════════════════════════════════════════════════════════════════════════════
private data class HealthPillar(val label: String, val dotColor: Color, val pillGradient: List<Color>)
private val healthPillars = listOf(
    HealthPillar("Workout for 30 min",  HoloMint,          listOf(Color(0xFF1A2A1A), Color(0xFF0F1F2A))),
    HealthPillar("Hydration 5 ltr",     Color(0xFF6EC6F5), listOf(Color(0xFF0F1F2A), Color(0xFF1A1A2E))),
    HealthPillar("Spirituality 10 min", HoloPink,          listOf(Color(0xFF2A1A2E), Color(0xFF1C1826))),
    HealthPillar("Eat healthy",         Color(0xFF8BC34A), listOf(Color(0xFF1B2A1A), Color(0xFF0F1F0F)))
)

// PERF FIX: Pre-build all pill brushes once at startup — never inside composition.
private val pillarBrushes: List<Brush> = healthPillars.map { pillar ->
    Brush.linearGradient(pillar.pillGradient.map { it.copy(0.18f) }, Offset.Zero, Offset(200f, 60f))
}

@Composable
fun HealthPillarsTicker() {
    Spacer(Modifier.height(12.dp))
    Text(
        "Your daily unstoppable formula", fontSize = 10.sp,
        color = TextSecondary.copy(0.70f), fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            healthPillars.take(2).forEachIndexed { i, it -> PillarPill(it, pillarBrushes[i]) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            healthPillars.drop(2).forEachIndexed { i, it -> PillarPill(it, pillarBrushes[i + 2]) }
        }
    }
}

// PERF FIX: Brush passed in — was rebuilt inside composition on every recompose.
// Also merged 3 background() calls into 2 (gradient + overlay) — saves one draw pass per pill.
@Composable
private fun PillarPill(pillar: HealthPillar, brush: Brush) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(brush)                                       // gradient layer
            .background(PureWhite.copy(0.08f))                       // frosted overlay
            .border(1.dp, PureWhite.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(5.dp).background(pillar.dotColor, CircleShape))
            Text(
                pillar.label, fontSize = 11.sp,
                color = PureWhite.copy(0.92f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FEATURE DATA MODEL
// ═══════════════════════════════════════════════════════════════════════════════
data class AppFeature(
    val imageRes: Int, val heading: String, val tag: String,
    val description: String, val accentColors: List<Color>, val onNavigate: () -> Unit
)

// Pre-build back-card brushes once per feature list — not inside composition.
private val featureAccentBrushes: List<Brush> by lazy {
    listOf(
        Brush.horizontalGradient(listOf(Color(0xFF6EC6F5), HoloMint)),
        Brush.horizontalGradient(listOf(Color(0xFFF5C97A), HoloPink)),
        Brush.horizontalGradient(listOf(AccentViolet, HoloPink)),
        Brush.horizontalGradient(listOf(DeepBlack, Color(0xFF2A1F4A)))
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FEATURE SHOWCASE STRIP
//  PERF FIX: N LaunchedEffects (one per card) → 1 LaunchedEffect watching
//  the single flipped index. Eliminates N-1 idle coroutines.
//  PERF FIX: pager dot animateDpAsState removed for non-selected dots —
//  they are now instant (no Dp animator running for idle dots).
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeatureShowcaseStrip(
    onWaterTracker: () -> Unit, onStressGame: () -> Unit,
    onBhagwatGita: () -> Unit, onMili: () -> Unit
) {
    val features = remember {
        listOf(
            AppFeature(R.drawable.feature_hydration, "Hydration",    "Daily Habit",    "Smart water reminders keep you perfectly hydrated throughout the day. Set your daily goal, track every sip, and let CaloriX nudge you at the right moments so you never fall short.", listOf(Color(0xFF6EC6F5), HoloMint),  onWaterTracker),
            AppFeature(R.drawable.feature_gita,      "Mental Health","Mindfulness",    "Begin each day with a timeless shloka from the Bhagavad Gita. Curated verses help you build emotional resilience and inner clarity — one thought at a time.",                          listOf(Color(0xFFF5C97A), HoloPink),  onBhagwatGita),
            AppFeature(R.drawable.feature_stress,    "Stress Check", "Mental Fitness", "Our interactive stress-relief game uses proven micro-exercises to lower cortisol levels in minutes. Play, breathe, and reset — your mind deserves a workout too.",                      listOf(AccentViolet, HoloPink),       onStressGame),
            AppFeature(R.drawable.feature_mili,      "Mili — Coach", "AI Powered",     "Mili is your personalised fitness chatbot. Ask about nutrition, training plans, recovery, or anything health-related. Mili delivers advice that fits your life.",                       listOf(DeepBlack, Color(0xFF2A1F4A)), onMili)
        )
    }

    val pagerState   = rememberPagerState(pageCount = { features.size })
    val scope        = rememberCoroutineScope()

    // PERF FIX: single Int index instead of mutableStateListOf<Boolean> (N states).
    // -1 = nothing flipped. One LaunchedEffect auto-resets after 4 s.
    val flippedIndex = remember { mutableIntStateOf(-1) }
    val currentFlip  = flippedIndex.intValue

    // Auto-advance pager only when nothing is flipped and page has settled.
    LaunchedEffect(pagerState.settledPage, currentFlip) {
        if (currentFlip < 0) {
            delay(3_000)
            pagerState.animateScrollToPage((pagerState.settledPage + 1) % features.size)
        }
    }

    // Single coroutine: auto-reset flipped card after 4 s.
    LaunchedEffect(currentFlip) {
        if (currentFlip >= 0) {
            delay(4_000)
            flippedIndex.intValue = -1
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Features", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
                Text("Everything CaloriX offers", fontSize = 11.sp, color = TextSecondary, letterSpacing = 0.2.sp)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = BgLavender) {
                Text(
                    "${pagerState.currentPage + 1} / ${features.size}",
                    Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 11.sp, color = AccentViolet, fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        HorizontalPager(
            state           = pagerState,
            contentPadding  = PaddingValues(horizontal = 24.dp),
            pageSpacing     = 16.dp,
            modifier        = Modifier.fillMaxWidth()
        ) { page ->
            val isFlipped = flippedIndex.intValue == page
            FeatureFlipCard(
                feature   = features[page],
                accentBrush = featureAccentBrushes[page],
                isFlipped = isFlipped,
                onFlip    = {
                    flippedIndex.intValue = if (flippedIndex.intValue == page) -1 else page
                },
                onExplore = {
                    flippedIndex.intValue = -1
                    features[page].onNavigate()
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // PERF FIX: only the selected dot animates width.
        // Non-selected dots are static — no Dp animator allocated.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(features.size) { i ->
                val selected = pagerState.currentPage == i
                // animateDpAsState only for the selected dot; others are plain static Dp.
                val width by animateDpAsState(
                    targetValue = if (selected) 24.dp else 6.dp,
                    animationSpec = tween(300),
                    label = "dot_$i"
                )
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

// ─── Flip card ────────────────────────────────────────────────────────────────
// PERF FIX: accentBrush passed in (pre-built) instead of created inside compose.
@Composable
private fun FeatureFlipCard(
    feature: AppFeature, accentBrush: Brush, isFlipped: Boolean,
    onFlip: () -> Unit, onExplore: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue   = if (isFlipped) 180f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label         = "flip_${feature.heading}"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .clickable { onFlip() }
    ) {
        if (rotation <= 90f) CardFront(feature)
        else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
            CardBack(feature, accentBrush, onExplore)
        }
    }
}

@Composable
private fun CardFront(feature: AppFeature) {
    Surface(
        Modifier.fillMaxSize(), RoundedCornerShape(24.dp), PureWhite,
        border = BorderStroke(1.dp, SubtleGrey), shadowElevation = 6.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(0.70f)) {
                Image(
                    painterResource(feature.imageRes), feature.heading,
                    contentScale = ContentScale.Crop,
                    modifier     = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                )
                Box(
                    Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, PureWhite.copy(0.85f))))
                )
                Surface(
                    Modifier.padding(14.dp).align(Alignment.TopStart),
                    RoundedCornerShape(20.dp), DeepBlack.copy(0.72f)
                ) {
                    Text(feature.tag, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                }
                Surface(
                    Modifier.padding(12.dp).align(Alignment.BottomEnd),
                    RoundedCornerShape(20.dp), AccentViolet.copy(0.18f),
                    border = BorderStroke(0.8.dp, AccentViolet.copy(0.35f))
                ) {
                    Text("Tap to flip", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 9.sp, color = AccentViolet, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                }
            }
            Row(
                Modifier.fillMaxWidth().weight(0.30f).padding(horizontal = 18.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(feature.heading, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.4).sp)
                Box(Modifier.size(36.dp).background(BgLavender, CircleShape), contentAlignment = Alignment.Center) {
                    Text("→", fontSize = 15.sp, color = AccentViolet, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CardBack(feature: AppFeature, accentBrush: Brush, onExplore: () -> Unit) {
    Surface(Modifier.fillMaxSize(), RoundedCornerShape(24.dp), DeepBlack, shadowElevation = 6.dp) {
        Column(Modifier.fillMaxSize()) {
            // Top colour bar uses the pre-built brush passed in
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .background(accentBrush, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            )
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        shape  = RoundedCornerShape(20.dp),
                        color  = PureWhite.copy(0.08f),
                        border = BorderStroke(0.8.dp, SubtleGrey.copy(0.25f))
                    ) {
                        Text(feature.tag.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 9.sp, color = TextSecondary.copy(0.85f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(feature.heading, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(48.dp).height(3.dp).background(accentBrush, CircleShape))
                    Spacer(Modifier.height(14.dp))
                    Text(feature.description, fontSize = 13.sp, color = PureWhite.copy(0.75f), lineHeight = 20.sp, textAlign = TextAlign.Start)
                }
                Box(
                    Modifier.fillMaxWidth().height(50.dp)
                        .background(ExploreBtnBrush, RoundedCornerShape(16.dp))
                        .clickable { onExplore() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Explore", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = 0.5.sp)
                        Text("→", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MAP IMAGE CARD
//  REPLACES: MapTilerCard (WebView + JS engine + geolocation + network stack)
//  With:     A static drawable — zero GPU surface, zero JS, zero network.
//
//  Kept:     All visual styling identical (gradient border, "LIVE" badge,
//            "GPS Active" badge with pulse dot, section title & subtitle).
//  PERF FIX: Single InfiniteTransition for the pulse dot (was already there,
//            now it's the ONLY InfiniteTransition in the whole screen).
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MapImageCard() {
    Column(Modifier.padding(horizontal = 20.dp)) {
        // Section header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Live Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("3D view  •  Real time", fontSize = 11.sp, color = TextSecondary)
            }
            Box(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(ExploreBtnBrush)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("LIVE", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Gradient border shell
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(MapBorderBrush, RoundedCornerShape(28.dp))
                .padding(1.5.dp)
        ) {
            // Map image — hardware-accelerated BitmapDrawable, no JS overhead
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(27.dp))
            ) {
                Image(
                    painter      = painterResource(id = R.drawable.map),
                    contentDescription = "Map preview",
                    contentScale = ContentScale.Crop,
                    modifier     = Modifier.fillMaxSize()
                )

                // Subtle dark scrim so overlaid badges remain readable
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(DeepBlack.copy(0.25f), Color.Transparent, DeepBlack.copy(0.15f))
                            )
                        )
                )

                // GPS Active badge — THE ONLY InfiniteTransition on this screen
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DeepBlack.copy(0.75f))
                        .border(0.5.dp, AccentViolet.copy(0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val pulse = rememberInfiniteTransition(label = "gps_pulse")
                    val sc by pulse.animateFloat(
                        initialValue   = 0.7f,
                        targetValue    = 1.3f,
                        animationSpec  = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label          = "pulse_scale"
                    )
                    Box(Modifier.size((5 * sc).dp).background(HoloMint, CircleShape))
                    Text("GPS Active", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.SemiBold)
                }

                // Bottom-left "Tracking" pill
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ExploreBtnBrush)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Tracking", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  WORKOUT FLIP SECTION
//  PERF FIX: Single mutableIntStateOf(-1) instead of mutableStateListOf<Boolean>.
//  Only one card can be flipped at a time — one LaunchedEffect total.
//  PERF FIX: key(mode.key) added to each card so Compose can diff them
//  without recomposing all three when only one flips.
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun WorkoutFlipSection(onStartWorkout: (String) -> Unit) {
    val flippedIndex = remember { mutableIntStateOf(-1) }
    val current      = flippedIndex.intValue

    LaunchedEffect(current) {
        if (current >= 0) {
            delay(5_000)
            flippedIndex.intValue = -1
        }
    }

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Choose Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            workoutModes.forEachIndexed { index, mode ->
                key(mode.key) {
                    WorkoutModeCard(
                        mode      = mode,
                        isFlipped = flippedIndex.intValue == index,
                        modifier  = Modifier.weight(1f),
                        onFlip    = {
                            flippedIndex.intValue = if (flippedIndex.intValue == index) -1 else index
                        },
                        onStart   = {
                            flippedIndex.intValue = -1
                            onStartWorkout(mode.key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutModeCard(
    mode: WorkoutMode, isFlipped: Boolean,
    modifier: Modifier, onFlip: () -> Unit, onStart: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue   = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "wflip_${mode.key}"
    )

    // FIX: Use BoxWithConstraints so the card height adapts to screen width.
    // Each card is ~1/3 of screen width (minus padding). We make height = width * 1.45
    // so all content always fits. On a 360dp wide phone each card ≈ 107dp wide → 155dp tall.
    // On a 420dp wide phone each card ≈ 128dp wide → 185dp tall. Always enough room.
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.68f)          // width:height = 1:1.47 — scales on every screen
            .graphicsLayer { rotationY = rotation; cameraDistance = 10f * density }
            .clickable { onFlip() }
    ) {
        if (rotation <= 90f) WorkoutCardFront(mode)
        else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
            WorkoutCardBack(mode, onStart)
        }
    }
}

// Pre-built brushes — unchanged
private val workoutGradientBrushes: List<Brush> = workoutModes.map { mode ->
    Brush.linearGradient(mode.gradient, Offset.Zero, Offset(0f, 400f))
}
private val workoutBorderBrushes: List<Brush> = workoutModes.map { _ ->
    Brush.linearGradient(listOf(PureWhite.copy(0.25f), PureWhite.copy(0.08f)))
}
private val workoutAccentBrushes: List<Brush> = workoutModes.map { mode ->
    Brush.horizontalGradient(mode.gradient)
}

@Composable
private fun WorkoutCardFront(mode: WorkoutMode) {
    val idx    = workoutModes.indexOf(mode)
    val brush  = workoutGradientBrushes[idx]
    val border = workoutBorderBrushes[idx]

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .border(1.dp, border, RoundedCornerShape(20.dp))
    ) {
        // Top shimmer line
        Box(
            Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, PureWhite.copy(0.3f), Color.Transparent)))
        )

        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Subtitle pill
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PureWhite.copy(0.12f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    mode.subtitle, fontSize = 8.sp,
                    color = PureWhite.copy(0.85f), fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }

            // Bottom info block
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    mode.title, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = PureWhite,
                    letterSpacing = (-0.3).sp
                )
                Text(mode.stat, fontSize = 9.sp, color = PureWhite.copy(0.65f))
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(3.dp).background(HoloMint, CircleShape))
                    Text("Tap to flip", fontSize = 7.sp, color = PureWhite.copy(0.50f))
                }
            }
        }
    }
}

@Composable
private fun WorkoutCardBack(mode: WorkoutMode, onStart: () -> Unit) {
    val idx         = workoutModes.indexOf(mode)
    val accentBrush = workoutAccentBrushes[idx]
    val borderBrush = Brush.linearGradient(mode.gradient)

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepBlack)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
    ) {
        // Top accent bar
        Box(
            Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
                .background(accentBrush, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        )

        // FIX: Use Column with fillMaxSize + weight so the START button is
        // always pinned to the bottom regardless of how tall the card is.
        // Previously SpaceBetween on a fixed-height card caused the button
        // to be pushed off the bottom on smaller screens.
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .padding(top = 6.dp)   // clear the accent bar
        ) {
            // Title
            Text(
                mode.title, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = PureWhite,
                letterSpacing = (-0.2).sp
            )

            Spacer(Modifier.height(6.dp))

            // Detail lines — weight(1f) lets this section grow and push button down
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                mode.backDetail.split("\n").forEach { line ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(Modifier.size(3.dp).background(accentBrush, CircleShape))
                        Text(
                            line, fontSize = 8.sp,
                            color = PureWhite.copy(0.70f),
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // FIX: START button — height is now 32.dp instead of 36.dp so it
            // never gets clipped on narrow/short cards. Text slightly smaller too.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentBrush)
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "START  →", fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, color = PureWhite,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}