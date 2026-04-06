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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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

// ─────────────────────────────────────────────
//  MAPTILER SATELLITE STYLE
// ─────────────────────────────────────────────
private const val MAP_STYLE_URL =
    "https://api.maptiler.com/maps/streets-v4/style.json?key=oGsc8v2qhePidbWmiKVt"

private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID  = "route-layer"

// ─────────────────────────────────────────────
//  THEME
// ─────────────────────────────────────────────
private val NeonGreen    = Color(0xFF00FF87)
private val NeonGreenDim = Color(0xFF00C96B)
private val DeepBlack    = Color(0xFF050A0E)
private val GlassWhite   = Color(0x22FFFFFF)
private val GlassBorder  = Color(0x44FFFFFF)
private val TextPrimary  = Color(0xFFF0F4F8)
private val TextMuted    = Color(0xFF8899AA)
private val ErrorRed     = Color(0xFFFF4757)
private val CardBg       = Color(0xCC0D1117)

// ─────────────────────────────────────────────
//  ACTIVITY
// ─────────────────────────────────────────────
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
                onMapReady  = { mv, map ->
                    mapViewRef  = mv
                    mapLibreRef = map
                }
            )
        }

        if (checkLocationPermission()) startWorkout() else requestLocationPermission()
    }

    // ── Workout logic ─────────────────────────────────────────────────────

    private fun startWorkout() {
        startTimer()
        initializeFirstLocation()
        startLocationTracking()
    }

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
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
            interval             = 5000
            fastestInterval      = 3000
            priority             = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isPaused) return
                result.lastLocation?.let {
                    updateDistance(it)
                    updatePath(it)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
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
        pathPoints.add(ll)
        updateMapRoute()
        animateCameraTo(ll)
    }

    fun updateMapRoute() {
        if (!mapStyleReady || pathPoints.size < 2) return
        val map = mapLibreRef ?: return
        map.getStyle { style ->
            val points = pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val collection = FeatureCollection.fromFeature(
                Feature.fromGeometry(LineString.fromLngLats(points))
            )
            val existing = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
            if (existing != null) {
                existing.setGeoJson(collection)
            } else {
                style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, collection))
                style.addLayer(
                    LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                        lineColor("#00FF87"),
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
        val camera  = CameraPosition.Builder()
            .target(latLng)
            .zoom(18.5)
            .tilt(55.0)
            .bearing(bearing)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 1200)
    }

    private fun updateStats() {
        distanceKm = totalDistanceMeters / 1000.0
        val calPerKm = when (workoutMode) {
            "RUNNING" -> 60; "WALKING" -> 40; "CYCLING" -> 30; else -> 50
        }
        calories = distanceKm * calPerKm
        if (distanceKm > 0.01 && seconds > 0) {
            val avgPace = (seconds / 60.0) / distanceKm
            if (avgPace.isFinite() && avgPace in 0.0..60.0) {
                val pm = avgPace.toInt()
                val ps = ((avgPace - pm) * 60).toInt()
                pace = String.format("%d:%02d", pm, ps)
            } else pace = "0:00"
        } else pace = "0:00"
    }

    private fun togglePause() {
        isPaused = !isPaused
        Toast.makeText(this, if (isPaused) "Paused" else "Resumed", Toast.LENGTH_SHORT).show()
    }

    private fun centerMap() {
        lastLocation?.let { animateCameraTo(LatLng(it.latitude, it.longitude)) }
    }

    // ── FIXED: Screenshot captured via MapLibre's snapshot API ────────────
    private fun stopWorkout() {
        val map = mapLibreRef
        if (map != null) {
            // Use MapLibre's built-in snapshot — works reliably
            map.snapshot { bitmap ->
                val imagePath = bitmap?.let { saveMapToFile(it) } ?: ""
                launchSummary(imagePath)
            }
        } else {
            launchSummary("")
        }
    }

    private fun launchSummary(imagePath: String) {
        val intent = Intent(this, WorkoutSummaryActivity::class.java).apply {
            putExtra("WORKOUT_MODE",   workoutMode)
            putExtra("DURATION",       formatTime(seconds))
            putExtra("DISTANCE",       String.format("%.2f", distanceKm))
            putExtra("CALORIES",       String.format("%.0f", calories))
            putExtra("PACE",           pace)
            putExtra("MAP_IMAGE_PATH", imagePath)
        }
        startActivity(intent)
        finish()
    }

    private fun saveMapToFile(bitmap: Bitmap): String {
        return try {
            val file = File(cacheDir, "workout_map_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            file.absolutePath
        } catch (e: Exception) { "" }
    }

    // ── Permissions ───────────────────────────────────────────────────────

    private fun checkLocationPermission() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startWorkout()
            else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onStart()     { super.onStart();     mapViewRef?.onStart()     }
    override fun onResume()    { super.onResume();    mapViewRef?.onResume()    }
    override fun onPause()     { super.onPause();     mapViewRef?.onPause()     }
    override fun onStop()      { super.onStop();      mapViewRef?.onStop()      }
    override fun onLowMemory() { super.onLowMemory(); mapViewRef?.onLowMemory() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewRef?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        mapViewRef?.onDestroy()
    }
}

