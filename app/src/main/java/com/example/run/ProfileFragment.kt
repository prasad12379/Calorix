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

// ─── Color Palette: Black · Gray · White · Electric Blue ───────────────────────
private val BgBlack      = Color(0xFF000000)
private val BgDark       = Color(0xFF111111)
private val BgCard       = Color(0xFF1C1C1C)
private val BgCardLight  = Color(0xFF2A2A2A)
private val GrayDark     = Color(0xFF3D3D3D)
private val GrayMid      = Color(0xFF6B6B6B)
private val GrayLight    = Color(0xFF9E9E9E)
private val GrayGhost    = Color(0xFFE0E0E0)
private val BlueVivid    = Color(0xFF0083C9)
private val BlueDark     = Color(0xFF005A8C)
private val BlueDeep     = Color(0xFF003D63)
private val BlueLight    = Color(0xFF29A8E8)
private val SubtleBorder = Color(0xFF2C2C2C)
private val TextPrimary  = Color(0xFFF5F5F5)
private val TextMuted    = Color(0xFF8A8A8A)
private val DangerMid    = Color(0xFFCC3333)
private val DangerDark   = Color(0xFF8B1A1A)

class ProfileFragment : Fragment() {
    private lateinit var apiInterface: ApiInterface

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initRetrofit()
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    ProfileScreen(
                        apiInterface    = apiInterface,
                        context         = requireContext(),
                        onNotifications = { startActivity(Intent(requireContext(), NotificationsActivity::class.java)) },
                        onHelp = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("dhokaneprasad6@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "CaloriX App Support")
                                putExtra(Intent.EXTRA_TEXT, "Hello Prasad,\n\nI need help with CaloriX app.")
                            }
                            startActivity(intent)
                        },
                        onAbout  = { startActivity(Intent(requireContext(), AboutActivity::class.java)) },
                        onLogout = { logoutUser() }
                    )
                }
            }
        }
    }

    private fun initRetrofit() {
        apiInterface = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiInterface::class.java)
    }

    private fun logoutUser() {
        requireContext().getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(requireContext(), SignInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}

data class ProfileData(
    val name: String = "", val email: String = "",
    val height: String = "", val weight: String = "",
    val age: String = "", val memberSince: String = ""
)

@Composable
fun ProfileScreen(
    apiInterface: ApiInterface, context: Context,
    onNotifications: () -> Unit, onHelp: () -> Unit,
    onAbout: () -> Unit, onLogout: () -> Unit
) {
    var profile   by remember { mutableStateOf(ProfileData()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("profile_cached", false)) {
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
            val email = sharedPref.getString("email", null) ?: run { isLoading = false; return@LaunchedEffect }
            apiInterface.getUser(email).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!.data
                        sharedPref.edit().apply {
                            putString("name", user.name); putString("email", user.email)
                            putString("height", user.height.toString()); putString("weight", user.weight.toString())
                            putString("age", user.age.toString()); putString("created_at", user.created_at)
                            putBoolean("profile_cached", true); apply()
                        }
                        profile = ProfileData(user.name, user.email, user.height.toString(), user.weight.toString(), user.age.toString(), formatDate(user.created_at))
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<UserResponse>, t: Throwable) { isLoading = false }
            })
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(BgBlack, BgDark, Color(0xFF0A0A0A))))) {
        // Blue glow — top right
        Box(modifier = Modifier.size(260.dp).offset(x = 180.dp, y = (-50).dp).blur(100.dp)
            .background(Brush.radialGradient(colors = listOf(BlueVivid.copy(alpha = 0.2f), Color.Transparent), radius = 450f), CircleShape))
        // Gray glow — mid left
        Box(modifier = Modifier.size(180.dp).offset(x = (-50).dp, y = 320.dp).blur(80.dp)
            .background(Brush.radialGradient(colors = listOf(GrayDark.copy(alpha = 0.3f), Color.Transparent), radius = 320f), CircleShape))

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ProfileHeader(profile = profile, isLoading = isLoading)
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(!isLoading, enter = fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { it / 2 })) {
                StatsRow(profile = profile)
            }
            Spacer(Modifier.height(28.dp))
            SectionLabel("Account")
            SettingsCard {
                SettingsRow(Icons.Filled.Notifications, listOf(BlueDark, BlueVivid), "Notifications", "Manage alerts & reminders", onClick = onNotifications)
                RowDivider()
                SettingsRow(Icons.Filled.HelpOutline, listOf(GrayDark, GrayMid), "Help & Support", "Get in touch with us", onClick = onHelp)
                RowDivider()
                SettingsRow(Icons.Filled.Info, listOf(Color(0xFF2A4060), Color(0xFF3A6090)), "About", "App info & version", onClick = onAbout)
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("Danger zone")
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
                    .border(1.dp, Brush.linearGradient(colors = listOf(DangerMid.copy(alpha = 0.5f), DangerDark.copy(alpha = 0.2f))), RoundedCornerShape(20.dp))
            ) {
                SettingsRow(Icons.Filled.Logout, listOf(DangerDark, DangerMid), "Log out", "You'll need to sign in again", labelColor = Color(0xFFFF6B6B), showChevron = false, onClick = onLogout)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileHeader(profile: ProfileData, isLoading: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.Start) {
        Text("MY PROFILE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar — blue gradient
            Box(
                modifier = Modifier.size(70.dp).clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(BlueDeep, BlueDark, BlueVivid)))
                    .border(2.dp, Brush.linearGradient(colors = listOf(BlueLight.copy(alpha = 0.6f), BlueDeep)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text(profile.name.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                if (isLoading) {
                    ShimmerBox(140.dp, 20.dp); Spacer(Modifier.height(6.dp)); ShimmerBox(100.dp, 14.dp)
                } else {
                    Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(profile.email, fontSize = 13.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (!isLoading && profile.memberSince.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
                    .border(1.dp, Brush.linearGradient(colors = listOf(BlueVivid.copy(alpha = 0.4f), SubtleBorder)), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Member since ${profile.memberSince}", fontSize = 11.sp, color = BlueLight, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(colors = listOf(BlueVivid, GrayDark.copy(alpha = 0.4f), Color.Transparent))))
    }
}

@Composable
fun StatsRow(profile: ProfileData) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(Modifier.weight(1f), profile.height, "cm", "Height", listOf(BlueDark, BlueVivid))
        StatCard(Modifier.weight(1f), profile.weight, "kg", "Weight", listOf(GrayDark, GrayMid))
        StatCard(Modifier.weight(1f), profile.age,    "yrs", "Age",   listOf(Color(0xFF2A4060), Color(0xFF3A6090)))
    }
}

@Composable
fun StatCard(modifier: Modifier, value: String, unit: String, label: String, accentColors: List<Color>) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
            .border(1.dp, Brush.linearGradient(colors = listOf(SubtleBorder, accentColors.last().copy(alpha = 0.25f))), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(6.dp).background(Brush.radialGradient(colors = accentColors.reversed()), CircleShape))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(2.dp))
                Text(unit, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, letterSpacing = 1.4.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
            .border(1.dp, Brush.linearGradient(colors = listOf(SubtleBorder, Color.Transparent)), RoundedCornerShape(20.dp))
    ) { Column(content = content) }
}

@Composable
fun SettingsRow(
    icon: ImageVector, iconGradient: List<Color>, label: String, subtitle: String,
    labelColor: Color = TextPrimary, showChevron: Boolean = true, onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors = iconGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
            if (showChevron) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayDark, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 72.dp).background(SubtleBorder))
}

@Composable
fun ShimmerBox(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmer.animateFloat(0.15f, 0.4f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    Box(modifier = Modifier.width(width).height(height).clip(RoundedCornerShape(6.dp)).background(GrayDark.copy(alpha = alpha)))
}

private fun formatDate(dateStr: String): String = try {
    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr)!!)
} catch (e: Exception) { "N/A" }