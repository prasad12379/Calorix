package com.example.run

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

// ─── Color Palette (HD Gradient Schema) ────────────────────────────────────────
private val BgDeep        = Color(0xFF1C2220)
private val BgDark        = Color(0xFF272F2D)
private val BgCard        = Color(0xFF2E3836)
private val BgCardLight   = Color(0xFF364240)
private val SiennaDeep    = Color(0xFF4A1C12)
private val SiennaMid     = Color(0xFF6E2B1E)
private val SiennaLight   = Color(0xFF9B4030)
private val SiennaGlow    = Color(0xFFBF6050)
private val SteelDeep     = Color(0xFF5A7A8A)
private val SteelMid      = Color(0xFF8FAEC0)
private val SteelLight    = Color(0xFFB0CCDA)
private val VioletDeep    = Color(0xFF4A3D7A)
private val VioletMid     = Color(0xFF7A6BA8)
private val SubtleBorder  = Color(0xFF3D4D4A)
private val TextPrimary   = Color(0xFFEEEEEC)
private val TextMuted     = Color(0xFF8A9E9A)

// ─── Per-type gradient styling ─────────────────────────────────────────────────
private fun activityGradient(type: String): List<Color> = when (type.uppercase()) {
    "RUNNING" -> listOf(SiennaDeep, SiennaMid, SiennaLight)
    "WALKING" -> listOf(SteelDeep, SteelMid)
    "CYCLING" -> listOf(VioletDeep, VioletMid)
    else      -> listOf(SiennaDeep, SiennaMid)
}

private fun activityAccentColor(type: String): Color = when (type.uppercase()) {
    "RUNNING" -> SiennaGlow
    "WALKING" -> SteelLight
    "CYCLING" -> Color(0xFF9B8FD4)
    else      -> SiennaGlow
}

private fun activityIcon(type: String): ImageVector = when (type.uppercase()) {
    "RUNNING" -> Icons.Filled.DirectionsRun
    "WALKING" -> Icons.Filled.DirectionsWalk
    "CYCLING" -> Icons.Filled.DirectionsBike
    else      -> Icons.Filled.FitnessCenter
}

private fun filterIcon(type: String): ImageVector = when (type) {
    "RUNNING" -> Icons.Filled.DirectionsRun
    "WALKING" -> Icons.Filled.DirectionsWalk
    "CYCLING" -> Icons.Filled.DirectionsBike
    else      -> Icons.Filled.GridView
}

private val filters = listOf("ALL", "RUNNING", "WALKING", "CYCLING")

class AF : Fragment() {

