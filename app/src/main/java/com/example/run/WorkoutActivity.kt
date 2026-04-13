package com.example.run

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import java.io.FileOutputStream

// ═══════════════════════════════════════════════════════════════════════════════
//  MAP STYLE
// ═══════════════════════════════════════════════════════════════════════════════
private const val MAP_STYLE_URL  = "https://api.maptiler.com/maps/streets-v4/style.json?key=oGsc8v2qhePidbWmiKVt"
private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID  = "route-layer"

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX PALETTE
// ═══════════════════════════════════════════════════════════════════════════════
private val BgDeep       = Color(0xFF0A0A0A)
private val HeaderDark   = Color(0xFF1C1826)
private val CardBg       = Color(0xDD0A0A0A)   // dark translucent card
private val PureWhite    = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF9B8FD4)
private val HoloPink     = Color(0xFFE8B4D8)
private val HoloMint     = Color(0xFFAEE8D8)
private val SubtleGrey   = Color(0xFFDDD8EE)
private val TextPrimary  = Color(0xFFF5F3FF)    // near-white for dark overlay
private val TextMuted    = Color(0xFF9E96C0)    // muted violet-grey
private val ErrorRed     = Color(0xFFE8574A)
private val GlassBorder  = Color(0x33FFFFFF)
private val GlassWhite   = Color(0x18FFFFFF)

private val modeIcon get() = mapOf("RUNNING" to "🏃", "WALKING" to "🚶", "CYCLING" to "🚴")

