package com.example.run

import UserResponse
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

// ── Palette ───────────────────────────────────────────────────────────────────
private val SIBgWhite      = Color(0xFFFAF9FF)
val SIDeepBlack    = Color(0xFF0A0A0A)
val SIPureWhite    = Color(0xFFFFFFFF)
val SIAccentViolet = Color(0xFF9B8FD4)
val SIHoloPink     = Color(0xFFE8B4D8)
private val SIHoloMint     = Color(0xFFAEE8D8)
val SISubtleGrey   = Color(0xFFDDD8EE)
val SITextSecondary= Color(0xFF7A7490)
private val SIBgLavender   = Color(0xFFECE8F5)

class SignIn : AppCompatActivity() {

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

        setContent {
            MaterialTheme {
                SignInScreen(
                    onSignIn      = { email, password -> loginUser(email, password) },
                    onCreateAccount = {
                        startActivity(Intent(this, SignUp::class.java))
                    }
                )
            }
        }
    }

    private fun loginUser(email: String, password: String, onLoading: (Boolean) -> Unit = {}) {
        // loading state is managed inside the composable via callback
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SIGN-IN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SignInScreen(
    onSignIn:       (email: String, password: String) -> Unit,
    onCreateAccount: () -> Unit
) {
    // We manage all state + network here so the Activity stays thin
    val context     = androidx.compose.ui.platform.LocalContext.current
    val focusMgr    = LocalFocusManager.current

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    // Retrofit instance (remember so it's not recreated)
    val api = remember {
        Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }

    fun doLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMsg = "Enter email & password"; return
        }
        isLoading = true; errorMsg = null
        val req = SigninRequest(email.trim(), password.trim())
        api.signinUser(req).enqueue(object : Callback<SigninResponse> {
            override fun onResponse(call: Call<SigninResponse>, response: Response<SigninResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val userEmail = response.body()!!.email
                    val sp = context.getSharedPreferences("USER_SESSION", 0)
                    sp.edit().putString("email", userEmail).apply()
                    // fetch profile
                    api.getUser(userEmail).enqueue(object : Callback<UserResponse> {
                        override fun onResponse(c: Call<UserResponse>, r: Response<UserResponse>) {
                            isLoading = false
                            if (r.isSuccessful && r.body() != null) {
                                val u = r.body()!!.data
                                sp.edit().apply {
                                    putString("name",       u.name)
                                    putString("email",      u.email)
                                    putString("height",     u.height.toString())
                                    putString("weight",     u.weight.toString())
                                    putString("age",        u.age.toString())
                                    putString("created_at", u.created_at)
                                    putBoolean("profile_cached", true)
                                    apply()
                                }
                            }
                            context.startActivity(Intent(context, MainActivity::class.java))
                            (context as? SignIn)?.finish()
                        }
                        override fun onFailure(c: Call<UserResponse>, t: Throwable) {
                            isLoading = false
                            context.startActivity(Intent(context, MainActivity::class.java))
                            (context as? SignIn)?.finish()
                        }
                    })
                } else {
                    isLoading = false; errorMsg = "Invalid credentials"
                }
            }
            override fun onFailure(call: Call<SigninResponse>, t: Throwable) {
                isLoading = false; errorMsg = "Network error: ${t.message}"
            }
        })
    }

    Box(Modifier.fillMaxSize().background(SIDeepBlack)) {

        // ── Holo blobs ────────────────────────────────────────────────────────
        val inf   = rememberInfiniteTransition(label = "si_blobs")
        val drift by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
        Box(Modifier.size(300.dp).offset(x = (180 + drift * 10).dp, y = (-80 + drift * 15).dp).blur(90.dp)
            .background(Brush.radialGradient(listOf(SIAccentViolet.copy(0.5f), SIHoloPink.copy(0.25f), Color.Transparent)), CircleShape))
        Box(Modifier.size(240.dp).offset(x = (-80).dp, y = (500 - drift * 14).dp).blur(80.dp)
            .background(Brush.radialGradient(listOf(SIHoloMint.copy(0.35f), Color.Transparent)), CircleShape))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(80.dp))

            // ── Logo / Title ──────────────────────────────────────────────────
            Box(
                Modifier.size(64.dp)
                    .background(Brush.linearGradient(listOf(SIAccentViolet, SIHoloPink)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("CX", color = SIPureWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            Text("Welcome back", color = SIPureWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(6.dp))
            Text("Sign in to CaloriX", color = SITextSecondary, fontSize = 14.sp)

            Spacer(Modifier.height(40.dp))

            // ── Glass card ────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(SIPureWhite.copy(0.06f))
                    .border(1.dp, Brush.horizontalGradient(listOf(SIAccentViolet.copy(0.4f), SIHoloPink.copy(0.3f))), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Email
                    CaloriXTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = "Email address",
                        keyboardType  = KeyboardType.Email,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusMgr.moveFocus(FocusDirection.Down) }
                    )

                    // Password
                    CaloriXTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = "Password",
                        isPassword    = true,
                        showPassword  = showPass,
                        onTogglePass  = { showPass = !showPass },
                        imeAction     = ImeAction.Done,
                        onDone        = { focusMgr.clearFocus(); doLogin() }
                    )

                    // Error
                    errorMsg?.let {
                        Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(4.dp))

                    // Sign In button
                    CaloriXButton(
                        text      = "Sign In",
                        isLoading = isLoading,
                        onClick   = { focusMgr.clearFocus(); doLogin() }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Create account link ───────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Don't have an account?", color = SITextSecondary, fontSize = 13.sp)
                Text(
                    "Create one",
                    color      = SIAccentViolet,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { onCreateAccount() }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Shared text field composable ──────────────────────────────────────────────
@Composable
fun CaloriXTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    isPassword:    Boolean  = false,
    showPassword:  Boolean  = false,
    onTogglePass:  () -> Unit = {},
    keyboardType:  KeyboardType = KeyboardType.Text,
    imeAction:     ImeAction    = ImeAction.Next,
    onNext:        () -> Unit   = {},
    onDone:        () -> Unit   = {}
) {
    val visual = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = SITextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SIPureWhite.copy(0.08f))
                .border(1.dp, SISubtleGrey.copy(0.25f), RoundedCornerShape(14.dp))
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value              = value,
                    onValueChange      = onValueChange,
                    visualTransformation = visual,
                    singleLine         = true,
                    textStyle          = androidx.compose.ui.text.TextStyle(color = SIPureWhite, fontSize = 14.sp),
                    keyboardOptions    = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions    = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
                    modifier           = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                    decorationBox      = { inner ->
                        if (value.isEmpty()) Text(label, color = SITextSecondary.copy(0.5f), fontSize = 14.sp)
                        inner()
                    }
                )
                if (isPassword) {
                    Text(
                        if (showPassword) "Hide" else "Show",
                        color    = SIAccentViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 14.dp).clickable { onTogglePass() }
                    )
                }
            }
        }
    }
}

// ── Shared CTA button ─────────────────────────────────────────────────────────
@Composable
fun CaloriXButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "btnGlow")
    val glow by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "g")

    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow((12 * glow).dp, RoundedCornerShape(16.dp), ambientColor = SIAccentViolet, spotColor = SIHoloPink)
            .background(Brush.horizontalGradient(listOf(SIDeepBlack, Color(0xFF2A1F4A))), RoundedCornerShape(16.dp))
            .border(1.dp, Brush.horizontalGradient(listOf(SIAccentViolet.copy(0.6f), SIHoloPink.copy(0.4f))), RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = SIAccentViolet, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(text, color = SIPureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}