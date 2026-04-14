package com.example.run

// ════════════════════════════════════════════════════════════════════════════
//  water_tracker.kt  —  CaloriX  •  Premium Water Tracker
//  Features:
//    • Goal selector (1–10 L) with smart suggestions
//    • Animated ANALOG CLOCK hand to pick reminder interval (10 min → 3 hr)
//    • Foreground + exact-alarm notifications even when app is closed
//    • Full-screen notification action → bottom-sheet container picker
//    • Liquid-fill animated progress bar (Apple Health inspired)
//    • Matches CaloriX dark/lavender palette exactly
// ════════════════════════════════════════════════════════════════════════════

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*



// ════════════════════════════════════════════════════════════════════════════
//  PALETTE  (mirrors HomeFragment exactly)
// ════════════════════════════════════════════════════════════════════════════
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
private val WaterBlue     = Color(0xFF4FC3F7)
private val WaterDeep     = Color(0xFF0277BD)

// ════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION CONSTANTS
// ════════════════════════════════════════════════════════════════════════════
const val CHANNEL_ID          = "water_reminder_channel"
const val NOTIF_ID            = 1001
const val ACTION_LOG_DRINK    = "com.example.run.ACTION_LOG_DRINK"
const val PREF_NAME           = "water_tracker_prefs"
const val PREF_GOAL           = "goal_ml"
const val PREF_INTAKE         = "intake_ml"
const val PREF_INTERVAL_MIN   = "interval_min"
const val EXTRA_CONTAINER_ML  = "container_ml"

// ════════════════════════════════════════════════════════════════════════════
//  DATA CLASSES  — POST these to MongoDB
// ════════════════════════════════════════════════════════════════════════════

/**
 * POST /api/water/settings
 * Stores user's daily goal and reminder preferences.
 */
data class WaterSettingsRequest(
    val userId          : String,           // from auth session
    val goalMl          : Int,              // e.g. 2000
    val reminderInterval: Int,              // minutes between reminders, e.g. 30
    val updatedAt       : Long = System.currentTimeMillis()
)

/**
 * POST /api/water/log
 * Each time the user drinks, POST one of these.
 */
data class WaterLogRequest(
    val userId         : String,
    val amountMl       : Int,               // e.g. 250
    val containerType  : String,            // "sip" | "glass" | "bottle" | "jug"
    val cumulativeMl   : Int,               // total so far today
    val goalMl         : Int,
    val loggedAt       : Long = System.currentTimeMillis(),
    val date           : String             // "YYYY-MM-DD" for daily grouping
)

/**
 * GET /api/water/today?userId=...
 * Expected response shape:
 */
data class WaterTodayResponse(
    val date       : String,
    val goalMl     : Int,
    val cumulativeMl: Int,
    val logs       : List<WaterLogResponse>
)

data class WaterLogResponse(
    val id          : String,
    val amountMl    : Int,
    val containerType: String,
    val loggedAt    : Long
)