// ═══════════════════════════════════════════════════════════════════════════════
//  ACTIVITY  — ALL backend logic 100% unchanged
// ═══════════════════════════════════════════════════════════════════════════════
class WorkoutActivity : ComponentActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private var seconds     by mutableStateOf(0)
    private var isPaused    by mutableStateOf(false)
    private var distanceKm  by mutableStateOf(0.0)
    private var calories    by mutableStateOf(0.0)
    private var pace        by mutableStateOf("0:00")
    private var workoutMode by mutableStateOf("RUNNING")

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var totalDistanceMeters = 0.0
    private var lastLocation: Location? = null
    private val pathPoints = mutableListOf<LatLng>()

    var mapViewRef: MapView?      = null
    var mapLibreRef: MapLibreMap? = null
    var mapStyleReady             = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        workoutMode = intent.getStringExtra("MODE") ?: "RUNNING"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            WorkoutScreen(
                seconds     = seconds,
                isPaused    = isPaused,
                distanceKm  = distanceKm,
                calories    = calories,
                pace        = pace,
                workoutMode = workoutMode,
                onPause     = { togglePause() },
                onStop      = { stopWorkout() },
                onCenterMap = { centerMap() },
                onMapReady  = { mv, map -> mapViewRef = mv; mapLibreRef = map }
            )
        }

        if (checkLocationPermission()) startWorkout() else requestLocationPermission()
    }

    // ── All workout logic unchanged ───────────────────────────────────────
    private fun startWorkout() { startTimer(); initializeFirstLocation(); startLocationTracking() }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (!isPaused) seconds++
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun initializeFirstLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    pathPoints.add(LatLng(location.latitude, location.longitude))
                    updateMapRoute()
                    animateCameraTo(LatLng(location.latitude, location.longitude))
                    Toast.makeText(this, "Tracking started!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLocationTracking() {
        @Suppress("DEPRECATION")
        val locationRequest = LocationRequest.create().apply {
            interval             = 5000; fastestInterval = 3000
            priority             = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isPaused) return
                result.lastLocation?.let { updateDistance(it); updatePath(it) }
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateDistance(newLocation: Location) {
        lastLocation?.let { old ->
            val d = old.distanceTo(newLocation)
            if (d > 2 && d < 200) totalDistanceMeters += d
        }
        lastLocation = newLocation
        updateStats()
    }

    private fun updatePath(location: Location) {
        val ll = LatLng(location.latitude, location.longitude)
        pathPoints.add(ll); updateMapRoute(); animateCameraTo(ll)
    }

    fun updateMapRoute() {
        if (!mapStyleReady || pathPoints.size < 2) return
        val map = mapLibreRef ?: return
        map.getStyle { style ->
            val points     = pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val collection = FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points)))
            val existing   = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
            if (existing != null) {
                existing.setGeoJson(collection)
            } else {
                style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, collection))
                // ✅ Route line color updated to AccentViolet hex
                style.addLayer(
                    LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                        lineColor("#9B8FD4"),
                        lineWidth(6f),
                        lineCap("round"),
                        lineJoin("round"),
                        lineOpacity(0.95f)
                    )
                )
            }
        }
    }

    private fun animateCameraTo(latLng: LatLng) {
        val map     = mapLibreRef ?: return
        val bearing = lastLocation?.bearing?.toDouble() ?: 0.0
        val camera  = CameraPosition.Builder().target(latLng).zoom(18.5).tilt(55.0).bearing(bearing).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 1200)
    }

    private fun updateStats() {
        distanceKm = totalDistanceMeters / 1000.0
        val calPerKm = when (workoutMode) { "RUNNING" -> 60; "WALKING" -> 40; "CYCLING" -> 30; else -> 50 }
        calories = distanceKm * calPerKm
        if (distanceKm > 0.01 && seconds > 0) {
            val avgPace = (seconds / 60.0) / distanceKm
            if (avgPace.isFinite() && avgPace in 0.0..60.0) {
                val pm = avgPace.toInt(); val ps = ((avgPace - pm) * 60).toInt()
                pace = String.format("%d:%02d", pm, ps)
            } else pace = "0:00"
        } else pace = "0:00"
    }

    private fun togglePause() {
        isPaused = !isPaused
        Toast.makeText(this, if (isPaused) "Paused" else "Resumed", Toast.LENGTH_SHORT).show()
    }

    private fun centerMap() { lastLocation?.let { animateCameraTo(LatLng(it.latitude, it.longitude)) } }

    private fun stopWorkout() {
        val map = mapLibreRef
        if (map != null) {
            map.snapshot { bitmap ->
                val imagePath = bitmap?.let { saveMapToFile(it) } ?: ""
                launchSummary(imagePath)
            }
        } else launchSummary("")
    }

    private fun launchSummary(imagePath: String) {
        val intent = Intent(this, workout_summary::class.java).apply {
            putExtra("WORKOUT_MODE",   workoutMode)
            putExtra("DURATION",       formatTime(seconds))
            putExtra("DISTANCE",       String.format("%.2f", distanceKm))
            putExtra("CALORIES",       String.format("%.0f", calories))
            putExtra("PACE",           pace)
            putExtra("MAP_IMAGE_PATH", imagePath)
        }
        startActivity(intent); finish()
    }

    private fun saveMapToFile(bitmap: Bitmap): String {
        return try {
            val file = File(cacheDir, "workout_map_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            file.absolutePath
        } catch (e: Exception) { "" }
    }

    private fun checkLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startWorkout()
            else { Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show(); finish() }
        }
    }

    override fun onStart()     { super.onStart();     mapViewRef?.onStart()     }
    override fun onResume()    { super.onResume();    mapViewRef?.onResume()    }
    override fun onPause()     { super.onPause();     mapViewRef?.onPause()     }
    override fun onStop()      { super.onStop();      mapViewRef?.onStop()      }
    override fun onLowMemory() { super.onLowMemory(); mapViewRef?.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapViewRef?.onSaveInstanceState(outState) }
    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        mapViewRef?.onDestroy()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HELPERS
// ═══════════════════════════════════════════════════════════════════════════════
private fun formatTime(s: Int): String {
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return String.format("%02d:%02d:%02d", h, m, sec)
}

