package com.example.run

// ════════════════════════════════════════════════════════════════════════════
//  water_tracker.kt — CaloriX Premium Water Tracker
//
//  Changes from original:
//  • Full CaloriX dark/lavender/violet palette throughout
//  • PNG images for container selection (glass, bottle, mug, custom)
//  • Animated horizontal container carousel (replaces grid)
//  • "Stop Reminders" button in Today tab
//  • Goal completion auto-stops reminders + shows celebration card
//  • API calls: GET /water/today, POST /water/settings, POST /water/log
//  • Notification bottom sheet uses PNG container images
//  • Performance: no blur(), static blobs, single InfiniteTransition per wave
// ════════════════════════════════════════════════════════════════════════════

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.*

// ════════════════════════════════════════════════════════════════════════════
//  PALETTE — exact CaloriX palette
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

// Static brushes — allocated once
private val HeaderGrad    = Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826)))
private val AccentBrush   = Brush.horizontalGradient(listOf(AccentViolet, HoloPink))
private val WaterBrush    = Brush.horizontalGradient(listOf(WaterDeep, WaterBlue, HoloMint))
private val BlobPinkBrush = Brush.radialGradient(listOf(HoloPink.copy(0.18f), Color.Transparent), radius = 600f)
private val BlobBlueBrush = Brush.radialGradient(listOf(WaterBlue.copy(0.12f), Color.Transparent), radius = 480f)
private val BlobViolBrush = Brush.radialGradient(listOf(AccentViolet.copy(0.14f), Color.Transparent), radius = 440f)

// ════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION CONSTANTS
// ════════════════════════════════════════════════════════════════════════════
const val CHANNEL_ID        = "water_reminder_channel"
const val NOTIF_ID          = 1001
const val ACTION_LOG_DRINK  = "com.example.run.ACTION_LOG_DRINK"
const val PREF_NAME         = "water_tracker_prefs"
const val PREF_GOAL         = "goal_ml"
const val PREF_INTAKE       = "intake_ml"
const val PREF_INTERVAL_MIN = "interval_min"
const val PREF_REMINDERS_ON = "reminders_on"
const val PREF_DATE         = "last_date"
const val EXTRA_CONTAINER_ML= "container_ml"

// ════════════════════════════════════════════════════════════════════════════
//  API MODELS
// ════════════════════════════════════════════════════════════════════════════

// POST /water/settings — Request
data class WaterSettingsRequest(
    val email:                 String,
    val goal_ml:               Int,
    val reminder_interval_min: Int,
    val reminder_active:       Boolean,
    val updated_at:            Long = System.currentTimeMillis()
)

// POST /water/log — Request
// Triggered when: user taps Drink button OR notification quick-action
data class WaterLogRequest(
    val email:          String,
    val amount_ml:      Int,
    val container_type: String,   // "sip"|"glass"|"mug"|"bottle"|"custom"
    val cumulative_ml:  Int,      // running total AFTER this drink
    val goal_ml:        Int,
    val logged_at:      Long = System.currentTimeMillis(),
    val date:           String    // "YYYY-MM-DD"
)

// POST /water/log — Response
data class WaterLogResponse(
    val success:      Boolean,
    val message:      String,
    val data:         WaterLogData?
)
data class WaterLogData(
    val id:            String,
    val cumulative_ml: Int,
    val goal_ml:       Int,
    val goal_reached:  Boolean   // true → auto-stop reminders
)

// GET /water/today — Response
data class WaterTodayResponse(
    val success: Boolean,
    val data:    WaterTodayData?
)
data class WaterTodayData(
    val date:                   String,
    val email:                  String,
    val goal_ml:                Int,
    val cumulative_ml:          Int,
    val reminder_interval_min:  Int,
    val reminder_active:        Boolean,
    val logs:                   List<WaterDrinkLog>
)
data class WaterDrinkLog(
    val id:             String,
    val amount_ml:      Int,
    val container_type: String,
    val logged_at:      Long
)

// POST /water/settings — Response
data class WaterSettingsResponse(
    val success: Boolean,
    val message: String
)

// ════════════════════════════════════════════════════════════════════════════
//  RETROFIT INTERFACE
// ════════════════════════════════════════════════════════════════════════════
interface WaterApiInterface {

    // Called on: activity open — loads today's intake + goal from server
    @GET("water/today")
    fun getTodayData(
        @Query("email") email: String,
        @Query("date")  date:  String
    ): Call<WaterTodayResponse>