// ════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION CHANNEL SETUP  (call once from Application or MainActivity)
// ════════════════════════════════════════════════════════════════════════════
fun createWaterNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Water Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description        = "Reminds you to drink water"
            enableLights(true)
            lightColor         = AColor.CYAN
            enableVibration(true)
            vibrationPattern   = longArrayOf(0, 200, 100, 200)
        }
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(channel)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  BROADCAST RECEIVER  — handles both alarm firing + quick-drink action
// ════════════════════════════════════════════════════════════════════════════
class WaterReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_LOG_DRINK -> {
                // Quick-add from notification (default 250 ml)
                val ml  = intent.getIntExtra(EXTRA_CONTAINER_ML, 250)
                val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val cur = pre.getInt(PREF_INTAKE, 0)
                pre.edit().putInt(PREF_INTAKE, cur + ml).apply()
                // Dismiss notification
                NotificationManagerCompat.from(context).cancel(NOTIF_ID)
                // Reschedule next alarm
                scheduleNextReminder(context)
            }
            else -> {
                // Alarm fired — show notification
                showWaterNotification(context)
                scheduleNextReminder(context)
            }
        }
    }
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun showWaterNotification(context: Context) {
    val prefs    = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val goalMl   = prefs.getInt(PREF_GOAL, 2000)
    val intakeMl = prefs.getInt(PREF_INTAKE, 0)
    val pct      = ((intakeMl * 100f) / goalMl).toInt().coerceIn(0, 100)

    // Full-screen intent → opens WaterDrinkActivity (half-screen style)
    val fullScreenIntent = Intent(context, WaterDrinkActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val fullScreenPi = PendingIntent.getActivity(
        context, 0, fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Quick-drink action (250 ml glass)
    val quickDrinkIntent = Intent(context, WaterReminderReceiver::class.java).apply {
        action = ACTION_LOG_DRINK
        putExtra(EXTRA_CONTAINER_ML, 250)
    }
    val quickDrinkPi = PendingIntent.getBroadcast(
        context, 1, quickDrinkIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_water)  // 👈 fixed
        .setContentTitle("💧 Time to Hydrate!")
        .setContentText("$intakeMl ml / $goalMl ml ($pct%) — Stay on track!")
        .setSubText("Water Tracker")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(fullScreenPi)
        .addAction(0, "Drink 250ml 💧", quickDrinkPi)
        .setAutoCancel(true)

        .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED) {
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }
}

fun scheduleNextReminder(context: Context) {
    val prefs       = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val intervalMin = prefs.getInt(PREF_INTERVAL_MIN, 60)
    val alarmMgr    = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi          = PendingIntent.getBroadcast(
        context, 2,
        Intent(context, WaterReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val triggerAt = System.currentTimeMillis() + intervalMin * 60_000L

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    } else {
        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }
}

fun cancelReminders(context: Context) {
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi       = PendingIntent.getBroadcast(
        context, 2,
        Intent(context, WaterReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmMgr.cancel(pi)
}

// ════════════════════════════════════════════════════════════════════════════
//  WATER DRINK ACTIVITY  — launched from notification (half-screen feel)
//  Declare in Manifest with: android:theme="@style/Theme.Translucent"
//  or use a Dialog theme for that half-screen look.
// ════════════════════════════════════════════════════════════════════════════
class WaterDrinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WaterDrinkBottomSheet(
                    onDrink = { ml ->
                        val pre = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        val cur = pre.getInt(PREF_INTAKE, 0)
                        pre.edit().putInt(PREF_INTAKE, cur + ml).apply()
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  MAIN WATER TRACKER ACTIVITY
// ════════════════════════════════════════════════════════════════════════════
class water_tracker : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createWaterNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                WaterTrackerScreen(
                    initialGoalMl      = prefs.getInt(PREF_GOAL, 2000),
                    initialIntakeMl    = prefs.getInt(PREF_INTAKE, 0),
                    initialIntervalMin = prefs.getInt(PREF_INTERVAL_MIN, 60),
                    onSaveSettings     = { goalMl, intervalMin ->
                        prefs.edit()
                            .putInt(PREF_GOAL, goalMl)
                            .putInt(PREF_INTERVAL_MIN, intervalMin)
                            .apply()
                        scheduleNextReminder(this)
                    },
                    onLogDrink         = { ml ->
                        val cur = prefs.getInt(PREF_INTAKE, 0)
                        prefs.edit().putInt(PREF_INTAKE, cur + ml).apply()
                    },
                    onBack             = { finish() }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN COMPOSABLE
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WaterTrackerScreen(
    initialGoalMl      : Int,
    initialIntakeMl    : Int,
    initialIntervalMin : Int,
    onSaveSettings     : (goalMl: Int, intervalMin: Int) -> Unit,
    onLogDrink         : (ml: Int) -> Unit,
    onBack             : () -> Unit
) {
    var currentTab      by remember { mutableIntStateOf(0) }   // 0=Today 1=Setup
    var goalMl          by remember { mutableIntStateOf(initialGoalMl) }
    var intakeMl        by remember { mutableIntStateOf(initialIntakeMl) }
    var intervalMin     by remember { mutableIntStateOf(initialIntervalMin) }
    var showDrinkSheet  by remember { mutableStateOf(false) }
    val scope           = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        // Background blobs — same as home
        Box(Modifier.size(300.dp).offset(140.dp, (-50).dp)
            .background(
                Brush.radialGradient(listOf(HoloPink.copy(0.18f), Color.Transparent), radius = 600f),
                CircleShape
            ))
        Box(Modifier.size(240.dp).offset((-50).dp, 550.dp)
            .background(
                Brush.radialGradient(listOf(AccentViolet.copy(0.14f), Color.Transparent), radius = 480f),
                CircleShape
            ))
        Box(Modifier.size(200.dp).offset(160.dp, 420.dp)
            .background(
                Brush.radialGradient(listOf(WaterBlue.copy(0.12f), Color.Transparent), radius = 400f),
                CircleShape
            ))

        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────
            WaterHeader(onBack = onBack)

            // ── Tab bar ───────────────────────────────────────────────────
            WaterTabBar(
                selected  = currentTab,
                onSelect  = { currentTab = it }
            )

            // ── Content ───────────────────────────────────────────────────
            AnimatedContent(
                targetState   = currentTab,
                transitionSpec = {
                    slideInHorizontally { if (targetState > initialState) it else -it } + fadeIn() togetherWith
                            slideOutHorizontally { if (targetState > initialState) -it else it } + fadeOut()
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> TodayTab(
                        intakeMl      = intakeMl,
                        goalMl        = goalMl,
                        intervalMin   = intervalMin,
                        onDrinkTap    = { showDrinkSheet = true }
                    )
                    1 -> SetupTab(
                        goalMl        = goalMl,
                        intervalMin   = intervalMin,
                        onGoalChange  = { goalMl = it },
                        onIntervalChange = { intervalMin = it },
                        onSave        = {
                            onSaveSettings(goalMl, intervalMin)
                            scope.launch {
                                delay(300)
                                currentTab = 0
                            }
                        }
                    )
                }
            }
        }

        // ── Drink bottom sheet ─────────────────────────────────────────────
        if (showDrinkSheet) {
            WaterDrinkBottomSheet(
                onDrink = { ml ->
                    intakeMl += ml
                    onLogDrink(ml)
                    showDrinkSheet = false
                },
                onDismiss = { showDrinkSheet = false }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WaterHeader(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826))))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(PureWhite.copy(0.08f), CircleShape)
                    .border(1.dp, SubtleGrey.copy(0.3f), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 16.sp, color = PureWhite, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "💧 Water Tracker",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = PureWhite, letterSpacing = (-0.5).sp
                )
                Text(
                    "Stay perfectly hydrated",
                    fontSize = 11.sp, color = TextSecondary, letterSpacing = 0.3.sp
                )
            }
            Box(Modifier.size(40.dp)) // spacer for symmetry
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TAB BAR
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WaterTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Today", "Setup")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .background(BgLavender, RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val isSelected = selected == i
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(listOf(AccentViolet, HoloPink))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PureWhite else TextSecondary
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TODAY TAB  —  animated liquid fill + stats
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun TodayTab(
    intakeMl    : Int,
    goalMl      : Int,
    intervalMin : Int,
    onDrinkTap  : () -> Unit
) {
    val pct         = (intakeMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    val animPct     by animateFloatAsState(
        targetValue   = pct,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label         = "water_fill"
    )
    val wave        = rememberInfiniteTransition(label = "wave")
    val waveOffset  by wave.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label         = "wave_offset"
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Big liquid circle ──────────────────────────────────────────────
        Box(
            Modifier
                .size(220.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(listOf(AccentViolet, WaterBlue, HoloPink, AccentViolet)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawWaterFill(animPct, waveOffset)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(animPct * 100).toInt()}%",
                    fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    color = if (animPct > 0.4f) PureWhite else WaterDeep,
                    letterSpacing = (-2).sp
                )
                Text(
                    "$intakeMl ml",
                    fontSize = 14.sp,
                    color = if (animPct > 0.4f) PureWhite.copy(0.85f) else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Stats row ─────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Goal", "${goalMl / 1000f} L",        WaterBlue,    Modifier.weight(1f))
            StatCard("Done", "$intakeMl ml",               HoloMint,     Modifier.weight(1f))
            StatCard("Left", "${(goalMl - intakeMl).coerceAtLeast(0)} ml", HoloPink, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // ── Reminder badge ────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgLavender)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val pulse = rememberInfiniteTransition(label = "reminder_pulse")
            val sc by pulse.animateFloat(
                0.8f, 1.2f,
                infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "rp_sc"
            )
            Box(
                Modifier.size((8 * sc).dp)
                    .background(HoloMint, CircleShape)
            )
            Text(
                "Next reminder in $intervalMin min",
                fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text("🔔", fontSize = 16.sp)
        }

        Spacer(Modifier.height(28.dp))

        // ── Drink button ──────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(WaterDeep, WaterBlue, HoloMint)))
                .clickable { onDrinkTap() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💧", fontSize = 22.sp)
                Text(
                    "Log a Drink",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = PureWhite, letterSpacing = 0.3.sp
                )
            }
        }

        // ── Completion message ────────────────────────────────────────────
        if (pct >= 1f) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(
                            HoloMint.copy(0.25f), WaterBlue.copy(0.15f)
                        ))
                    )
                    .border(1.dp, HoloMint.copy(0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🎉 Goal crushed! You're perfectly hydrated today.",
                    fontSize = 13.sp, color = TextPrimary, textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

private fun DrawScope.drawWaterFill(pct: Float, waveOffsetDeg: Float) {
    val w = size.width
    val h = size.height
    val fillY = h * (1f - pct)

    val path = Path()
    path.moveTo(0f, fillY)

    val waveAmp    = h * 0.04f
    val waveOffset = (waveOffsetDeg / 360f) * w

    var x = 0f
    while (x <= w) {
        val angle = ((x + waveOffset) / w) * 2 * PI.toFloat()
        val y     = fillY + sin(angle) * waveAmp
        path.lineTo(x, y)
        x += 2f
    }
    path.lineTo(w, h)
    path.lineTo(0f, h)
    path.close()

    drawPath(
        path,
        Brush.verticalGradient(
            listOf(WaterBlue.copy(0.85f), WaterDeep.copy(0.95f)),
            startY = fillY, endY = h
        )
    )
    // second wave (offset)
    val path2 = Path()
    val shift = w * 0.5f
    path2.moveTo(0f, fillY)
    x = 0f
    while (x <= w) {
        val angle = ((x + waveOffset + shift) / w) * 2 * PI.toFloat()
        val y     = fillY + sin(angle) * (waveAmp * 0.6f)
        path2.lineTo(x, y)
        x += 2f
    }
    path2.lineTo(w, h)
    path2.lineTo(0f, h)
    path2.close()
    drawPath(path2, WaterBlue.copy(alpha = 0.35f))
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BgLavender)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(6.dp).background(accent, CircleShape))
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SETUP TAB  —  Goal selector + Animated clock interval picker
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SetupTab(
    goalMl           : Int,
    intervalMin      : Int,
    onGoalChange     : (Int) -> Unit,
    onIntervalChange : (Int) -> Unit,
    onSave           : () -> Unit
) {
    // Goal presets in ml
    val goalOptions = listOf(
        500  to "½ L",  1000 to "1 L",  1500 to "1.5 L",
        2000 to "2 L",  2500 to "2.5 L", 3000 to "3 L",
        3500 to "3.5 L", 4000 to "4 L",  5000 to "5 L",
        6000 to "6 L",  8000 to "8 L",  10000 to "10 L"
    )

    // Goal suggestions
    val suggestions = listOf(
        "🏃 Active athlete"  to 3500,
        "💼 Office worker"   to 2000,
        "🧘 Wellness focus"  to 2500,
        "🌞 Hot climate"     to 3000,
        "👶 Light activity"  to 1500
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Goal header ───────────────────────────────────────────────────
        SectionHeader("Daily Goal", "How much to drink each day")

        Spacer(Modifier.height(12.dp))

        // ── Suggestions ───────────────────────────────────────────────────
        Text("Suggestions", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(suggestions) { _, (label, ml) ->
                val active = goalMl == ml
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (active) Brush.horizontalGradient(listOf(AccentViolet, HoloPink))
                            else Brush.horizontalGradient(listOf(BgLavender, BgLavender))
                        )
                        .border(
                            1.dp,
                            if (active) AccentViolet else SubtleGrey,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onGoalChange(ml) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "$label\n${ml / 1000f} L",
                        fontSize = 11.sp,
                        color = if (active) PureWhite else TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Goal grid ─────────────────────────────────────────────────────
        Text("Custom Amount", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // 4-column grid
        val rows = goalOptions.chunked(4)
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (ml, label) ->
                    val active = goalMl == ml
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (active)
                                    Brush.verticalGradient(listOf(AccentViolet, HoloPink.copy(0.8f)))
                                else
                                    Brush.verticalGradient(listOf(BgLavender, BgLavender))
                            )
                            .border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) AccentViolet else SubtleGrey,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onGoalChange(ml) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) PureWhite else TextPrimary
                        )
                    }
                }
                // Fill remaining cells if row < 4
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        // ── Clock interval picker ─────────────────────────────────────────
        SectionHeader("Reminder Interval", "Drag clock hand to set time")
        Spacer(Modifier.height(16.dp))

        AnimatedClockPicker(
            intervalMin      = intervalMin,
            onIntervalChange = onIntervalChange
        )

        Spacer(Modifier.height(28.dp))

        // ── Save button ───────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)))
                .clickable { onSave() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Save & Activate Reminders",
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = PureWhite, letterSpacing = 0.3.sp
            )
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun SectionHeader(title: String, sub: String) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(40.dp).height(3.dp)
            .background(Brush.horizontalGradient(listOf(AccentViolet, HoloPink)), RoundedCornerShape(2.dp)))
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ANIMATED CLOCK PICKER
//  The clock face shows intervals from 10 min to 180 min (3 hr).
//  The full circle = 180-minute range.
//  Dragging the hand around the face selects the interval.
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun AnimatedClockPicker(
    intervalMin     : Int,
    onIntervalChange: (Int) -> Unit
) {
    // Clamp range: 10..180 min
    // Map: 0° (12 o'clock) = 10 min, 360° = 180 min
    // angle = ((min - 10) / 170f) * 360f
    val minValue = 10
    val maxValue = 180

    fun minToAngle(min: Int) = ((min - minValue).toFloat() / (maxValue - minValue)) * 360f
    fun angleToMin(angle: Float): Int {
        val norm = ((angle % 360 + 360) % 360)
        return (minValue + (norm / 360f) * (maxValue - minValue)).roundToInt().coerceIn(minValue, maxValue)
    }

    var handAngle by remember { mutableFloatStateOf(minToAngle(intervalMin)) }
    val animAngle by animateFloatAsState(
        targetValue   = handAngle,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label         = "clock_hand"
    )
    val haptic = LocalHapticFeedback.current
    var lastHapticMin by remember { mutableIntStateOf(intervalMin) }

    // Tick marks for every 30 min: 10, 30, 60, 90, 120, 150, 180
    val tickLabels = listOf(10 to "10m", 30 to "30m", 60 to "1h", 90 to "90m", 120 to "2h", 150 to "2.5h", 180 to "3h")

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current value display
        val currentMin = angleToMin(handAngle)
        val displayText = when {
            currentMin < 60               -> "$currentMin min"
            currentMin % 60 == 0          -> "${currentMin / 60} hr"
            else                          -> "${currentMin / 60} hr ${currentMin % 60} min"
        }

        Text(
            displayText,
            fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = TextPrimary, letterSpacing = (-1).sp
        )
        Text(
            "between reminders",
            fontSize = 12.sp, color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        // ── Clock face with draggable hand ────────────────────────────────
        Box(
            Modifier
                .size(240.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pos    = change.position
                        val dx     = pos.x - center.x
                        val dy     = pos.y - center.y
                        // atan2: 0 = right, positive = clockwise
                        // We want 0° = 12 o'clock = top = -90°
                        var angle  = atan2(dy, dx) * (180f / PI.toFloat()) + 90f
                        if (angle < 0) angle += 360f
                        handAngle = angle
                        val newMin = angleToMin(angle)
                        if (newMin != lastHapticMin) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticMin = newMin
                        }
                        onIntervalChange(newMin)
                    }
                },
            contentAlignment = Alignment.Center
        ) {

            val minToAngle: (Int) -> Float = { minute ->
                minute * 6f   // 360° / 60 = 6°
            }
            Canvas(Modifier.fillMaxSize()) {
                drawClockFace(animAngle, minToAngle, tickLabels)
            }

            // Center dot label (drawn on top)
            Box(
                Modifier
                    .size(52.dp)
                    .background(
                        Brush.radialGradient(listOf(AccentViolet.copy(0.2f), Color.Transparent)),
                        CircleShape
                    )
                    .border(2.dp, AccentViolet.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⏱", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Interval quick-picks
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15 to "15m", 30 to "30m", 60 to "1h", 90 to "90m", 120 to "2h").forEach { (min, label) ->
                val active = currentMin == min
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active) Brush.horizontalGradient(listOf(AccentViolet, HoloPink))
                            else Brush.horizontalGradient(listOf(BgLavender, BgLavender))
                        )
                        .clickable {
                            handAngle = minToAngle(min)
                            onIntervalChange(min)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) PureWhite else TextSecondary
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawClockFace(
    handAngleDeg : Float,
    minToAngle   : (Int) -> Float,
    tickLabels   : List<Pair<Int, String>>
) {
    val cx     = size.width / 2f
    val cy     = size.height / 2f
    val radius = size.width / 2f - 8.dp.toPx()

    // Outer ring
    drawCircle(
        brush  = Brush.sweepGradient(listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.3f), HoloMint.copy(0.2f), AccentViolet.copy(0.4f))),
        radius = radius,
        style  = Stroke(width = 3.dp.toPx())
    )

    // Filled arc from 0° to handAngle (progress fill)
    drawArc(
        brush       = Brush.sweepGradient(
            0f    to AccentViolet.copy(0.7f),
            (handAngleDeg / 360f) to HoloPink.copy(0.7f),
            1f    to AccentViolet.copy(0f)
        ),
        startAngle  = -90f,
        sweepAngle  = handAngleDeg,
        useCenter   = false,
        topLeft     = Offset(cx - radius, cy - radius),
        size        = Size(radius * 2, radius * 2),
        style       = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Tick marks at each label position
    tickLabels.forEach { (min, _) ->
        val tickAngleRad = ((minToAngle(min) - 90f) * (PI / 180f)).toFloat()
        val outerR = radius - 4.dp.toPx()
        val innerR = radius - 14.dp.toPx()
        drawLine(
            color       = SubtleGrey.copy(0.6f),
            start       = Offset(cx + cos(tickAngleRad) * innerR, cy + sin(tickAngleRad) * innerR),
            end         = Offset(cx + cos(tickAngleRad) * outerR, cy + sin(tickAngleRad) * outerR),
            strokeWidth = 2.dp.toPx(),
            cap         = StrokeCap.Round
        )
    }

    // Hour dots every 30 degrees
    for (deg in 0..330 step 30) {
        val rad = ((deg - 90f) * (PI / 180f)).toFloat()
        val dotR = radius - 20.dp.toPx()
        drawCircle(
            color  = SubtleGrey.copy(0.35f),
            radius = 2.dp.toPx(),
            center = Offset(cx + cos(rad) * dotR, cy + sin(rad) * dotR)
        )
    }

    // Clock hand
    val handRad  = ((handAngleDeg - 90f) * (PI / 180f)).toFloat()
    val handLen  = radius - 28.dp.toPx()
    val handEnd  = Offset(cx + cos(handRad) * handLen, cy + sin(handRad) * handLen)

    // Hand shadow
    drawLine(
        color       = AccentViolet.copy(0.2f),
        start       = Offset(cx + 2, cy + 2),
        end         = Offset(handEnd.x + 2, handEnd.y + 2),
        strokeWidth = 5.dp.toPx(),
        cap         = StrokeCap.Round
    )
    // Hand
    drawLine(
        brush       = Brush.linearGradient(
            listOf(AccentViolet, HoloPink),
            start = Offset(cx, cy),
            end   = handEnd
        ),
        start       = Offset(cx, cy),
        end         = handEnd,
        strokeWidth = 4.dp.toPx(),
        cap         = StrokeCap.Round
    )
    // Hand tip glow dot
    drawCircle(
        brush  = Brush.radialGradient(
            listOf(HoloPink, AccentViolet.copy(0.4f)),
            center = handEnd,
            radius = 8.dp.toPx()
        ),
        radius = 8.dp.toPx(),
        center = handEnd
    )
    // Center dot
    drawCircle(
        brush  = Brush.radialGradient(listOf(AccentViolet, HoloPink.copy(0.6f))),
        radius = 6.dp.toPx(),
        center = Offset(cx, cy)
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  DRINK BOTTOM SHEET  — container type picker
//  Used both from inside the app AND from WaterDrinkActivity (notification)
// ════════════════════════════════════════════════════════════════════════════
data class ContainerType(
    val emoji : String,
    val label : String,
    val ml    : Int,
    val key   : String
)

private val containers = listOf(
    ContainerType("💧", "Sip",        50,  "sip"),
    ContainerType("🫗", "Small Cup",  150, "small_cup"),
    ContainerType("🥛", "Glass",      250, "glass"),
    ContainerType("🍶", "Large Glass",400, "large_glass"),
    ContainerType("🫙", "Bottle",     500, "bottle"),
    ContainerType("🏺", "Large Bottle",750,"large_bottle"),
    ContainerType("🪣", "Big Jug",   1000, "jug")
)

@Composable
fun WaterDrinkBottomSheet(
    onDrink   : (Int) -> Unit,
    onDismiss : () -> Unit
) {
    var selected by remember { mutableIntStateOf(2) } // default: Glass 250ml
    var visible  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(50); visible = true }

    val slideIn by animateFloatAsState(
        targetValue   = if (visible) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f),
        label         = "sheet_slide"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(DeepBlack.copy(0.6f))
            .clickable { onDismiss() }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = slideIn * 600f }
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(BgWhite)
                .clickable { /* absorb */ }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // Drag handle
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(SubtleGrey)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    "What did you drink?",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, letterSpacing = (-0.5).sp
                )
                Text(
                    "Select a container",
                    fontSize = 12.sp, color = TextSecondary
                )

                Spacer(Modifier.height(20.dp))

                // Container grid (scroll horizontal)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(containers) { i, c ->
                        val isSelected = selected == i
                        val scale by animateFloatAsState(
                            targetValue   = if (isSelected) 1.1f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f),
                            label         = "container_scale_$i"
                        )
                        Column(
                            Modifier
                                .width(78.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected)
                                        Brush.verticalGradient(listOf(AccentViolet.copy(0.15f), HoloPink.copy(0.1f)))
                                    else
                                        Brush.verticalGradient(listOf(BgLavender, BgLavender))
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AccentViolet else SubtleGrey,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selected = i }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(c.emoji, fontSize = 26.sp)
                            Text(c.label, fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Text("${c.ml} ml", fontSize = 9.sp, color = AccentViolet, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Drink button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(WaterDeep, WaterBlue)))
                        .clickable { onDrink(containers[selected].ml) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💧", fontSize = 20.sp)
                        Text(
                            "Drink  +${containers[selected].ml} ml",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = PureWhite, letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}