// ═══════════════════════════════════════════════════════════════════════════════
//  WORKOUT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkoutScreen(
    seconds:    Int,
    isPaused:   Boolean,
    distanceKm: Double,
    calories:   Double,
    pace:       String,
    workoutMode: String,
    onPause:    () -> Unit,
    onStop:     () -> Unit,
    onCenterMap: () -> Unit,
    onMapReady:  (MapView, MapLibreMap) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val inf   = rememberInfiniteTransition(label = "wblobs")
    val drift by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "wd"
    )

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {

        // ── MAP ──────────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapAsync { map ->
                        map.setStyle(MAP_STYLE_URL) { style ->
                            (ctx as? WorkoutActivity)?.mapStyleReady = true
                            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                map.locationComponent.apply {
                                    activateLocationComponent(LocationComponentActivationOptions.builder(ctx, style).build())
                                    isLocationComponentEnabled = true
                                    cameraMode = CameraMode.NONE
                                    renderMode = RenderMode.GPS
                                }
                            }
                            map.cameraPosition = CameraPosition.Builder().zoom(17.0).tilt(55.0).build()
                            (ctx as? WorkoutActivity)?.updateMapRoute()
                        }
                        map.uiSettings.isRotateGesturesEnabled = false
                        map.uiSettings.isTiltGesturesEnabled   = true
                        map.uiSettings.isLogoEnabled           = false
                        map.uiSettings.isAttributionEnabled    = true
                        onMapReady(this, map)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Gradient overlays — dark fade top + bottom ────────────────────
        Box(
            Modifier.fillMaxWidth().height(300.dp)
                .background(Brush.verticalGradient(listOf(BgDeep, Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(220.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, BgDeep)))
        )

        // ── Holographic blobs on top overlay ─────────────────────────────
        Box(
            Modifier.size(200.dp).align(Alignment.TopEnd)
                .offset(x = (40 + drift * 6).dp, y = (-30 + drift * 8).dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(listOf(HoloPink.copy(0.25f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier.size(160.dp).align(Alignment.TopStart)
                .offset(x = (-30).dp, y = (50 - drift * 6).dp)
                .blur(50.dp)
                .background(
                    Brush.radialGradient(listOf(AccentViolet.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
        )

        // ── TOP STATS ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(500)) + slideInVertically(tween(500)) { -80 }
        ) {
            TopStatsBox(
                seconds     = seconds,
                isPaused    = isPaused,
                distanceKm  = distanceKm,
                calories    = calories,
                pace        = pace,
                workoutMode = workoutMode
            )
        }

        // ── BOTTOM CONTROLS ───────────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible,
            enter    = fadeIn(tween(500)) + slideInVertically(tween(500)) { 80 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(isPaused = isPaused, onPause = onPause, onStop = onStop)
        }

        // ── CENTER FAB ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible,
            enter    = fadeIn(tween(600)) + scaleIn(tween(600)),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)
        ) {
            CenterFab(onClick = onCenterMap)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  TOP STATS BOX
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TopStatsBox(
    seconds:    Int,
    isPaused:   Boolean,
    distanceKm: Double,
    calories:   Double,
    pace:       String,
    workoutMode: String
) {
    val badgeColor by animateColorAsState(
        if (isPaused) Color(0xFFF5C97A) else HoloMint,
        tween(400), label = "badge"
    )
    val glowAlpha by animateFloatAsState(
        if (!isPaused) 0.22f else 0.05f, tween(600), label = "ga"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Top row: app name + mode chip + live badge ────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // CaloriX brand chip
            Column {
                Text(
                    "CaloriX",
                    color         = PureWhite,
                    fontSize      = 18.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Box(
                    Modifier
                        .width(36.dp).height(2.dp)
                        .background(
                            Brush.horizontalGradient(listOf(AccentViolet, HoloPink)),
                            RoundedCornerShape(1.dp)
                        )
                )
            }

            // Mode chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassWhite)
                    .border(1.dp, GlassBorder, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(modeIcon[workoutMode] ?: "🏃", fontSize = 13.sp)
                Text(
                    workoutMode, color = TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp
                )
            }

            // Live / Paused badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeColor.copy(0.12f))
                    .border(1.dp, badgeColor.copy(0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PulsingDot(badgeColor)
                Text(
                    if (isPaused) "PAUSED" else "LIVE",
                    color = badgeColor, fontWeight = FontWeight.Bold,
                    fontSize = 10.sp, letterSpacing = 2.sp
                )
            }
        }

        // ── Main stats card ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
        ) {
            // Shimmer top edge
            Box(
                Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, PureWhite.copy(0.15f), Color.Transparent))
                    )
            )
            // Violet glow behind timer
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(180.dp)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(AccentViolet.copy(glowAlpha), Color.Transparent)
                            ),
                            size.minDimension
                        )
                    }
            )

            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "DURATION",
                    color = TextMuted, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )

                // Timer — uses violet accent instead of neon green
                val timerScale by animateFloatAsState(
                    if (!isPaused && seconds % 2 == 0) 1.010f else 1f,
                    tween(500), label = "tp"
                )
                Text(
                    formatTime(seconds),
                    color         = PureWhite,
                    fontSize      = 54.sp,
                    fontWeight    = FontWeight.W200,
                    letterSpacing = 4.sp,
                    modifier      = Modifier.scale(timerScale)
                )

                // Gradient divider
                Box(
                    Modifier.fillMaxWidth(0.85f).height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AccentViolet.copy(0.4f), HoloPink.copy(0.3f), Color.Transparent)
                            )
                        )
                )

                // 3 stats row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WorkoutStatItem(label = "DISTANCE", value = String.format("%.2f", distanceKm), unit = "km")
                    Box(Modifier.width(1.dp).height(48.dp).background(GlassBorder))
                    WorkoutStatItem(label = "PACE",     value = pace,                               unit = "min/km")
                    Box(Modifier.width(1.dp).height(48.dp).background(GlassBorder))
                    WorkoutStatItem(label = "CALORIES", value = String.format("%.0f", calories),    unit = "kcal")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STAT ITEM
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkoutStatItem(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier            = Modifier.width(90.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
        AnimatedContent(
            targetState  = value,
            transitionSpec = {
                (slideInVertically { -16 } + fadeIn()) togetherWith (slideOutVertically { 16 } + fadeOut())
            },
            label = "sv"
        ) { v ->
            Text(
                v,
                color      = AccentViolet,   // ← CaloriX violet instead of NeonGreen
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        }
        Text(unit, color = TextMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  BOTTOM CONTROLS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun BottomControls(isPaused: Boolean, onPause: () -> Unit, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Stop button
        WorkoutControlButton(
            label    = "■",
            sublabel = "STOP",
            color    = ErrorRed,
            modifier = Modifier.weight(1f),
            onClick  = onStop
        )
        // Pause / Resume — violet gradient when resumed, pink when paused
        WorkoutPauseButton(
            isPaused = isPaused,
            onClick  = onPause,
            modifier = Modifier.weight(1.6f)
        )
    }
}

@Composable
private fun WorkoutPauseButton(isPaused: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale       by animateFloatAsState(if (isPaused) 1.04f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "ps")
    val bgBrush     = if (isPaused)
        Brush.linearGradient(listOf(AccentViolet, HoloPink))
    else
        Brush.linearGradient(listOf(AccentViolet.copy(0.15f), HoloPink.copy(0.1f)))
    val textColor   by animateColorAsState(if (isPaused) PureWhite else AccentViolet, tween(300), label = "pt")
    val borderBrush = if (isPaused)
        Brush.linearGradient(listOf(AccentViolet, HoloPink))
    else
        Brush.linearGradient(listOf(AccentViolet.copy(0.5f), HoloPink.copy(0.3f)))

    Box(
        modifier = modifier
            .scale(scale).height(64.dp).clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.5.dp, borderBrush, RoundedCornerShape(20.dp))
            .clickable(remember { MutableInteractionSource() }, ripple(color = AccentViolet)) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isPaused) "▶" else "⏸", color = textColor, fontSize = 22.sp)
            Text(
                if (isPaused) "RESUME" else "PAUSE",
                color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun WorkoutControlButton(
    label: String, sublabel: String, color: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bs")

    Box(
        modifier = modifier
            .scale(scale).height(64.dp).clip(RoundedCornerShape(20.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(20.dp))
            .clickable(remember { MutableInteractionSource() }, ripple(color = color)) { pressed = true; onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label,    color = color, fontSize = 20.sp)
            Text(sublabel, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CENTER FAB  — glass style matching HomeFragment map overlay
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun CenterFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(AccentViolet.copy(0.3f), HeaderDark))
            )
            .border(1.dp, AccentViolet.copy(0.5f), CircleShape)
            .clickable(remember { MutableInteractionSource() }, ripple(color = AccentViolet)) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("⊙", fontSize = 20.sp, color = PureWhite)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PULSING DOT
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PulsingDot(color: Color) {
    val inf   = rememberInfiniteTransition(label = "dot")
    val alpha by inf.animateFloat(
        1f, 0.2f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "da"
    )
    Box(Modifier.size(6.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}