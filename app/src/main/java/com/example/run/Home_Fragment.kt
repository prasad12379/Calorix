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
//  CALORIX PALETTE
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
//  WORKOUT MODE DATA
// ═══════════════════════════════════════════════════════════════════════════════
private data class WorkoutMode(
    val key:         String,
    val title:       String,
    val subtitle:    String,
    val stat:        String,      // e.g. "Avg 5 km"
    val backDetail:  String,      // shown on flipped back
    val gradient:    List<Color>
)

private val workoutModes = listOf(
    WorkoutMode(
        key        = "RUNNING",
        title      = "Running",
        subtitle   = "High Intensity",
        stat       = "Avg 5 km",
        backDetail = "Burns ~400 kcal\nBoosts cardio\nImproves mood",
        gradient   = listOf(Color(0xFF2A1F4A), AccentViolet)
    ),
    WorkoutMode(
        key        = "WALKING",
        title      = "Walking",
        subtitle   = "Low Impact",
        stat       = "Avg 3 km",
        backDetail = "Burns ~200 kcal\nEasy on joints\nClears the mind",
        gradient   = listOf(Color(0xFF0F2A1A), Color(0xFF2E7D52))
    ),
    WorkoutMode(
        key        = "CYCLING",
        title      = "Cycling",
        subtitle   = "Endurance",
        stat       = "Avg 12 km",
        backDetail = "Burns ~350 kcal\nBuilds leg power\nFull body burn",
        gradient   = listOf(Color(0xFF2A1A0F), Color(0xFF8B5E28))
    )
)

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
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        HolographicBlobs()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(visible, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 }) {
                PremiumHeader(onNotifications = onNotifications)
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { 30 }) {
                FeatureShowcaseStrip(
                    onWaterTracker = onWaterTracker,
                    onStressGame   = onStressGame,
                    onBhagwatGita  = onBhagwatGita,
                    onMili         = onMili
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── PREMIUM MAP CARD ──────────────────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(600, 200)) + scaleIn(tween(600, 200), 0.95f)) {
                MapTilerCard()
            }

            Spacer(Modifier.height(24.dp))

            // ── WORKOUT MODE FLIP CARDS ───────────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(500, 300)) + slideInVertically(tween(500, 300)) { 40 }) {
                WorkoutFlipSection(onStartWorkout = onStartWorkout)
            }

            // Bottom padding so last card clears the floating nav bar
            Spacer(Modifier.height(140.dp))
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
                .background(Brush.radialGradient(listOf(HoloPink.copy(0.55f), HoloMint.copy(0.2f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (520 - drift * 12).dp)
                .blur(70.dp)
                .background(Brush.radialGradient(listOf(AccentViolet.copy(0.45f), Color.Transparent)), CircleShape)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PREMIUM HEADER
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun PremiumHeader(onNotifications: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826))))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Box(
            Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .blur(60.dp)
                .background(Brush.radialGradient(listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.2f), Color.Transparent)), CircleShape)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text("CaloriX", color = PureWhite, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.width(60.dp).height(3.dp)
                        .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(2.dp))
                )
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
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = PureWhite, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEALTH PILLARS TICKER
// ═══════════════════════════════════════════════════════════════════════════════
private data class HealthPillar(val label: String, val dotColor: Color, val pillGradient: List<Color>)

private val healthPillars = listOf(
    HealthPillar("Workout for 30 min",  HoloMint,          listOf(Color(0xFF1A2A1A), Color(0xFF0F1F2A))),
    HealthPillar("Hydration 5 ltr",     Color(0xFF6EC6F5), listOf(Color(0xFF0F1F2A), Color(0xFF1A1A2E))),
    HealthPillar("Spirituality 10 min", HoloPink,          listOf(Color(0xFF2A1A2E), Color(0xFF1C1826))),
    HealthPillar("Eat healthy",         Color(0xFF8BC34A), listOf(Color(0xFF1B2A1A), Color(0xFF0F1F0F)))
)

