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

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX PALETTE  — replaces the old dark-green / sienna schema
// ═══════════════════════════════════════════════════════════════════════════════
private val BgWhite        = Color(0xFFFAF9FF)   // screen background
private val BgLavender     = Color(0xFFECE8F5)   // card surface
private val BgCard         = Color(0xFFFFFFFF)   // card fill
private val BgCardLight    = Color(0xFFF5F3FC)   // card hover / alt surface
private val DeepBlack      = Color(0xFF0A0A0A)   // header background
private val HeaderDark     = Color(0xFF1C1826)   // header gradient end
private val PureWhite      = Color(0xFFFFFFFF)
private val AccentViolet   = Color(0xFF9B8FD4)   // primary accent
private val AccentVioletDk = Color(0xFF6B5FA4)   // darker violet for gradients
private val HoloPink       = Color(0xFFE8B4D8)   // secondary accent
private val HoloMint       = Color(0xFFAEE8D8)   // tertiary accent
private val SubtleGrey     = Color(0xFFDDD8EE)   // border / divider
private val TextPrimary    = Color(0xFF0A0A0A)   // main text
private val TextSecondary  = Color(0xFF7A7490)   // muted text

// ═══════════════════════════════════════════════════════════════════════════════
//  PER-TYPE GRADIENT  — maps workout modes to CaloriX gradients
// ═══════════════════════════════════════════════════════════════════════════════
private fun activityGradient(type: String): List<Color> = when (type.uppercase()) {
    "RUNNING" -> listOf(Color(0xFF2A1F4A), AccentViolet)          // deep violet → violet
    "WALKING" -> listOf(Color(0xFF0F2A1A), Color(0xFF2E7D52))     // deep green → green
    "CYCLING" -> listOf(Color(0xFF2A1A0F), Color(0xFF8B5E28))     // deep amber → amber
    else      -> listOf(DeepBlack, AccentViolet)
}

