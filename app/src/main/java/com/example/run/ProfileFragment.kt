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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

private val BgWhite      = Color(0xFFFAF9FF)
private val BgLavender   = Color(0xFFECE8F5)
private val DeepBlack    = Color(0xFF0A0A0A)
private val PureWhite    = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF9B8FD4)
private val HoloPink     = Color(0xFFE8B4D8)
private val HoloMint     = Color(0xFFAEE8D8)
private val SubtleGrey   = Color(0xFFDDD8EE)
private val TextPrimary  = Color(0xFF0A0A0A)
private val TextMuted    = Color(0xFF7A7490)
private val DangerRed    = Color(0xFFD97B6C)

// FIX: Static blob brush — was blur(70.dp) which caused GPU re-render every frame
private val ProfileBlobBrush = Brush.radialGradient(
    listOf(HoloPink.copy(0.20f), HoloMint.copy(0.10f), Color.Transparent),
    radius = 520f
)

data class ProfileData(
    val name: String = "", val email: String = "", val height: String = "",
    val weight: String = "", val age: String = "", val memberSince: String = ""
)

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
                            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("dhokaneprasad6@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "CaloriX App Support")
                                putExtra(Intent.EXTRA_TEXT, "Hello Prasad,\n\nI need help with CaloriX app.")
                            })
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
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiInterface::class.java)
    }

    private fun logoutUser() {
        requireContext().getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(requireContext(), SignIn::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ProfileScreen(
    apiInterface: ApiInterface, context: Context,
    onNotifications: () -> Unit, onHelp: () -> Unit, onAbout: () -> Unit, onLogout: () -> Unit
) {
    var profile   by remember { mutableStateOf(ProfileData()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val sp       = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        val isCached = sp.getBoolean("profile_cached", false)
        if (isCached) {
            profile = ProfileData(
                name        = sp.getString("name", "N/A") ?: "N/A",
                email       = sp.getString("email", "N/A") ?: "N/A",
                height      = sp.getString("height", "0") ?: "0",
                weight      = sp.getString("weight", "0") ?: "0",
                age         = sp.getString("age", "0") ?: "0",
                memberSince = formatProfileDate(sp.getString("created_at", "") ?: "")
            )
            isLoading = false
        } else {
            val email = sp.getString("email", null) ?: run { isLoading = false; return@LaunchedEffect }
            apiInterface.getUser(email).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!.data
                        sp.edit().apply {
                            putString("name", user.name); putString("email", user.email)
                            putString("height", user.height.toString()); putString("weight", user.weight.toString())
                            putString("age", user.age.toString()); putString("created_at", user.created_at)
                            putBoolean("profile_cached", true); apply()
                        }
                        profile = ProfileData(user.name, user.email, user.height.toString(), user.weight.toString(), user.age.toString(), formatProfileDate(user.created_at))
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<UserResponse>, t: Throwable) { isLoading = false }
            })
        }
    }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        // FIX: Static blob — was blur(70.dp). Same visual, zero GPU cost.
        Box(Modifier.size(220.dp).offset(x = 200.dp, y = (-40).dp).background(ProfileBlobBrush, CircleShape))

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Dark header
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826))))
                    .padding(horizontal = 24.dp, vertical = 30.dp)
            ) {
                // FIX: removed blur() corner blob — static radial gradient instead
                Box(
                    Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-30).dp)
                        .background(Brush.radialGradient(listOf(AccentViolet.copy(0.22f), HoloPink.copy(0.10f), Color.Transparent), radius = 320f), CircleShape)
                )
                Column {
                    Text("MY PROFILE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A86A0), letterSpacing = 1.6.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(68.dp).background(Brush.sweepGradient(listOf(HoloPink, AccentViolet, HoloMint, HoloPink)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(60.dp).background(Color(0xFF1C1826), CircleShape), contentAlignment = Alignment.Center) {
                                if (isLoading) CircularProgressIndicator(color = AccentViolet, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                else Text(profile.name.take(1).uppercase(), color = PureWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column {
                            if (isLoading) {
                                ProfileShimmerBox(140.dp, 18.dp); Spacer(Modifier.height(8.dp)); ProfileShimmerBox(100.dp, 13.dp)
                            } else {
                                Text(profile.name, color = PureWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(profile.email, color = Color(0xFF8A86A0), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (!isLoading && profile.memberSince.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(BgLavender.copy(0.15f))
                                .border(BorderStroke(1.dp, BgLavender.copy(0.3f)), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Member since ${profile.memberSince}", fontSize = 11.sp, color = BgLavender, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(!isLoading, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileStatCard(Modifier.weight(1f), profile.height, "cm", "Height", HoloPink)
                    ProfileStatCard(Modifier.weight(1f), profile.weight, "kg", "Weight", AccentViolet)
                    ProfileStatCard(Modifier.weight(1f), profile.age,    "yrs","Age",    HoloMint)
                }
            }

            Spacer(Modifier.height(28.dp))

            ProfileSectionLabel("Account")
            ProfileSettingsCard {
                ProfileSettingsRow(Icons.Filled.Notifications, AccentViolet, "Notifications", "Manage alerts & reminders", onClick = onNotifications)
                ProfileRowDivider()
                ProfileSettingsRow(Icons.Filled.HelpOutline,  HoloPink,     "Help & Support","Get in touch with us",      onClick = onHelp)
                ProfileRowDivider()
                ProfileSettingsRow(Icons.Filled.Info,          HoloMint,    "About",          "App info & version",        onClick = onAbout)
            }

            Spacer(Modifier.height(16.dp))
            ProfileSectionLabel("Danger zone")
            Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), RoundedCornerShape(20.dp), PureWhite, border = BorderStroke(1.dp, DangerRed.copy(0.35f)), shadowElevation = 3.dp) {
                ProfileSettingsRow(Icons.Filled.Logout, DangerRed, "Log out", "You'll need to sign in again", labelColor = DangerRed, showChevron = false, onClick = onLogout)
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun ProfileStatCard(modifier: Modifier, value: String, unit: String, label: String, accent: Color) {
    Surface(modifier, RoundedCornerShape(20.dp), PureWhite, border = BorderStroke(1.dp, SubtleGrey), shadowElevation = 4.dp) {
        Column(Modifier.padding(vertical = 18.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(6.dp).background(accent, CircleShape))
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
fun ProfileSectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, letterSpacing = 1.4.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

@Composable
fun ProfileSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), RoundedCornerShape(20.dp), PureWhite, border = BorderStroke(1.dp, SubtleGrey), shadowElevation = 3.dp) { Column(content = content) }
}

@Composable
fun ProfileSettingsRow(icon: ImageVector, iconTint: Color, label: String, subtitle: String, labelColor: Color = TextPrimary, showChevron: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconTint.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
            if (showChevron) Icon(Icons.Filled.ChevronRight, null, tint = SubtleGrey, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileRowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 72.dp).background(SubtleGrey))
}

@Composable
fun ProfileShimmerBox(width: Dp, height: Dp) {
    // FIX: single InfiniteTransition for both shimmer boxes (was 2 separate ones before)
    val shimmer = rememberInfiniteTransition(label = "pShimmer")
    val alpha by shimmer.animateFloat(0.2f, 0.5f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(6.dp)).background(Color(0xFF4A4460).copy(alpha = alpha)))
}

private fun formatProfileDate(dateStr: String): String = try {
    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr)!!)
} catch (e: Exception) { "N/A" }