@Composable
fun HealthPillarsTicker() {
    Spacer(Modifier.height(12.dp))
    Text("Your daily unstoppable formula", fontSize = 10.sp, color = TextSecondary.copy(0.70f), fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            healthPillars.take(2).forEach { PillarPill(it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            healthPillars.drop(2).forEach { PillarPill(it) }
        }
    }
}

@Composable
private fun PillarPill(pillar: HealthPillar) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(pillar.pillGradient.map { it.copy(0.18f) }, Offset.Zero, Offset(200f, 60f)))
            .background(PureWhite.copy(0.08f))
            .background(Brush.linearGradient(listOf(PureWhite.copy(0.18f), Color.Transparent, PureWhite.copy(0.05f)), Offset.Zero, Offset(200f, 80f)))
            .border(1.dp, PureWhite.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(5.dp).background(pillar.dotColor, CircleShape))
            Text(pillar.label, fontSize = 11.sp, color = PureWhite.copy(0.92f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
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
//  FEATURE SHOWCASE STRIP
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
        AppFeature(R.drawable.feature_hydration, "Hydration",       "Daily Habit",    "Smart water reminders keep you perfectly hydrated throughout the day. Set your daily goal, track every sip, and let CaloriX nudge you at the right moments so you never fall short.", listOf(Color(0xFF6EC6F5), HoloMint),  onWaterTracker),
        AppFeature(R.drawable.feature_gita,      "Mental Health",   "Mindfulness",    "Begin each day with a timeless shloka from the Bhagavad Gita. Curated verses paired with calm reflections help you build emotional resilience and inner clarity — one thought at a time.",              listOf(Color(0xFFF5C97A), HoloPink),  onBhagwatGita),
        AppFeature(R.drawable.feature_stress,    "Stress Check",    "Mental Fitness", "Our interactive stress-relief game uses proven micro-exercises to lower cortisol levels in minutes. Play, breathe, and reset — your mind deserves a workout too.",                                       listOf(AccentViolet, HoloPink),       onStressGame),
        AppFeature(R.drawable.feature_mili,      "Mili — Coach",    "AI Powered",     "Mili is your personalised fitness chatbot. Ask about nutrition, training plans, recovery, or anything health-related. Mili learns your goals and delivers advice that actually fits your life.",        listOf(DeepBlack, Color(0xFF2A1F4A)), onMili)
    )

    val pagerState    = rememberPagerState(pageCount = { features.size })
    val scope         = rememberCoroutineScope()
    val flippedStates = remember { mutableStateListOf(*Array(features.size) { false }) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            delay(3_000)
            if (!flippedStates[settledPage]) pagerState.animateScrollToPage((settledPage + 1) % features.size)
        }
    }
    flippedStates.forEachIndexed { index, isFlipped ->
        LaunchedEffect(isFlipped) { if (isFlipped) { delay(4_000); flippedStates[index] = false } }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Features", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
                Text("Everything CaloriX offers", fontSize = 11.sp, color = TextSecondary, letterSpacing = 0.2.sp)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = BgLavender) {
                Text("${pagerState.currentPage + 1} / ${features.size}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 11.sp, color = AccentViolet, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 24.dp), pageSpacing = 16.dp, modifier = Modifier.fillMaxWidth()) { page ->
            FeatureFlipCard(
                feature   = features[page],
                isFlipped = flippedStates[page],
                onFlip    = { flippedStates[page] = !flippedStates[page] },
                onExplore = { flippedStates[page] = false; features[page].onNavigate() }
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(features.size) { i ->
                val selected = pagerState.currentPage == i
                val width by animateDpAsState(if (selected) 24.dp else 6.dp, tween(300), label = "dw$i")
                Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(width).clip(CircleShape).background(if (selected) AccentViolet else SubtleGrey).clickable { scope.launch { pagerState.animateScrollToPage(i) } })
            }
        }
    }
}