// ─────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────
private fun formatTime(s: Int): String {
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return String.format("%02d:%02d:%02d", h, m, sec)
}

private val modeIcon get() = mapOf(
    "RUNNING" to "🏃", "WALKING" to "🚶", "CYCLING" to "🚴"
)

// ─────────────────────────────────────────────
//  ROOT COMPOSE SCREEN
// ─────────────────────────────────────────────
@Composable
private fun WorkoutScreen(
    seconds: Int,
    isPaused: Boolean,
    distanceKm: Double,
    calories: Double,
    pace: String,
    workoutMode: String,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onCenterMap: () -> Unit,
    onMapReady: (MapView, MapLibreMap) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {

        // ── MAP ──────────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapAsync { map ->
                        map.setStyle(MAP_STYLE_URL) { style ->
                            (ctx as? WorkoutActivity)?.mapStyleReady = true
                            if (ActivityCompat.checkSelfPermission(
                                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                map.locationComponent.apply {
                                    activateLocationComponent(
                                        LocationComponentActivationOptions
                                            .builder(ctx, style).build()
                                    )
                                    isLocationComponentEnabled = true
                                    cameraMode = CameraMode.NONE
                                    renderMode = RenderMode.GPS
                                }
                            }
                            map.cameraPosition = CameraPosition.Builder()
                                .zoom(17.0).tilt(55.0).build()
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

        // ── TOP FADE ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(DeepBlack, Color.Transparent)))
        )

        // ── BOTTOM FADE ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, DeepBlack)))
        )

        // ── TOP STATS BOX ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn() + slideInVertically { -80 }
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

        // ── BOTTOM CONTROLS ──────────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible,
            enter    = fadeIn() + slideInVertically { 80 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(
                isPaused = isPaused,
                onPause  = onPause,
                onStop   = onStop
            )
        }

        // ── CENTER FAB ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible,
            enter    = fadeIn(tween(600)) + scaleIn(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            GlassFab(icon = "⊙", onClick = onCenterMap)
        }
    }
}