    private lateinit var apiInterface: ApiInterface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initRetrofit()
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    ActivityScreen(
                        apiInterface = apiInterface,
                        context      = requireContext(),
                        onStartWorkout = {
                            Toast.makeText(requireContext(), "Start workout from Home tab", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiInterface = retrofit.create(ApiInterface::class.java)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityScreen(
    apiInterface: ApiInterface,
    context: Context,
    onStartWorkout: () -> Unit
) {
    var fullList      by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var currentFilter by remember { mutableStateOf("ALL") }
    var isLoading     by remember { mutableStateOf(true) }
    var hasError      by remember { mutableStateOf(false) }

    val displayList = remember(fullList, currentFilter) {
        if (currentFilter == "ALL") fullList
        else fullList.filter { it.workout_mode.equals(currentFilter, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", null)
        if (email == null) { isLoading = false; hasError = true; return@LaunchedEffect }

        apiInterface.getActivities(email).enqueue(object : Callback<ActivityListResponse> {
            override fun onResponse(call: Call<ActivityListResponse>, response: Response<ActivityListResponse>) {
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    fullList = response.body()!!.data
                } else {
                    hasError = true
                    Toast.makeText(context, "Failed to load: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ActivityListResponse>, t: Throwable) {
                isLoading = false
                hasError  = true
                Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgDeep, BgDark, Color(0xFF1F2826))
                )
            )
    ) {
        // Sienna blob — bottom left
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SiennaMid.copy(alpha = 0.3f),
                            SiennaDeep.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 450f
                    ),
                    CircleShape
                )
        )
        // Steel blob — top right
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-40).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SteelDeep.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = 400f
                    ),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            ActivityHeader(totalCount = fullList.size, isLoading = isLoading)

            FilterChipsRow(
                selected  = currentFilter,
                onSelect  = { currentFilter = it }
            )

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> LoadingState()
                    displayList.isEmpty() -> EmptyActivityState(
                        filter         = currentFilter,
                        onStartWorkout = onStartWorkout
                    )
                    else -> ActivityList(items = displayList)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HEADER
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityHeader(totalCount: Int, isLoading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text          = "ACTIVITY",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextMuted,
            letterSpacing = 1.6.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text          = "Your workouts.",
            fontSize      = 30.sp,
            fontWeight    = FontWeight.Bold,
            color         = TextPrimary,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(6.dp))

        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "count"
        ) { loading ->
            if (loading) {
                ShimmerLine(width = 120.dp, height = 14.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                Brush.radialGradient(colors = listOf(SteelLight, SteelMid)),
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = "$totalCount total workouts",
                        fontSize = 13.sp,
                        color    = TextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(SiennaMid, SteelMid.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  FILTER CHIPS ROW
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun FilterChipsRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        filters.forEach { filter ->
            FilterChipItem(
                label      = filter.lowercase().replaceFirstChar { it.uppercase() },
                icon       = filterIcon(filter),
                isSelected = selected == filter,
                gradient   = if (filter == "ALL") listOf(SiennaDeep, SiennaMid)
                else activityGradient(filter),
                onClick    = { onSelect(filter) }
            )
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected)
                    Brush.linearGradient(colors = gradient)
                else
                    Brush.linearGradient(colors = listOf(BgCard, BgCardLight))
            )
            .border(
                width = 1.dp,
                brush = if (isSelected)
                    Brush.linearGradient(colors = listOf(gradient.last().copy(alpha = 0.6f), gradient.first()))
                else
                    Brush.linearGradient(colors = listOf(SubtleBorder, Color.Transparent)),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (isSelected) TextPrimary else TextMuted,
                modifier           = Modifier.size(15.dp)
            )
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color      = if (isSelected) TextPrimary else TextMuted
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ACTIVITY LIST
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityList(items: List<ActivityItem>) {
    val listState = rememberLazyListState()

    LazyColumn(
        state           = listState,
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(
            start  = 20.dp,
            end    = 20.dp,
            top    = 16.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { i, _ -> i }) { index, item ->
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter   = fadeIn(tween(300, delayMillis = (index * 60).coerceAtMost(300))) +
                        slideInVertically(
                            tween(350, delayMillis = (index * 60).coerceAtMost(300)),
                            initialOffsetY = { it / 3 }
                        )
            ) {
                ActivityCard(item = item)
            }
        }
    }
}

@Composable
fun ActivityCard(item: ActivityItem) {
    val gradient     = activityGradient(item.workout_mode)
    val accentColor  = activityAccentColor(item.workout_mode)
    val icon         = activityIcon(item.workout_mode)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(colors = listOf(BgCard, BgCardLight))
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(SubtleBorder, gradient.first().copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble with activity gradient
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(colors = gradient)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor.copy(alpha = 0.4f), gradient.first())
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = item.workout_mode,
                    tint               = TextPrimary,
                    modifier           = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.workout_mode.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = formatActivityDate(item.date ?: ""),
                    fontSize = 12.sp,
                    color    = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatPill(value = "${item.distance} km", gradient = gradient)
                Spacer(Modifier.height(5.dp))
                StatPill(value = "${item.duration} min", gradient = listOf(BgCardLight, BgCardLight))
            }
        }
    }
}

@Composable
fun StatPill(value: String, gradient: List<Color>) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = gradient.map { it.copy(alpha = 0.25f) }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(gradient.last().copy(alpha = 0.3f), Color.Transparent)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = value,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (gradient.size == 1 || gradient.first() == gradient.last()) TextMuted
            else gradient.last().copy(alpha = 1f).let {
                // lighten for readability on dark bg
                Color(
                    red   = (it.red * 1.3f).coerceAtMost(1f),
                    green = (it.green * 1.3f).coerceAtMost(1f),
                    blue  = (it.blue * 1.3f).coerceAtMost(1f),
                    alpha = 1f
                )
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun EmptyActivityState(filter: String, onStartWorkout: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (filter == "ALL") listOf(SiennaDeep, SiennaMid)
                        else activityGradient(filter)
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(SiennaGlow.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (filter == "ALL") Icons.Filled.FitnessCenter
                else activityIcon(filter),
                contentDescription = null,
                tint               = TextPrimary,
                modifier           = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text       = if (filter == "ALL") "No workouts yet"
            else "No ${filter.lowercase()} sessions",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = if (filter == "ALL") "Start your first workout\nand track your progress here."
            else "Try a different filter\nor log your first session.",
            fontSize   = 14.sp,
            color      = TextMuted,
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp
        )

        if (filter == "ALL") {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SiennaDeep, SiennaMid, SiennaLight)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SiennaGlow.copy(alpha = 0.5f), SiennaDeep)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onStartWorkout() }
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    text       = "Start First Workout →",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  LOADING STATE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun LoadingState() {
    Column(
        modifier        = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) { ShimmerActivityCard() }
    }
}

@Composable
fun ShimmerActivityCard() {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmer.animateFloat(
        initialValue  = 0.15f,
        targetValue   = 0.4f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(colors = listOf(BgCard, BgCardLight))
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = listOf(SubtleBorder, Color.Transparent)),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SubtleBorder.copy(alpha = alpha))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(width = 110.dp, height = 15.dp, alpha = alpha)
                Spacer(Modifier.height(8.dp))
                ShimmerLine(width = 80.dp, height = 12.dp, alpha = alpha)
            }
            Column(horizontalAlignment = Alignment.End) {
                ShimmerLine(width = 55.dp, height = 22.dp, alpha = alpha)
                Spacer(Modifier.height(6.dp))
                ShimmerLine(width = 55.dp, height = 22.dp, alpha = alpha)
            }
        }
    }
}

@Composable
fun ShimmerLine(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    alpha: Float = 0.3f
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(SubtleBorder.copy(alpha = alpha))
    )
}

private fun formatActivityDate(dateStr: String): String {
    return try {
        val input  = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val output = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        output.format(input.parse(dateStr)!!)
    } catch (e: Exception) { dateStr.take(10).ifEmpty { "—" } }
}


