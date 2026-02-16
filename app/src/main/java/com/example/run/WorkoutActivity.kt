package com.example.run

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class WorkoutActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // UI Elements
    private lateinit var timerView: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvPace: TextView
    private lateinit var tvWorkoutMode: TextView
    private lateinit var btnPause: CardView
    private lateinit var btnStop: CardView
    private lateinit var btnCenterMap: CardView
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    // Timer
    private var seconds = 0
    private var isRunning = true
    private var isPaused = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    // Location Tracking
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var totalDistanceMeters = 0.0
    private var lastLocation: Location? = null
    private val pathPoints = mutableListOf<GeoPoint>()
    private lateinit var pathLine: Polyline

    // Workout Mode
    private var workoutMode = "RUNNING"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )

        setContentView(R.layout.activity_workout)

        initViews()
        initMap()
        setupListeners()

        if (checkLocationPermission()) {
            startWorkout()
        } else {
            requestLocationPermission()
        }
    }

    private fun initViews() {
        timerView = findViewById(R.id.tvTimer)
        tvDistance = findViewById(R.id.tvDistance2)
        tvCalories = findViewById(R.id.tvCalories2)
        tvPace = findViewById(R.id.tvPace2)
        tvWorkoutMode = findViewById(R.id.tvWorkoutMode)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnCenterMap = findViewById(R.id.btnCenterMap2)
        mapView = findViewById(R.id.map2)

        workoutMode = intent.getStringExtra("MODE") ?: "RUNNING"
        tvWorkoutMode.text = workoutMode

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)
        mapView.controller.setZoom(18.0)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        mapView.overlays.add(myLocationOverlay)

        pathLine = Polyline().apply {
            outlinePaint.color = Color.parseColor("#4CAF50")
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        }
        mapView.overlays.add(pathLine)

        myLocationOverlay.runOnFirstFix {
            runOnUiThread {
                myLocationOverlay.myLocation?.let {
                    mapView.controller.setCenter(it)
                }
            }
        }
    }

    private fun setupListeners() {
        btnPause.setOnClickListener {
            togglePause()
        }

        btnStop.setOnClickListener {
            stopWorkout()
        }

        btnCenterMap.setOnClickListener {
            myLocationOverlay.myLocation?.let {
                mapView.controller.animateTo(it)
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

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
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWorkout()
            } else {
                Toast.makeText(
                    this,
                    "Location permission required for tracking",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun startWorkout() {
        startTimer()
        initializeFirstLocation() // 🔥 FIX: Get initial location first
        startLocationTracking()
    }

    // 🔥 NEW: Get initial location to start tracking
    private fun initializeFirstLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    pathPoints.add(geoPoint)
                    pathLine.setPoints(pathPoints)
                    mapView.invalidate()

                    Toast.makeText(this, "Tracking started!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (!isPaused) {
                    val hrs = seconds / 3600
                    val mins = (seconds % 3600) / 60
                    val secs = seconds % 60

                    timerView.text = String.format("%02d:%02d:%02d", hrs, mins, secs)
                    seconds++
                }
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun startLocationTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 🔥 Changed to 5 seconds for better outdoor tracking
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f // 🔥 Only update if moved 5 meters
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isPaused) return

                locationResult.lastLocation?.let { location ->
                    // 🔥 Log for debugging
                    android.util.Log.d("WorkoutActivity",
                        "New location: ${location.latitude}, ${location.longitude}, Accuracy: ${location.accuracy}")

                    updateDistance(location)
                    updatePath(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun updateDistance(newLocation: Location) {
        lastLocation?.let { oldLoc ->
            val distance = oldLoc.distanceTo(newLocation)

            // 🔥 More lenient filtering for outdoor walking
            android.util.Log.d("WorkoutActivity", "Distance: $distance meters")

            if (distance > 2 && distance < 200) { // Min 2m, Max 200m
                totalDistanceMeters += distance
                android.util.Log.d("WorkoutActivity", "Total Distance: $totalDistanceMeters meters")
            } else {
                android.util.Log.d("WorkoutActivity", "Distance filtered: $distance meters")
            }
        }

        lastLocation = newLocation
        updateStatsUI()
    }

    private fun updatePath(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        pathPoints.add(geoPoint)
        pathLine.setPoints(pathPoints)
        mapView.invalidate()
    }

    private fun updateStatsUI() {
        val distanceKm = totalDistanceMeters / 1000.0

        // 🔥 Always update UI even if distance is 0
        tvDistance.text = String.format("%.2f", distanceKm)

        // Calculate pace (min/km)
        if (distanceKm > 0.01 && seconds > 0) { // Only calculate if moved at least 10m
            val totalMinutes = seconds / 60.0
            val avgPace = totalMinutes / distanceKm

            if (avgPace.isFinite() && avgPace > 0 && avgPace < 60) { // Reasonable pace range
                val paceMin = avgPace.toInt()
                val paceSec = ((avgPace - paceMin) * 60).toInt()
                tvPace.text = String.format("%d:%02d", paceMin, paceSec)
            } else {
                tvPace.text = "0:00"
            }
        } else {
            tvPace.text = "0:00"
        }

        // Calculate calories
        val caloriesPerKm = when (workoutMode) {
            "RUNNING" -> 60
            "WALKING" -> 40
            "CYCLING" -> 30
            else -> 50
        }
        val kcal = distanceKm * caloriesPerKm
        tvCalories.text = String.format("%.0f", kcal)
    }

    private fun togglePause() {
        isPaused = !isPaused

        Toast.makeText(
            this,
            if (isPaused) "Workout Paused" else "Workout Resumed",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopWorkout() {
        // Pass data to summary activity
        val intent = Intent(this, WorkoutSummaryActivity::class.java).apply {
            putExtra("WORKOUT_MODE", workoutMode)
            putExtra("DURATION", timerView.text.toString())
            putExtra("DISTANCE", tvDistance.text.toString())
            putExtra("CALORIES", tvCalories.text.toString())
            putExtra("PACE", tvPace.text.toString())
        }

        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)

        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        mapView.onDetach()
    }
}