// ─────────────────────────────────────────────
//  TOP STATS BOX  (timer + all 3 stats in one card)
// ─────────────────────────────────────────────
@Composable
private fun TopStatsBox(
    seconds: Int,
    isPaused: Boolean,
    distanceKm: Double,
    calories: Double,
    pace: String,
    workoutMode: String
) {
    val badgeColor by animateColorAsState(
        if (isPaused) Color(0xFFFFAA00) else NeonGreen,
        tween(400), label = "badge"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── MODE + LIVE badge row ─────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
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
                Text(modeIcon[workoutMode] ?: "🏃", fontSize = 14.sp)
                Text(
                    workoutMode, color = TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.5.sp
                )
            }

            // Live/Paused badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PulsingDot(badgeColor)
                Text(
                    if (isPaused) "PAUSED" else "LIVE",
                    color = badgeColor, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, letterSpacing = 2.sp
                )
            }
        }

        // ── MAIN CARD: timer + 3 stats ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
        ) {
            // Glow behind timer
            val glowAlpha by animateFloatAsState(
                if (!isPaused) 0.18f else 0.04f, tween(600), label = "ga"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(200.dp)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(NeonGreen.copy(glowAlpha), Color.Transparent)
                            ),
                            size.minDimension
                        )
                    }
            )

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timer label
                Text(
                    "DURATION",
                    color         = TextMuted,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                // Big timer
                val scale by animateFloatAsState(
                    if (!isPaused && seconds % 2 == 0) 1.012f else 1f,
                    tween(500), label = "tp"
                )
                Text(
                    formatTime(seconds),
                    color         = TextPrimary,
                    fontSize      = 56.sp,
                    fontWeight    = FontWeight.W200,
                    letterSpacing = 4.sp,
                    modifier      = Modifier.scale(scale)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(1.dp)
                        .background(GlassBorder)
                )

                // 3 stats in a row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InlineStatItem(
                        label = "DISTANCE",
                        value = String.format("%.2f", distanceKm),
                        unit  = "km"
                    )
                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(GlassBorder)
                    )
                    InlineStatItem(
                        label = "PACE",
                        value = pace,
                        unit  = "min/km"
                    )
                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(GlassBorder)
                    )
                    InlineStatItem(
                        label = "CALORIES",
                        value = String.format("%.0f", calories),
                        unit  = "kcal"
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  INLINE STAT ITEM  (inside the top card)
// ─────────────────────────────────────────────
@Composable
private fun InlineStatItem(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width(90.dp)
    ) {
        Text(
            label,
            color         = TextMuted,
            fontSize      = 8.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            textAlign     = TextAlign.Center
        )
        AnimatedContent(
            targetState  = value,
            transitionSpec = {
                (slideInVertically { -16 } + fadeIn()) togetherWith
                        (slideOutVertically { 16 } + fadeOut())
            },
            label = "sv"
        ) { v ->
            Text(
                v,
                color      = NeonGreen,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        }
        Text(
            unit,
            color     = TextMuted,
            fontSize  = 8.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
//  BOTTOM CONTROLS  (pause + stop only)
// ─────────────────────────────────────────────
@Composable
private fun BottomControls(
    isPaused: Boolean,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Stop button
        ControlButton(
            label    = "■",
            sublabel = "STOP",
            color    = ErrorRed,
            modifier = Modifier.weight(1f),
            onClick  = onStop
        )
        // Pause / Resume
        PauseResumeButton(
            isPaused = isPaused,
            onClick  = onPause,
            modifier = Modifier.weight(1.6f)
        )
    }
}

// ─────────────────────────────────────────────
//  PAUSE / RESUME BUTTON
// ─────────────────────────────────────────────
@Composable
private fun PauseResumeButton(isPaused: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale       by animateFloatAsState(if (isPaused) 1.05f else 1f,                         spring(Spring.DampingRatioMediumBouncy), label = "ps")
    val bgColor     by animateColorAsState(if (isPaused) NeonGreen else NeonGreenDim.copy(0.2f), tween(300), label = "pb")
    val textColor   by animateColorAsState(if (isPaused) DeepBlack else NeonGreen,               tween(300), label = "pt")
    val borderColor by animateColorAsState(if (isPaused) NeonGreen else NeonGreen.copy(0.4f),    tween(300), label = "pbd")

    Box(
        modifier = modifier
            .scale(scale).height(64.dp).clip(RoundedCornerShape(20.dp))
            .background(bgColor).border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(remember { MutableInteractionSource() }, ripple(color = NeonGreen)) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isPaused) "▶" else "⏸", color = textColor, fontSize = 22.sp)
            Text(
                if (isPaused) "RESUME" else "PAUSE",
                color = textColor, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
//  CONTROL BUTTON  (Stop)
// ─────────────────────────────────────────────
@Composable
private fun ControlButton(
    label: String, sublabel: String, color: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bs"
    )
    Box(
        modifier = modifier
            .scale(scale).height(64.dp).clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(remember { MutableInteractionSource() }, ripple(color = color)) {
                pressed = true; onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label,    color = color, fontSize = 20.sp)
            Text(sublabel, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

// ─────────────────────────────────────────────
//  GLASS FAB
// ─────────────────────────────────────────────
@Composable
private fun GlassFab(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp).clip(CircleShape).background(GlassWhite)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(remember { MutableInteractionSource() }, ripple()) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp, color = TextPrimary)
    }
}

// ─────────────────────────────────────────────
//  PULSING DOT
// ─────────────────────────────────────────────
@Composable
private fun PulsingDot(color: Color) {
    val inf   = rememberInfiniteTransition(label = "dot")
    val alpha by inf.animateFloat(
        1f, 0.2f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "da"
    )
    Box(Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}