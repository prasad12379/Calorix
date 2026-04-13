package com.example.run

import ActivityRequest
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Palette aliases (use same as rest of app) ─────────────────────────────────
private val WSDeepBlack    = Color(0xFF0A0A0A)
private val WSPureWhite    = Color(0xFFFFFFFF)
private val WSBgWhite      = Color(0xFFFAF9FF)
private val WSAccentViolet = Color(0xFF9B8FD4)
private val WSHoloPink     = Color(0xFFE8B4D8)
private val WSHoloMint     = Color(0xFFAEE8D8)
private val WSSubtleGrey   = Color(0xFFDDD8EE)
private val WSTextPrimary  = Color(0xFF0A0A0A)
private val WSTextSecondary= Color(0xFF7A7490)
private val WSBgLavender   = Color(0xFFECE8F5)

class workout_summary : AppCompatActivity() {

    private lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiInterface = retrofit.create(ApiInterface::class.java)

        // Pull intent extras
        val workoutMode  = intent.getStringExtra("WORKOUT_MODE") ?: "RUNNING"
        val duration     = intent.getStringExtra("DURATION")     ?: "00:00:00"
        val distance     = intent.getStringExtra("DISTANCE")     ?: "0.00"
        val calories     = intent.getStringExtra("CALORIES")     ?: "0"
        val pace         = intent.getStringExtra("PACE")         ?: "0:00"
        val mapImagePath = intent.getStringExtra("MAP_IMAGE_PATH")

