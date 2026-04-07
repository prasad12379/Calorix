package com.example.run

import UserResponse
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

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
private val SteelGlow     = Color(0xFFCCDEE8)
private val VioletAccent  = Color(0xFF7A6BA8)
private val SubtleBorder  = Color(0xFF3D4D4A)
private val TextPrimary   = Color(0xFFEEEEEC)
private val TextMuted     = Color(0xFF8A9E9A)
private val DangerDeep    = Color(0xFF5C1A1A)
private val DangerMid     = Color(0xFF8B2E2E)
private val DangerLight   = Color(0xFFB04040)

class ProfileFragment : Fragment() {

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
                    ProfileScreen(
                        apiInterface    = apiInterface,
                        context         = requireContext(),
                        onNotifications = {
                            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
                        },
                        onHelp = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("dhokaneprasad6@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "CaloriX App Support")
                                putExtra(Intent.EXTRA_TEXT, "Hello Prasad,\n\nI need help with CaloriX app.")
                            }
                            startActivity(intent)
                        },
                        onAbout  = {
                            startActivity(Intent(requireContext(), AboutActivity::class.java))
                        },
                        onLogout = { logoutUser() }
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

    private fun logoutUser() {
        val sharedPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DATA HOLDER
// ══════════════════════════════════════════════════════════════════════════════
data class ProfileData(
    val name: String       = "",
    val email: String      = "",
    val height: String     = "",
    val weight: String     = "",
    val age: String        = "",
    val memberSince: String = ""
)

// ══════════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ProfileScreen(
    apiInterface: ApiInterface,
    context: Context,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit
) {
    var profile by remember { mutableStateOf(ProfileData()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        val isCached   = sharedPref.getBoolean("profile_cached", false)

        if (isCached) {
            profile = ProfileData(
                name        = sharedPref.getString("name", "N/A") ?: "N/A",
                email       = sharedPref.getString("email", "N/A") ?: "N/A",
                height      = sharedPref.getString("height", "0") ?: "0",
                weight      = sharedPref.getString("weight", "0") ?: "0",
                age         = sharedPref.getString("age", "0") ?: "0",
                memberSince = formatDate(sharedPref.getString("created_at", "") ?: "")
            )
            isLoading = false
        } else {
            val email = sharedPref.getString("email", null)
            if (email == null) {
                isLoading = false
                return@LaunchedEffect
            }
            apiInterface.getUser(email).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!.data
                        sharedPref.edit().apply {
                            putString("name", user.name)
                            putString("email", user.email)
                            putString("height", user.height.toString())
                            putString("weight", user.weight.toString())
                            putString("age", user.age.toString())
                            putString("created_at", user.created_at)
                            putBoolean("profile_cached", true)
                            apply()
                        }
                        profile = ProfileData(
                            name        = user.name,
                            email       = user.email,
                            height      = user.height.toString(),
                            weight      = user.weight.toString(),
                            age         = user.age.toString(),
                            memberSince = formatDate(user.created_at)
                        )
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    isLoading = false
                }
            })
        }
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
        // Sienna blob — top right
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 180.dp, y = (-50).dp)
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
        // Steel blob — mid left
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-60).dp, y = 300.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SteelDeep.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = 350f
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(profile = profile, isLoading = isLoading)

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = !isLoading,
                enter   = fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { it / 2 })
            ) {
                StatsRow(profile = profile)
            }

            Spacer(Modifier.height(28.dp))

            SectionLabel("Account")

            SettingsCard {
                SettingsRow(
                    icon     = Icons.Filled.Notifications,
                    iconGradient = listOf(SteelDeep, SteelMid),
                    label    = "Notifications",
                    subtitle = "Manage alerts & reminders",
                    onClick  = onNotifications
                )
                RowDivider()
                SettingsRow(
                    icon     = Icons.Filled.HelpOutline,
                    iconGradient = listOf(SiennaDeep, SiennaMid),
                    label    = "Help & Support",
                    subtitle = "Get in touch with us",
                    onClick  = onHelp
                )
                RowDivider()
                SettingsRow(
                    icon     = Icons.Filled.Info,
                    iconGradient = listOf(Color(0xFF4A3D7A), VioletAccent),
                    label    = "About",
                    subtitle = "App info & version",
                    onClick  = onAbout
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionLabel("Danger zone")

            // Logout card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BgCard, BgCardLight)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(DangerMid.copy(alpha = 0.5f), DangerDeep.copy(alpha = 0.2f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                SettingsRow(
                    icon         = Icons.Filled.Logout,
                    iconGradient = listOf(DangerDeep, DangerMid),
                    label        = "Log out",
                    subtitle     = "You'll need to sign in again",
                    labelColor   = DangerLight,
                    onClick      = onLogout,
                    showChevron  = false
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HEADER
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ProfileHeader(profile: ProfileData, isLoading: Boolean) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text          = "MY PROFILE",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextMuted,
            letterSpacing = 1.6.sp
        )

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar — sienna gradient
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SiennaDeep, SiennaMid, SiennaLight)
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SiennaGlow.copy(alpha = 0.6f), SteelDeep.copy(alpha = 0.3f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color     = TextPrimary,
                        modifier  = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = profile.name.take(1).uppercase(),
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            Column {
                if (isLoading) {
                    ShimmerBox(width = 140.dp, height = 20.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(width = 100.dp, height = 14.dp)
                } else {
                    Text(
                        text       = profile.name,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = profile.email,
                        fontSize = 13.sp,
                        color    = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!isLoading && profile.memberSince.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            // Member since pill — charcoal with gradient border
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BgCard, BgCardLight)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SteelMid.copy(alpha = 0.4f), SubtleBorder)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text          = "Member since ${profile.memberSince}",
                    fontSize      = 11.sp,
                    color         = SteelLight,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // HD gradient divider
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
//  STATS ROW
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun StatsRow(profile: ProfileData) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier     = Modifier.weight(1f),
            value        = profile.height,
            unit         = "cm",
            label        = "Height",
            accentColors = listOf(SteelDeep, SteelMid)
        )
        StatCard(
            modifier     = Modifier.weight(1f),
            value        = profile.weight,
            unit         = "kg",
            label        = "Weight",
            accentColors = listOf(SiennaDeep, SiennaLight)
        )
        StatCard(
            modifier     = Modifier.weight(1f),
            value        = profile.age,
            unit         = "yrs",
            label        = "Age",
            accentColors = listOf(Color(0xFF4A3D7A), VioletAccent)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    value: String,
    unit: String,
    label: String,
    accentColors: List<Color>
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BgCard, BgCardLight)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(SubtleBorder, accentColors.first().copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gradient accent dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        Brush.radialGradient(colors = listOf(accentColors.last(), accentColors.first())),
                        CircleShape
                    )
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = value,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text     = unit,
                    fontSize = 11.sp,
                    color    = TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text          = label,
                fontSize      = 11.sp,
                color         = TextMuted,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SETTINGS COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = TextMuted,
        letterSpacing = 1.4.sp,
        modifier      = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BgCard, BgCardLight)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(SubtleBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconGradient: List<Color>,
    label: String,
    subtitle: String,
    labelColor: Color = TextPrimary,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color   = Color.Transparent
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon pill with gradient background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(colors = iconGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = TextPrimary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = label,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = labelColor
                )
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = TextMuted
                )
            }

            if (showChevron) {
                Icon(
                    imageVector        = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint               = TextMuted,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 72.dp)
            .background(SubtleBorder)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  SHIMMER PLACEHOLDER
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShimmerBox(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmer.animateFloat(
        initialValue  = 0.2f,
        targetValue   = 0.5f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "alpha"
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(SubtleBorder.copy(alpha = alpha))
    )
}

private fun formatDate(dateStr: String): String {
    return try {
        val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        output.format(input.parse(dateStr)!!)
    } catch (e: Exception) { "N/A" }
}