private fun activityAccentColor(type: String): Color = when (type.uppercase()) {
    "RUNNING" -> AccentViolet
    "WALKING" -> Color(0xFF2E7D52)
    "CYCLING" -> Color(0xFF8B5E28)
    else      -> AccentViolet
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

// ═══════════════════════════════════════════════════════════════════════════════
//  FRAGMENT  — unchanged
// ═══════════════════════════════════════════════════════════════════════════════
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
                        apiInterface   = apiInterface,
                        context        = requireContext(),
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

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityScreen(
    apiInterface:   ApiInterface,
    context:        Context,
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
            .background(BgWhite)   // ← BgWhite instead of dark gradient
    ) {
        // HoloPink blob — top right  (mirrors HomeFragment)
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(HoloPink.copy(0.45f), HoloMint.copy(0.15f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // AccentViolet blob — bottom left
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 60.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentViolet.copy(0.35f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            ActivityHeader(totalCount = fullList.size, isLoading = isLoading)
            FilterChipsRow(selected = currentFilter, onSelect = { currentFilter = it })
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading          -> LoadingState()
                    displayList.isEmpty() -> EmptyActivityState(
                        filter         = currentFilter,
                        onStartWorkout = onStartWorkout
                    )
                    else -> ActivityList(items = displayList)
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEADER  — dark banner matching HomeFragment's PremiumHeader
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityHeader(totalCount: Int, isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(DeepBlack, HeaderDark))
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Corner glow — same as PremiumHeader
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.2f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column {
            // Eyebrow label
            Text(
                text          = "ACTIVITY",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = TextSecondary.copy(0.7f),
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(6.dp))
            // Title
            Text(
                text          = "Your workouts.",
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Bold,
                color         = PureWhite,
                letterSpacing = (-0.8).sp
            )
            Spacer(Modifier.height(4.dp))
            // Gradient underbar — matches HomeFragment brand bar
            Box(
                Modifier
                    .width(52.dp)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(AccentViolet, HoloPink)),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.height(10.dp))

            AnimatedContent(
                targetState  = isLoading,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label        = "count"
            ) { loading ->
                if (loading) {
                    ShimmerLine(width = 120.dp, height = 14.dp)
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PureWhite.copy(0.08f)
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(HoloMint, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text     = "$totalCount total workouts",
                                fontSize = 11.sp,
                                color    = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FILTER CHIPS ROW
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun FilterChipsRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        filters.forEach { filter ->
            FilterChipItem(
                label      = filter.lowercase().replaceFirstChar { it.uppercase() },
                icon       = filterIcon(filter),
                isSelected = selected == filter,
                gradient   = if (filter == "ALL") listOf(DeepBlack, AccentViolet)
                else activityGradient(filter),
                onClick    = { onSelect(filter) }
            )
        }
    }
}

@Composable
fun FilterChipItem(
    label:      String,
    icon:       ImageVector,
    isSelected: Boolean,
    gradient:   List<Color>,
    onClick:    () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label         = "chipScale"
    )
    val bgColor    by animateColorAsState(
        targetValue   = if (isSelected) Color.Transparent else SubtleGrey,
        animationSpec = tween(250),
        label         = "chipBg"
    )
    val textColor  by animateColorAsState(
        targetValue   = if (isSelected) PureWhite else TextSecondary,
        animationSpec = tween(250),
        label         = "chipText"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) Brush.linearGradient(gradient)
                else            Brush.linearGradient(listOf(SubtleGrey, BgLavender))
            )
            .border(
                width = 1.dp,
                brush = if (isSelected)
                    Brush.linearGradient(listOf(gradient.last().copy(0.5f), gradient.first()))
                else
                    Brush.linearGradient(listOf(SubtleGrey, Color.Transparent)),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = textColor,
                modifier           = Modifier.size(15.dp)
            )
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color      = textColor
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ACTIVITY LIST  — unchanged
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityList(items: List<ActivityItem>) {
    val listState = rememberLazyListState()
    LazyColumn(
        state               = listState,
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { i, _ -> i }) { index, item ->
            AnimatedVisibility(
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

// ═══════════════════════════════════════════════════════════════════════════════
//  ACTIVITY CARD  — white card language matching HomeFragment cards
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ActivityCard(item: ActivityItem) {
    val gradient    = activityGradient(item.workout_mode)
    val accentColor = activityAccentColor(item.workout_mode)
    val icon        = activityIcon(item.workout_mode)

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(20.dp),
        color           = BgCard,
        shadowElevation = 6.dp,
        border          = BorderStroke(1.dp, SubtleGrey)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble with workout gradient
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(gradient))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(accentColor.copy(0.4f), gradient.first())
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = item.workout_mode,
                    tint               = PureWhite,
                    modifier           = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.workout_mode.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = formatActivityDate(item.date ?: ""),
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatPill(value = "${item.distance} km",  gradient = gradient)
                Spacer(Modifier.height(5.dp))
                StatPill(value = "${item.duration} min", gradient = listOf(BgLavender, BgLavender))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STAT PILL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StatPill(value: String, gradient: List<Color>) {
    val isNeutral = gradient.first() == gradient.last() || gradient.first() == BgLavender
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(gradient.map { it.copy(alpha = if (isNeutral) 1f else 0.15f) })
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        if (isNeutral) SubtleGrey else gradient.last().copy(0.4f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = value,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isNeutral) TextSecondary
            else Color(
                red   = (gradient.last().red   * 1.2f).coerceAtMost(1f),
                green = (gradient.last().green * 1.2f).coerceAtMost(1f),
                blue  = (gradient.last().blue  * 1.2f).coerceAtMost(1f),
                alpha = 1f
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun EmptyActivityState(filter: String, onStartWorkout: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(BgLavender)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(AccentViolet.copy(0.4f), HoloPink.copy(0.2f))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (filter == "ALL") Icons.Filled.FitnessCenter
                else activityIcon(filter),
                contentDescription = null,
                tint               = AccentViolet,
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
            textAlign  = TextAlign.Center,
            letterSpacing = (-0.4).sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = if (filter == "ALL") "Start your first workout\nand track your progress here."
            else "Try a different filter\nor log your first session.",
            fontSize   = 14.sp,
            color      = TextSecondary,
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp
        )

        if (filter == "ALL") {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(DeepBlack, HeaderDark))
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(SubtleGrey.copy(0.3f), Color.Transparent)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onStartWorkout() }
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.DirectionsRun,
                        contentDescription = null,
                        tint     = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text       = "Start First Workout →",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = PureWhite,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  LOADING STATE  — shimmer cards on white background
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun LoadingState() {
    Column(
        modifier            = Modifier
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
        initialValue  = 0.4f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "alpha"
    )
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(20.dp),
        color           = BgCard,
        shadowElevation = 3.dp,
        border          = BorderStroke(1.dp, SubtleGrey)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SubtleGrey.copy(alpha = alpha))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(width = 110.dp, height = 15.dp, alpha = alpha)
                Spacer(Modifier.height(8.dp))
                ShimmerLine(width = 80.dp,  height = 12.dp, alpha = alpha)
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
    width:  androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    alpha:  Float = 0.5f
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(SubtleGrey.copy(alpha = alpha))
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  DATE FORMATTER  — unchanged
// ═══════════════════════════════════════════════════════════════════════════════
private fun formatActivityDate(dateStr: String): String {
    return try {
        val input  = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val output = java.text.SimpleDateFormat("dd MMM yyyy",           java.util.Locale.getDefault())
        output.format(input.parse(dateStr)!!)
    } catch (e: Exception) {
        dateStr.take(10).ifEmpty { "—" }
    }
}