        setContent {
            MaterialTheme {
                WorkoutSummaryScreen(
                    workoutMode  = workoutMode,
                    duration     = duration,
                    distance     = distance,
                    calories     = calories,
                    pace         = pace,
                    mapImagePath = mapImagePath,
                    onSave       = { steps, bestPace, dateStr ->
                        saveWorkout(workoutMode, duration, distance, calories, pace, steps, bestPace, dateStr)
                    },
                    onDiscard    = { finish() },
                    onShare      = { shareWorkout(workoutMode, duration, distance, calories, pace) }
                )
            }
        }
    }

    private fun saveWorkout(
        mode: String, dur: String, dist: String, cal: String, pace: String,
        steps: Int, bestPace: String, dateStr: String
    ) {
        val email = getSharedPreferences("USER_SESSION", MODE_PRIVATE).getString("email", null)
        if (email == null) { android.widget.Toast.makeText(this, "Not logged in", android.widget.Toast.LENGTH_SHORT).show(); return }

        val req = ActivityRequest(email, mode, dur, dist, cal, pace, steps, bestPace, dateStr)
        apiInterface.saveActivity(req).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                android.widget.Toast.makeText(this@workout_summary,
                    if (response.isSuccessful) response.body()?.message ?: "Saved!" else "Failed to save",
                    android.widget.Toast.LENGTH_SHORT).show()
                if (response.isSuccessful) finish()
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                android.widget.Toast.makeText(this@workout_summary, "Network error: ${t.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun shareWorkout(mode: String, dur: String, dist: String, cal: String, pace: String) {
        val text = "CaloriX Workout\nType: $mode\nTime: $dur\nDistance: $dist km\nCalories: $cal kcal\nPace: $pace min/km"
        startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share Workout"))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WORKOUT SUMMARY SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WorkoutSummaryScreen(
    workoutMode:  String,
    duration:     String,
    distance:     String,
    calories:     String,
    pace:         String,
    mapImagePath: String?,
    onSave:       (steps: Int, bestPace: String, dateStr: String) -> Unit,
    onDiscard:    () -> Unit,
    onShare:      () -> Unit
) {
    // Computed values
    val dateStr   = remember { SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date()) }
    val steps     = remember { (distance.toDoubleOrNull() ?: 0.0).times(1300).toInt() }
    val bestPace  = remember {
        try {
            val parts = pace.split(":")
            if (parts.size == 2) {
                val secs = (parts[0].toInt() * 60 + parts[1].toInt()) * 0.9
                "%d:%02d".format((secs / 60).toInt(), (secs % 60).toInt())
            } else "N/A"
        } catch (e: Exception) { "N/A" }
    }
    val workoutTitle = when (workoutMode) {
        "RUNNING" -> "Morning Run"
        "WALKING" -> "Walking Session"
        "CYCLING" -> "Cycling Ride"
        else      -> "Workout"
    }

    // Mode accent colours
    val modeGradient = when (workoutMode) {
        "RUNNING" -> listOf(Color(0xFF2A1F4A), WSAccentViolet)
        "WALKING" -> listOf(Color(0xFF0F2A1A), Color(0xFF2E7D52))
        "CYCLING" -> listOf(Color(0xFF2A1A0F), Color(0xFF8B5E28))
        else      -> listOf(WSDeepBlack, WSAccentViolet)
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest  = { showDiscardDialog = false },
            title             = { Text("Discard Workout?", color = WSTextPrimary) },
            text              = { Text("Are you sure you want to discard this workout?", color = WSTextSecondary) },
            confirmButton     = {
                TextButton(onClick = onDiscard) { Text("Discard", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold) }
            },
            dismissButton     = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep", color = WSAccentViolet, fontWeight = FontWeight.Bold) }
            },
            containerColor    = WSPureWhite,
            shape             = RoundedCornerShape(20.dp)
        )
    }

    Box(Modifier.fillMaxSize().background(WSBgWhite)) {

        // Holo blobs
        Box(Modifier.size(260.dp).offset(x = 140.dp, y = (-50).dp).blur(80.dp)
            .background(Brush.radialGradient(listOf(WSHoloPink.copy(0.4f), WSHoloMint.copy(0.2f), Color.Transparent)), CircleShape))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── DARK HERO HEADER ──────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(WSDeepBlack, Color(0xFF1C1826))))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Shimmer blob
                Box(Modifier.size(140.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp).blur(50.dp)
                    .background(Brush.radialGradient(listOf(WSAccentViolet.copy(0.4f), Color.Transparent)), CircleShape))

                Column {
                    // Mode tag pill
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(modeGradient))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(workoutMode, color = WSPureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(workoutTitle, color = WSPureWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(4.dp))
                    Text(dateStr, color = WSTextSecondary, fontSize = 12.sp)

                    Spacer(Modifier.height(24.dp))

                    // ── Big 3 stats ───────────────────────────────────────────
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HeroBigStat("Distance", "$distance km")
                        // Divider
                        Box(Modifier.width(1.dp).height(48.dp).background(WSPureWhite.copy(0.12f)))
                        HeroBigStat("Duration", duration)
                        Box(Modifier.width(1.dp).height(48.dp).background(WSPureWhite.copy(0.12f)))
                        HeroBigStat("Calories", "$calories kcal")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── MAP SNAPSHOT ──────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Route Map", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WSTextPrimary)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(modeGradient))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("$distance km", color = WSPureWhite, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = WSAccentViolet.copy(0.2f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(WSDeepBlack)
                ) {
                    val bitmap = remember(mapImagePath) {
                        mapImagePath?.let { path ->
                            val f = File(path)
                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                        }
                    }
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = "Route", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF0D0B14)))), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No route map", color = WSTextSecondary, fontSize = 12.sp)
                                Text("available", color = WSTextSecondary.copy(0.6f), fontSize = 11.sp)
                            }
                        }
                    }
                    // Overlay badge
                    Box(
                        Modifier.align(Alignment.BottomStart).padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WSDeepBlack.copy(0.75f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("$distance km route", color = WSPureWhite, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── DETAILED STATS GRID ───────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Stats", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WSTextPrimary)
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "Avg Pace",   "$pace /km",   modeGradient)
                    StatCard(Modifier.weight(1f), "Best Pace",  "$bestPace /km", modeGradient)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "Steps",  "%,d".format(steps), modeGradient)
                    StatCard(Modifier.weight(1f), "Workout", workoutMode.lowercase().replaceFirstChar { it.uppercase() }, modeGradient)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── ACTION BUTTONS ────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Save — primary
                val inf = rememberInfiniteTransition(label = "saveGlow")
                val glow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "g")
                Box(
                    Modifier.fillMaxWidth().height(54.dp)
                        .shadow((14 * glow).dp, RoundedCornerShape(18.dp), ambientColor = WSAccentViolet, spotColor = WSHoloPink)
                        .background(Brush.horizontalGradient(listOf(WSDeepBlack, Color(0xFF2A1F4A))), RoundedCornerShape(18.dp))
                        .border(1.dp, Brush.horizontalGradient(listOf(WSAccentViolet.copy(0.6f), WSHoloPink.copy(0.4f))), RoundedCornerShape(18.dp))
                        .clickable { onSave(steps, bestPace, dateStr) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Workout", color = WSPureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }

                // Share + Discard row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Share
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(WSBgLavender)
                            .border(1.dp, WSSubtleGrey, RoundedCornerShape(14.dp))
                            .clickable { onShare() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Share", color = WSAccentViolet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // Discard
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFEBEB))
                            .border(1.dp, Color(0xFFFFB3B3), RoundedCornerShape(14.dp))
                            .clickable { showDiscardDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Discard", color = Color(0xFFCC3333), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Hero big stat (inside header) ─────────────────────────────────────────────
@Composable
private fun HeroBigStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = WSPureWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = WSTextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp)
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, gradient: List<Color>) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        color           = WSPureWhite,
        border          = BorderStroke(1.dp, WSSubtleGrey),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Accent dot
            Box(Modifier.size(6.dp).background(Brush.linearGradient(gradient), CircleShape))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WSTextPrimary)
            Text(label, fontSize = 10.sp, color = WSTextSecondary, letterSpacing = 0.3.sp)
        }
    }
}