@Composable
private fun FeatureFlipCard(feature: AppFeature, isFlipped: Boolean, onFlip: () -> Unit, onExplore: () -> Unit) {
    val rotation by animateFloatAsState(if (isFlipped) 180f else 0f, tween(520, easing = FastOutSlowInEasing), label = "flip")
    Box(Modifier.fillMaxWidth().height(300.dp).graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }.clickable { onFlip() }) {
        if (rotation <= 90f) CardFront(feature)
        else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { CardBack(feature, onExplore) }
    }
}

@Composable
private fun CardFront(feature: AppFeature) {
    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), color = PureWhite, border = BorderStroke(1.dp, SubtleGrey), shadowElevation = 10.dp) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(0.70f)) {
                Image(painterResource(feature.imageRes), feature.heading, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)))
                Box(Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, PureWhite.copy(0.85f)))))
                Surface(Modifier.padding(14.dp).align(Alignment.TopStart), RoundedCornerShape(20.dp), DeepBlack.copy(0.72f)) {
                    Text(feature.tag, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                }
                Surface(Modifier.padding(12.dp).align(Alignment.BottomEnd), RoundedCornerShape(20.dp), AccentViolet.copy(0.18f), border = BorderStroke(0.8.dp, AccentViolet.copy(0.35f))) {
                    Text("Tap to flip", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 9.sp, color = AccentViolet, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                }
            }
            Row(Modifier.fillMaxWidth().weight(0.30f).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(feature.heading, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.4).sp)
                Box(Modifier.size(36.dp).background(BgLavender, CircleShape), contentAlignment = Alignment.Center) {
                    Text("→", fontSize = 15.sp, color = AccentViolet, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CardBack(feature: AppFeature, onExplore: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), color = DeepBlack, shadowElevation = 10.dp) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(Brush.horizontalGradient(feature.accentColors), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)))
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Surface(shape = RoundedCornerShape(20.dp), color = PureWhite.copy(0.08f), border = BorderStroke(0.8.dp, SubtleGrey.copy(0.25f))) {
                        Text(feature.tag.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 9.sp, color = TextSecondary.copy(0.85f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(feature.heading, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(48.dp).height(3.dp).background(Brush.horizontalGradient(feature.accentColors), CircleShape))
                    Spacer(Modifier.height(14.dp))
                    Text(feature.description, fontSize = 13.sp, color = PureWhite.copy(0.75f), lineHeight = 20.sp, textAlign = TextAlign.Start)
                }
                Box(Modifier.fillMaxWidth().height(50.dp).background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(16.dp)).clickable { onExplore() }, contentAlignment = Alignment.Center) {
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
//  MAPTILER 3D CARD  — FIXED: uses loadUrl() with proper geolocation support
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MapTilerCard() {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Live Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("3D view  •  Real time", fontSize = 11.sp, color = TextSecondary)
            }
            // Premium badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("LIVE", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Multi-layer premium border effect
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = AccentViolet.copy(0.3f), spotColor = HoloPink.copy(0.2f))
                .background(Brush.linearGradient(listOf(AccentViolet.copy(0.6f), HoloPink.copy(0.5f), HoloMint.copy(0.4f))), RoundedCornerShape(28.dp))
                .padding(1.5.dp)
        ) {
            Surface(
                modifier        = Modifier.fillMaxSize(),
                shape           = RoundedCornerShape(27.dp),
                color           = DeepBlack,
                shadowElevation = 0.dp
            ) {
                Box {
                    // WebView — loads the self-hosted HTML via data URL with maptiler base URL
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled             = true
                                    domStorageEnabled             = true
                                    loadWithOverviewMode          = true
                                    useWideViewPort               = true
                                    mixedContentMode              = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode                     = WebSettings.LOAD_DEFAULT
                                    allowContentAccess            = true
                                    allowFileAccess               = true
                                    setSupportZoom(true)
                                    builtInZoomControls           = false
                                    displayZoomControls           = false
                                    // Required for geolocation in WebView
                                    setGeolocationEnabled(true)
                                    databaseEnabled               = true
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String) {
                                        super.onPageFinished(view, url)
                                    }
                                }
                                // Grant geolocation permission automatically
                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onGeolocationPermissionsShowPrompt(
                                        origin: String,
                                        callback: android.webkit.GeolocationPermissions.Callback
                                    ) {
                                        callback.invoke(origin, true, false)
                                    }
                                }
                                setBackgroundColor(android.graphics.Color.parseColor("#0A0A0A"))

                                val html = buildMapHtml()
                                // loadDataWithBaseURL so MapTiler CDN scripts resolve correctly
                                loadDataWithBaseURL(
                                    "https://api.maptiler.com/maps/streets-v4/style.json?key=oGsc8v2qhePidbWmiKVt",
                                    html,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update   = { },
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(27.dp))
                    )

                    // Overlay: top-right live indicator
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepBlack.copy(0.75f))
                            .border(0.5.dp, AccentViolet.copy(0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val pulse = rememberInfiniteTransition(label = "mp")
                        val sc by pulse.animateFloat(0.7f, 1.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "sc")
                        Box(Modifier.size((5 * sc).dp).background(HoloMint, CircleShape))
                        Text("GPS Active", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun buildMapHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<script src="https://cdn.maptiler.com/maptiler-sdk-js/v2.4.1/maptiler-sdk.umd.min.js"></script>
<link href="https://cdn.maptiler.com/maptiler-sdk-js/v2.4.1/maptiler-sdk.css" rel="stylesheet"/>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  html, body { width:100%; height:100%; overflow:hidden; background:#0A0A0A; }
  #map { width:100%; height:100%; }
  .badge {
    position:absolute; bottom:12px; left:12px;
    background:linear-gradient(135deg,#9B8FD4,#E8B4D8);
    border-radius:20px; padding:4px 12px;
    color:#fff; font-size:10px; font-weight:700;
    letter-spacing:0.6px; font-family:sans-serif;
    pointer-events:none;
  }
</style>
</head>
<body>
<div id="map"></div>
<div class="badge">&#128205; Tracking</div>
<script>
  maptilersdk.config.apiKey = 'oGsc8v2qhePidbWmiKVt';

  const map = new maptilersdk.Map({
    container: 'map',
    style: maptilersdk.MapStyle.STREETS,
    center: [73.8567, 18.5204],
    zoom: 14,
    pitch: 50,
    bearing: -15,
    antialias: true,
    navigationControl: false,
    geolocateControl: false,
    attributionControl: false
  });

  map.on('load', function() {
    // 3-D buildings
    if (map.getLayer('building')) {
      map.setPaintProperty('building', 'fill-extrusion-height', ['get','height']);
      map.setPaintProperty('building', 'fill-extrusion-color', '#1C1826');
      map.setPaintProperty('building', 'fill-extrusion-opacity', 0.8);
    }
  });

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      function(pos) {
        var lng = pos.coords.longitude;
        var lat = pos.coords.latitude;

        map.flyTo({
          center: [lng, lat],
          zoom: 16,
          pitch: 54,
          bearing: -15,
          speed: 1.2,
          curve: 1.4,
          essential: true
        });

        map.on('load', function() {
          // User location source
          map.addSource('me', {
            type: 'geojson',
            data: { type:'Feature', geometry:{ type:'Point', coordinates:[lng, lat] } }
          });

          // Outer pulse ring
          map.addLayer({
            id: 'me-ring',
            type: 'circle',
            source: 'me',
            paint: {
              'circle-radius': 26,
              'circle-color': '#9B8FD4',
              'circle-opacity': 0.20,
              'circle-stroke-width': 0
            }
          });

          // Inner dot
          map.addLayer({
            id: 'me-dot',
            type: 'circle',
            source: 'me',
            paint: {
              'circle-radius': 10,
              'circle-color': '#9B8FD4',
              'circle-opacity': 0.95,
              'circle-stroke-width': 3,
              'circle-stroke-color': '#FAF9FF'
            }
          });
        });
      },
      function() {},
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }
</script>
</body>
</html>
""".trimIndent()

// ═══════════════════════════════════════════════════════════════════════════════
//  WORKOUT FLIP SECTION  — 3 equal-width cards, vertical Y-axis flip
//  Front: mode name + subtitle + stat
//  Back : detail text + "Tap to Start" button → launches CountActivity
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun WorkoutFlipSection(onStartWorkout: (String) -> Unit) {
    // Track which card is flipped; only one at a time
    val flippedIndex = remember { mutableIntStateOf(-1) }

    // Auto-reset flipped card after 5 s
    val currentFlipped = flippedIndex.intValue
    LaunchedEffect(currentFlipped) {
        if (currentFlipped >= 0) {
            delay(5_000)
            flippedIndex.intValue = -1
        }
    }

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Choose Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            workoutModes.forEachIndexed { index, mode ->
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

@Composable
private fun WorkoutModeCard(
    mode:      WorkoutMode,
    isFlipped: Boolean,
    modifier:  Modifier,
    onFlip:    () -> Unit,
    onStart:   () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue   = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "wflip_${mode.key}"
    )
    val isFront = rotation <= 90f

    Box(
        modifier
            .height(160.dp)
            .graphicsLayer {
                rotationY      = rotation
                cameraDistance = 10f * density
            }
            .clickable { onFlip() }
    ) {
        if (isFront) {
            WorkoutCardFront(mode = mode)
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                WorkoutCardBack(mode = mode, onStart = onStart)
            }
        }
    }
}

// ── Workout FRONT ─────────────────────────────────────────────────────────────
@Composable
private fun WorkoutCardFront(mode: WorkoutMode) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(mode.gradient, Offset.Zero, Offset(0f, 400f)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(PureWhite.copy(0.25f), PureWhite.copy(0.08f))),
                RoundedCornerShape(20.dp)
            )
    ) {
        // Top shimmer line
        Box(
            Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, PureWhite.copy(0.3f), Color.Transparent)))
        )

        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement   = Arrangement.SpaceBetween
        ) {
            // Top: subtitle pill
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PureWhite.copy(0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(mode.subtitle, fontSize = 9.sp, color = PureWhite.copy(0.85f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }

            // Bottom: title + stat + tap hint
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    mode.title,
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = PureWhite,
                    letterSpacing = (-0.3).sp
                )
                Text(mode.stat, fontSize = 10.sp, color = PureWhite.copy(0.65f))
                Spacer(Modifier.height(6.dp))
                // Tap indicator
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(3.dp).background(HoloMint, CircleShape))
                    Text("Tap to flip", fontSize = 8.sp, color = PureWhite.copy(0.50f))
                }
            }
        }
    }
}

// ── Workout BACK ──────────────────────────────────────────────────────────────
@Composable
private fun WorkoutCardBack(mode: WorkoutMode, onStart: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepBlack)
            .border(
                1.dp,
                Brush.linearGradient(mode.gradient),
                RoundedCornerShape(20.dp)
            )
    ) {
        // Gradient top strip
        Box(
            Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(mode.gradient), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        )

        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mode.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = (-0.2).sp)
                // Detail lines
                mode.backDetail.split("\n").forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            Modifier.size(3.dp)
                                .background(Brush.linearGradient(mode.gradient), CircleShape)
                        )
                        Text(line, fontSize = 9.sp, color = PureWhite.copy(0.70f))
                    }
                }
            }

            // START button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(mode.gradient))
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "START  →",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = PureWhite,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}