    // Called on: user taps "Save & Activate Reminders"
    @POST("water/settings")
    fun saveSettings(@Body request: WaterSettingsRequest): Call<WaterSettingsResponse>

    // Called on: user taps Drink button (bottom sheet) OR notification action
    @POST("water/log")
    fun logDrink(@Body request: WaterLogRequest): Call<WaterLogResponse>
}

// ════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION CHANNEL
// ════════════════════════════════════════════════════════════════════════════
fun createWaterNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(CHANNEL_ID, "Water Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description      = "Reminds you to drink water"
            enableLights(true)
            lightColor       = AColor.CYAN
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  BROADCAST RECEIVER
// ════════════════════════════════════════════════════════════════════════════
class WaterReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_LOG_DRINK -> {
                val ml   = intent.getIntExtra(EXTRA_CONTAINER_ML, 250)
                val pre  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val cur  = pre.getInt(PREF_INTAKE, 0)
                val goal = pre.getInt(PREF_GOAL, 2000)
                val newTotal = cur + ml
                pre.edit().putInt(PREF_INTAKE, newTotal).apply()

                // POST to backend (fire-and-forget from receiver)
                postDrinkFromReceiver(context, ml, newTotal, goal)

                NotificationManagerCompat.from(context).cancel(NOTIF_ID)

                // If goal reached → stop reminders
                if (newTotal >= goal) {
                    cancelReminders(context)
                    pre.edit().putBoolean(PREF_REMINDERS_ON, false).apply()
                } else {
                    scheduleNextReminder(context)
                }
            }
            else -> {
                // Alarm fired — check goal not already met
                val pre  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val cur  = pre.getInt(PREF_INTAKE, 0)
                val goal = pre.getInt(PREF_GOAL, 2000)
                if (cur < goal) {
                    showWaterNotification(context)
                    scheduleNextReminder(context)
                }
                // If goal met, do nothing — reminders naturally stop
            }
        }
    }

    private fun postDrinkFromReceiver(context: Context, ml: Int, cumulative: Int, goal: Int) {
        val sp    = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        val email = sp.getString("email", null) ?: return
        val date  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create()).build()
        val api = retrofit.create(WaterApiInterface::class.java)

        api.logDrink(WaterLogRequest(email, ml, "glass", cumulative, goal, date = date))
            .enqueue(object : Callback<WaterLogResponse> {
                override fun onResponse(call: Call<WaterLogResponse>, response: Response<WaterLogResponse>) {}
                override fun onFailure(call: Call<WaterLogResponse>, t: Throwable) {}
            })
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION BUILDER
// ════════════════════════════════════════════════════════════════════════════
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun showWaterNotification(context: Context) {
    val prefs    = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val goalMl   = prefs.getInt(PREF_GOAL, 2000)
    val intakeMl = prefs.getInt(PREF_INTAKE, 0)
    val pct      = ((intakeMl * 100f) / goalMl).toInt().coerceIn(0, 100)

    val fullScreenPi = PendingIntent.getActivity(
        context, 0,
        Intent(context, WaterDrinkActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val quickDrinkPi = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, WaterReminderReceiver::class.java).apply { action = ACTION_LOG_DRINK; putExtra(EXTRA_CONTAINER_ML, 250) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_water)
        .setContentTitle("💧 Time to Hydrate!")
        .setContentText("$intakeMl ml / $goalMl ml ($pct%) — Keep going!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(fullScreenPi)
        .addAction(0, "Drink 250ml", quickDrinkPi)
        .setAutoCancel(true)
        .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }
}

fun scheduleNextReminder(context: Context) {
    val prefs       = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val on          = prefs.getBoolean(PREF_REMINDERS_ON, true)
    if (!on) return
    val intervalMin = prefs.getInt(PREF_INTERVAL_MIN, 60)
    val alarmMgr    = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi          = PendingIntent.getBroadcast(context, 2, Intent(context, WaterReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val triggerAt   = System.currentTimeMillis() + intervalMin * 60_000L
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    else
        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
}

fun cancelReminders(context: Context) {
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi       = PendingIntent.getBroadcast(context, 2, Intent(context, WaterReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmMgr.cancel(pi)
}

// ════════════════════════════════════════════════════════════════════════════
//  CONTAINER DATA  — using PNG drawables
// ════════════════════════════════════════════════════════════════════════════
data class ContainerType(
    val drawableRes: Int,     // R.drawable.ic_water_glass etc.
    val label:       String,
    val ml:          Int,
    val key:         String
)

// PNG images — add these to res/drawable/:
//   ic_water_sip.png    (50 ml)
//   ic_water_glass.png  (250 ml)
//   ic_water_mug.png    (350 ml)
//   ic_water_bottle.png (500 ml)
//   ic_water_custom.png (custom)
private val containers = listOf(

    ContainerType(R.drawable.ic_water_glass,  "Glass",  250, "glass"),
    ContainerType(R.drawable.ic_water_mug,    "Mug",    350, "mug"),
    ContainerType(R.drawable.ic_water_bottle, "Bottle", 500, "bottle"),

)

// ════════════════════════════════════════════════════════════════════════════
//  WATER DRINK ACTIVITY  (launched from notification — half-screen feel)
// ════════════════════════════════════════════════════════════════════════════
class WaterDrinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp    = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val email = sp.getString("email", "") ?: ""
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        val api = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(WaterApiInterface::class.java)

        setContent {
            MaterialTheme {
                WaterDrinkBottomSheet(
                    onDrink = { ml, containerKey ->
                        val cur   = prefs.getInt(PREF_INTAKE, 0)
                        val goal  = prefs.getInt(PREF_GOAL, 2000)
                        val total = cur + ml
                        prefs.edit().putInt(PREF_INTAKE, total).apply()

                        // POST /water/log
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        api.logDrink(WaterLogRequest(email, ml, containerKey, total, goal, date = date))
                            .enqueue(object : Callback<WaterLogResponse> {
                                override fun onResponse(call: Call<WaterLogResponse>, response: Response<WaterLogResponse>) {
                                    if (response.body()?.data?.goal_reached == true) {
                                        cancelReminders(this@WaterDrinkActivity)
                                        prefs.edit().putBoolean(PREF_REMINDERS_ON, false).apply()
                                    }
                                }
                                override fun onFailure(call: Call<WaterLogResponse>, t: Throwable) {}
                            })
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  MAIN ACTIVITY
// ════════════════════════════════════════════════════════════════════════════
class water_tracker : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createWaterNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sp    = getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        val email = sp.getString("email", "") ?: ""

        // Reset intake if date changed
        val today     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate  = prefs.getString(PREF_DATE, "")
        if (lastDate != today) {
            prefs.edit().putInt(PREF_INTAKE, 0).putString(PREF_DATE, today).apply()
        }

        val api = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(WaterApiInterface::class.java)

        setContent {
            MaterialTheme {
                WaterTrackerScreen(
                    email              = email,
                    api                = api,
                    initialGoalMl      = prefs.getInt(PREF_GOAL, 2000),
                    initialIntakeMl    = prefs.getInt(PREF_INTAKE, 0),
                    initialIntervalMin = prefs.getInt(PREF_INTERVAL_MIN, 60),
                    initialRemindersOn = prefs.getBoolean(PREF_REMINDERS_ON, true),
                    onSaveSettings     = { goalMl, intervalMin ->
                        prefs.edit().putInt(PREF_GOAL, goalMl).putInt(PREF_INTERVAL_MIN, intervalMin)
                            .putBoolean(PREF_REMINDERS_ON, true).apply()
                        scheduleNextReminder(this)
                    },
                    onLogDrink         = { ml ->
                        val cur  = prefs.getInt(PREF_INTAKE, 0)
                        prefs.edit().putInt(PREF_INTAKE, cur + ml).apply()
                    },
                    onStopReminders    = {
                        cancelReminders(this)
                        prefs.edit().putBoolean(PREF_REMINDERS_ON, false).apply()
                    },
                    onGoalComplete     = {
                        cancelReminders(this)
                        prefs.edit().putBoolean(PREF_REMINDERS_ON, false).apply()
                    },
                    onBack             = { finish() }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WaterTrackerScreen(
    email:              String,
    api:                WaterApiInterface,
    initialGoalMl:      Int,
    initialIntakeMl:    Int,
    initialIntervalMin: Int,
    initialRemindersOn: Boolean,
    onSaveSettings:     (goalMl: Int, intervalMin: Int) -> Unit,
    onLogDrink:         (ml: Int) -> Unit,
    onStopReminders:    () -> Unit,
    onGoalComplete:     () -> Unit,
    onBack:             () -> Unit
) {
    var currentTab     by remember { mutableIntStateOf(0) }
    var goalMl         by remember { mutableIntStateOf(initialGoalMl) }
    var intakeMl       by remember { mutableIntStateOf(initialIntakeMl) }
    var intervalMin    by remember { mutableIntStateOf(initialIntervalMin) }
    var remindersOn    by remember { mutableStateOf(initialRemindersOn) }
    var showDrinkSheet by remember { mutableStateOf(false) }
    var isLoadingApi   by remember { mutableStateOf(true) }
    val scope          = rememberCoroutineScope()

    // ── GET /water/today on open ──────────────────────────────────────────
    LaunchedEffect(Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        api.getTodayData(email, today).enqueue(object : Callback<WaterTodayResponse> {
            override fun onResponse(call: Call<WaterTodayResponse>, response: Response<WaterTodayResponse>) {
                isLoadingApi = false
                response.body()?.data?.let { data ->
                    goalMl      = data.goal_ml
                    intakeMl    = data.cumulative_ml
                    intervalMin = data.reminder_interval_min
                    remindersOn = data.reminder_active
                }
            }
            override fun onFailure(call: Call<WaterTodayResponse>, t: Throwable) { isLoadingApi = false }
        })
    }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        // Static decorative blobs — no blur(), no animation
        Box(Modifier.size(300.dp).offset(140.dp, (-50).dp).background(BlobPinkBrush, CircleShape))
        Box(Modifier.size(200.dp).offset(160.dp, 420.dp).background(BlobBlueBrush, CircleShape))
        Box(Modifier.size(240.dp).offset((-50).dp, 550.dp).background(BlobViolBrush, CircleShape))

        Column(Modifier.fillMaxSize()) {
            WaterHeader(onBack = onBack)
            WaterTabBar(selected = currentTab, onSelect = { currentTab = it })

            AnimatedContent(
                targetState   = currentTab,
                transitionSpec = { slideInHorizontally { if (targetState > initialState) it else -it } + fadeIn() togetherWith slideOutHorizontally { if (targetState > initialState) -it else it } + fadeOut() },
                label = "water_tab"
            ) { tab ->
                when (tab) {
                    0 -> TodayTab(
                        intakeMl      = intakeMl,
                        goalMl        = goalMl,
                        intervalMin   = intervalMin,
                        remindersOn   = remindersOn,
                        isLoading     = isLoadingApi,
                        onDrinkTap    = { showDrinkSheet = true },
                        onStopReminders = {
                            remindersOn = false
                            onStopReminders()
                        },
                        onGoalComplete = onGoalComplete
                    )
                    1 -> SetupTab(
                        goalMl           = goalMl,
                        intervalMin      = intervalMin,
                        onGoalChange     = { goalMl = it },
                        onIntervalChange = { intervalMin = it },
                        onSave           = {
                            onSaveSettings(goalMl, intervalMin)
                            remindersOn = true
                            // POST /water/settings
                            api.saveSettings(WaterSettingsRequest(email, goalMl, intervalMin, true))
                                .enqueue(object : Callback<WaterSettingsResponse> {
                                    override fun onResponse(c: Call<WaterSettingsResponse>, r: Response<WaterSettingsResponse>) {}
                                    override fun onFailure(c: Call<WaterSettingsResponse>, t: Throwable) {}
                                })
                            scope.launch { delay(300); currentTab = 0 }
                        }
                    )
                }
            }
        }

        // Bottom sheet overlay
        if (showDrinkSheet) {
            WaterDrinkBottomSheet(
                onDrink = { ml, containerKey ->
                    val newTotal = intakeMl + ml
                    intakeMl = newTotal
                    onLogDrink(ml)
                    showDrinkSheet = false

                    // POST /water/log
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    api.logDrink(WaterLogRequest(email, ml, containerKey, newTotal, goalMl, date = date))
                        .enqueue(object : Callback<WaterLogResponse> {
                            override fun onResponse(call: Call<WaterLogResponse>, response: Response<WaterLogResponse>) {
                                // If server confirms goal reached → auto-stop reminders
                                if (response.body()?.data?.goal_reached == true) {
                                    remindersOn = false
                                    onGoalComplete()
                                }
                            }
                            override fun onFailure(call: Call<WaterLogResponse>, t: Throwable) {}
                        })
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
    Box(Modifier.fillMaxWidth().background(HeaderGrad).padding(horizontal = 20.dp, vertical = 20.dp)) {
        // Corner blob — static, no blur
        Box(Modifier.size(130.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp)
            .background(Brush.radialGradient(listOf(WaterBlue.copy(0.20f), Color.Transparent), radius = 260f), CircleShape))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).background(PureWhite.copy(0.08f), CircleShape).border(1.dp, SubtleGrey.copy(0.3f), CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Hydration", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = (-0.5).sp)
                Text("Stay perfectly hydrated", fontSize = 11.sp, color = TextSecondary, letterSpacing = 0.3.sp)
            }
            Box(Modifier.size(40.dp)) // balance
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TAB BAR
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WaterTabBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).background(BgLavender, RoundedCornerShape(20.dp)).padding(4.dp)) {
        listOf("Today", "Setup").forEachIndexed { i, label ->
            val sel = selected == i
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) AccentBrush else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) PureWhite else TextSecondary)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TODAY TAB
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun TodayTab(
    intakeMl:       Int,
    goalMl:         Int,
    intervalMin:    Int,
    remindersOn:    Boolean,
    isLoading:      Boolean,
    onDrinkTap:     () -> Unit,
    onStopReminders: () -> Unit,
    onGoalComplete: () -> Unit
) {
    val pct     = (intakeMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    val animPct by animateFloatAsState(pct, tween(1200, easing = FastOutSlowInEasing), label = "water_fill")

    // Single wave transition — one InfiniteTransition covers both waves
    val waveInf    = rememberInfiniteTransition(label = "wave")
    val waveOffset by waveInf.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "wo")

    // Auto trigger goal complete
    LaunchedEffect(pct) {
        if (pct >= 1f) onGoalComplete()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Spacer(Modifier.height(60.dp))
            CircularProgressIndicator(color = AccentViolet)
        } else {

            // ── Liquid circle ─────────────────────────────────────────────
            Box(
                Modifier.size(220.dp).clip(CircleShape)
                    .border(3.dp, Brush.sweepGradient(listOf(AccentViolet, WaterBlue, HoloPink, AccentViolet)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    drawWaterFill(animPct, waveOffset)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(animPct * 100).toInt()}%", fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        color = if (animPct > 0.4f) PureWhite else WaterDeep, letterSpacing = (-2).sp)
                    Text("$intakeMl ml", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (animPct > 0.4f) PureWhite.copy(0.85f) else TextSecondary)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 3 stat chips ──────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WaterStatChip("Goal",  "${goalMl / 1000f} L",                                WaterBlue,   Modifier.weight(1f))
                WaterStatChip("Done",  "$intakeMl ml",                                        HoloMint,    Modifier.weight(1f))
                WaterStatChip("Left",  "${(goalMl - intakeMl).coerceAtLeast(0)} ml",          HoloPink,    Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // ── Reminder status card ──────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (remindersOn) BgLavender else Color(0xFFFFF0F0))
                    .border(1.dp, if (remindersOn) SubtleGrey else Color(0xFFFFB3B3), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (remindersOn) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        null,
                        tint     = if (remindersOn) HoloMint else Color(0xFFCC3333),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (remindersOn) "Reminders every $intervalMin min" else "Reminders paused",
                        fontSize   = 12.sp,
                        color      = if (remindersOn) TextSecondary else Color(0xFFCC3333),
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.weight(1f)
                    )
                    if (remindersOn) {
                        // ── STOP REMINDERS BUTTON ─────────────────────────
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFEBEB))
                                .border(1.dp, Color(0xFFFFB3B3), RoundedCornerShape(10.dp))
                                .clickable { onStopReminders() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Stop", fontSize = 11.sp, color = Color(0xFFCC3333), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Drink button ──────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp))
                    .background(WaterBrush).clickable { onDrinkTap() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.WaterDrop, null, tint = PureWhite, modifier = Modifier.size(22.dp))
                    Text("Log a Drink", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = 0.3.sp)
                }
            }

            // ── Goal complete celebration ─────────────────────────────────
            if (pct >= 1f) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(HoloMint.copy(0.25f), WaterBlue.copy(0.18f))))
                        .border(1.5.dp, Brush.horizontalGradient(listOf(HoloMint, WaterBlue)), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Goal Complete!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WaterDeep, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Congratulations! You've hit your daily goal.\nDrink more water for bonus hydration! 💧",
                            fontSize = 13.sp, color = TextPrimary, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        // Bonus progress pill
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Brush.horizontalGradient(listOf(WaterDeep, WaterBlue)))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Bonus: ${intakeMl - goalMl} ml extra", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun WaterStatChip(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(16.dp), BgLavender, border = BorderStroke(1.dp, SubtleGrey), shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(6.dp).background(accent, CircleShape))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// Water fill canvas drawing — same wave logic, uses single waveOffset
fun DrawScope.drawWaterFill(pct: Float, waveOffsetDeg: Float) {
    val w      = size.width
    val h      = size.height
    val fillY  = h * (1f - pct)
    val waveAmp = h * 0.04f
    val waveShift = (waveOffsetDeg / 360f) * w

    // Wave 1
    val path1 = Path().apply {
        moveTo(0f, fillY)
        var x = 0f
        while (x <= w) {
            lineTo(x, fillY + sin(((x + waveShift) / w) * 2 * Math.PI.toFloat()) * waveAmp)
            x += 2f
        }
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(path1, Brush.verticalGradient(listOf(WaterBlue.copy(0.85f), WaterDeep.copy(0.95f)), startY = fillY, endY = h))

    // Wave 2 (offset)
    val path2 = Path().apply {
        moveTo(0f, fillY)
        var x = 0f
        while (x <= w) {
            lineTo(x, fillY + sin(((x + waveShift + w * 0.5f) / w) * 2 * Math.PI.toFloat()) * (waveAmp * 0.6f))
            x += 2f
        }
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(path2, WaterBlue.copy(alpha = 0.35f))
}

// ════════════════════════════════════════════════════════════════════════════
//  SETUP TAB
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SetupTab(goalMl: Int, intervalMin: Int, onGoalChange: (Int) -> Unit, onIntervalChange: (Int) -> Unit, onSave: () -> Unit) {
    val goalOptions = listOf(
        500 to "½ L", 1000 to "1 L", 1500 to "1.5 L", 2000 to "2 L",
        2500 to "2.5 L", 3000 to "3 L", 3500 to "3.5 L", 4000 to "4 L",
        5000 to "5 L", 6000 to "6 L", 8000 to "8 L", 10000 to "10 L"
    )
    val suggestions = listOf("🏃 Athlete" to 3500, "💼 Office" to 2000, "🧘 Wellness" to 2500, "🌞 Hot Climate" to 3000, "👶 Light" to 1500)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(4.dp))

        WaterSectionHeader("Daily Goal", "How much water per day")
        Spacer(Modifier.height(12.dp))

        // Suggestions row
        Text("Quick picks", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(suggestions) { _, (label, ml) ->
                val active = goalMl == ml
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (active) AccentBrush else Brush.horizontalGradient(listOf(BgLavender, BgLavender)))
                        .border(1.dp, if (active) AccentViolet else SubtleGrey, RoundedCornerShape(20.dp))
                        .clickable { onGoalChange(ml) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("$label\n${ml / 1000f} L", fontSize = 11.sp, color = if (active) PureWhite else TextSecondary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Custom Amount", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // 4-column goal grid
        goalOptions.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (ml, label) ->
                    val active = goalMl == ml
                    Box(
                        Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (active) AccentBrush else Brush.verticalGradient(listOf(BgLavender, BgLavender)))
                            .border(if (active) 1.5.dp else 1.dp, if (active) AccentViolet else SubtleGrey, RoundedCornerShape(14.dp))
                            .clickable { onGoalChange(ml) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) PureWhite else TextPrimary)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))
        WaterSectionHeader("Reminder Interval", "Drag clock hand to set frequency")
        Spacer(Modifier.height(16.dp))
        AnimatedClockPicker(intervalMin = intervalMin, onIntervalChange = onIntervalChange)
        Spacer(Modifier.height(28.dp))

        // Save button
        Box(
            Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(AccentBrush).clickable { onSave() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.NotificationsActive, null, tint = PureWhite, modifier = Modifier.size(20.dp))
                Text("Save & Activate Reminders", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = 0.3.sp)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun WaterSectionHeader(title: String, sub: String) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(40.dp).height(3.dp).background(AccentBrush, RoundedCornerShape(2.dp)))
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ANIMATED CLOCK PICKER — unchanged logic, improved colours
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun AnimatedClockPicker(intervalMin: Int, onIntervalChange: (Int) -> Unit) {
    val minValue = 10; val maxValue = 180
    fun minToAngle(min: Int) = ((min - minValue).toFloat() / (maxValue - minValue)) * 360f
    fun angleToMin(angle: Float) = (minValue + (((angle % 360 + 360) % 360) / 360f) * (maxValue - minValue)).roundToInt().coerceIn(minValue, maxValue)

    var handAngle by remember { mutableFloatStateOf(minToAngle(intervalMin)) }
    val animAngle by animateFloatAsState(handAngle, spring(0.7f, 200f), label = "clock")
    val haptic    = LocalHapticFeedback.current
    var lastHapticMin by remember { mutableIntStateOf(intervalMin) }

    val currentMin = angleToMin(handAngle)
    val displayText = when {
        currentMin < 60      -> "$currentMin min"
        currentMin % 60 == 0 -> "${currentMin / 60} hr"
        else                 -> "${currentMin / 60} hr ${currentMin % 60} min"
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(displayText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-1).sp)
        Text("between reminders", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        Box(
            Modifier.size(240.dp).pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    var angle = atan2(dy, dx) * (180f / Math.PI.toFloat()) + 90f
                    if (angle < 0) angle += 360f
                    handAngle = angle
                    val newMin = angleToMin(angle)
                    if (newMin != lastHapticMin) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); lastHapticMin = newMin }
                    onIntervalChange(newMin)
                }
            },
            contentAlignment = Alignment.Center
        ) {
            val minToAngleFn: (Int) -> Float = { m -> m * 6f }
            val tickLabels = listOf(10 to "10m", 30 to "30m", 60 to "1h", 90 to "90m", 120 to "2h", 150 to "2.5h", 180 to "3h")
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawClockFace(animAngle, minToAngleFn, tickLabels)
            }
            Box(Modifier.size(52.dp).background(Brush.radialGradient(listOf(AccentViolet.copy(0.2f), Color.Transparent)), CircleShape).border(2.dp, AccentViolet.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Timer, null, tint = AccentViolet, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Quick-pick chips
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15 to "15m", 30 to "30m", 60 to "1h", 90 to "90m", 120 to "2h").forEach { (min, label) ->
                val active = currentMin == min
                Box(
                    Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (active) AccentBrush else Brush.horizontalGradient(listOf(BgLavender, BgLavender)))
                        .clickable { handAngle = minToAngle(min); onIntervalChange(min) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) PureWhite else TextSecondary)
                }
            }
        }
    }
}

private fun DrawScope.drawClockFace(handAngleDeg: Float, minToAngle: (Int) -> Float, tickLabels: List<Pair<Int, String>>) {
    val cx = size.width / 2f; val cy = size.height / 2f
    val radius = size.width / 2f - 8.dp.toPx()

    drawCircle(brush = Brush.sweepGradient(listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.3f), HoloMint.copy(0.2f), AccentViolet.copy(0.4f))), radius = radius, style = Stroke(3.dp.toPx()))

    drawArc(
        brush = Brush.sweepGradient(0f to AccentViolet.copy(0.7f), (handAngleDeg / 360f).coerceIn(0.001f, 0.999f) to HoloPink.copy(0.7f), 1f to AccentViolet.copy(0f)),
        startAngle = -90f, sweepAngle = handAngleDeg, useCenter = false,
        topLeft = Offset(cx - radius, cy - radius), size = Size(radius * 2, radius * 2),
        style = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
    )

    tickLabels.forEach { (min, _) ->
        val rad = ((minToAngle(min) - 90f) * (Math.PI / 180f)).toFloat()
        drawLine(SubtleGrey.copy(0.6f), Offset(cx + cos(rad) * (radius - 14.dp.toPx()), cy + sin(rad) * (radius - 14.dp.toPx())), Offset(cx + cos(rad) * (radius - 4.dp.toPx()), cy + sin(rad) * (radius - 4.dp.toPx())), 2.dp.toPx(), StrokeCap.Round)
    }
    for (deg in 0..330 step 30) {
        val rad = ((deg - 90f) * (Math.PI / 180f)).toFloat()
        drawCircle(SubtleGrey.copy(0.35f), 2.dp.toPx(), Offset(cx + cos(rad) * (radius - 20.dp.toPx()), cy + sin(rad) * (radius - 20.dp.toPx())))
    }

    val handRad = ((handAngleDeg - 90f) * (Math.PI / 180f)).toFloat()
    val handEnd = Offset(cx + cos(handRad) * (radius - 28.dp.toPx()), cy + sin(handRad) * (radius - 28.dp.toPx()))
    drawLine(AccentViolet.copy(0.2f), Offset(cx + 2, cy + 2), Offset(handEnd.x + 2, handEnd.y + 2), 5.dp.toPx(), StrokeCap.Round)
    drawLine(Brush.linearGradient(listOf(AccentViolet, HoloPink), Offset(cx, cy), handEnd), Offset(cx, cy), handEnd, 4.dp.toPx(), StrokeCap.Round)
    drawCircle(Brush.radialGradient(listOf(HoloPink, AccentViolet.copy(0.4f)), handEnd, 8.dp.toPx()), 8.dp.toPx(), handEnd)
    drawCircle(Brush.radialGradient(listOf(AccentViolet, HoloPink.copy(0.6f))), 6.dp.toPx(), Offset(cx, cy))
}

// ════════════════════════════════════════════════════════════════════════════
//  DRINK BOTTOM SHEET — PNG container images, animated carousel
//  Receives onDrink(ml: Int, containerKey: String) so we can log container type
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WaterDrinkBottomSheet(
    onDrink:   (ml: Int, containerKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected      by remember { mutableIntStateOf(1) }  // default: Glass
    var customMl      by remember { mutableIntStateOf(250) }
    var showCustom    by remember { mutableStateOf(false) }
    var visible       by remember { mutableStateOf(false) }
    val listState     = rememberLazyListState()
    val scope         = rememberCoroutineScope()

    LaunchedEffect(Unit) { delay(40); visible = true }

    val slideY by animateFloatAsState(if (visible) 0f else 700f, spring(0.75f, 250f), label = "sheet")

    val selectedContainer = containers[selected]
    val effectiveMl       = if (selectedContainer.key == "custom") customMl else selectedContainer.ml

    Box(Modifier.fillMaxSize().background(DeepBlack.copy(0.55f)).clickable { onDismiss() }) {
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.58f).align(Alignment.BottomCenter)
                .graphicsLayer { translationY = slideY }
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(BgWhite)
                .clickable { /* absorb */ }
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(12.dp))

                // Drag handle
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(SubtleGrey).align(Alignment.CenterHorizontally))

                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("What did you drink?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Swipe to choose container", fontSize = 11.sp, color = TextSecondary)
                    }
                    // Amount badge
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(WaterDeep, WaterBlue))).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("+$effectiveMl ml", fontSize = 13.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── PNG Container Carousel ────────────────────────────────
                LazyRow(
                    state                 = listState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(containers) { i, container ->
                        val isSelected = selected == i
                        val scale by animateFloatAsState(if (isSelected) 1.12f else 0.95f, spring(0.65f), label = "cscale$i")
                        val bgAlpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(200), label = "cbg$i")

                        Column(
                            Modifier
                                .width(82.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            AccentViolet.copy(bgAlpha * 0.18f),
                                            HoloPink.copy(bgAlpha * 0.12f)
                                        )
                                    )
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = if (isSelected) AccentBrush else Brush.horizontalGradient(listOf(SubtleGrey, SubtleGrey)),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .clickable {
                                    selected    = i
                                    showCustom  = container.key == "custom"
                                    scope.launch { listState.animateScrollToItem(i) }
                                }
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // PNG image — 56x56 dp
                            Image(
                                painter            = painterResource(id = container.drawableRes),
                                contentDescription = container.label,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier.size(52.dp)
                            )
                            Text(container.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) TextPrimary else TextSecondary, textAlign = TextAlign.Center)
                            Text(
                                if (container.key == "custom") "Custom" else "${container.ml} ml",
                                fontSize = 10.sp, color = if (isSelected) AccentViolet else TextSecondary, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Custom amount input
                AnimatedVisibility(visible = showCustom, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(Modifier.padding(top = 14.dp)) {
                        Text("Enter amount (ml)", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value         = if (customMl == 0) "" else customMl.toString(),
                            onValueChange = { customMl = it.toIntOrNull() ?: 0 },
                            placeholder   = { Text("e.g. 400", color = TextSecondary.copy(0.4f)) },
                            singleLine    = true,
                            shape         = RoundedCornerShape(14.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AccentViolet,
                                unfocusedBorderColor = SubtleGrey,
                                focusedContainerColor   = PureWhite,
                                unfocusedContainerColor = PureWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Drink button
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(WaterBrush)
                        .clickable { if (effectiveMl > 0) onDrink(effectiveMl, selectedContainer.key) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.WaterDrop, null, tint = PureWhite, modifier = Modifier.size(20.dp))
                        Text("Drink +$effectiveMl ml", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite, letterSpacing = 0.5.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

