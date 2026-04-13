package com.example.run

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class SignUp : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            MaterialTheme {
                SignupScreen(
                    onSuccess = {
                        startActivity(Intent(this, SignIn::class.java))
                        finish()
                    },
                    onGoToSignIn = {
                        startActivity(Intent(this, SignIn::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SIGNUP SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SignupScreen(onSuccess: () -> Unit, onGoToSignIn: () -> Unit) {

    val context  = LocalContext.current
    val focusMgr = LocalFocusManager.current

    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var showPass  by remember { mutableStateOf(false) }
    var age       by remember { mutableStateOf("") }
    var height    by remember { mutableStateOf("") }
    var weight    by remember { mutableStateOf("") }
    var gender    by remember { mutableStateOf("") }
    var goal      by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    var showGenderMenu by remember { mutableStateOf(false) }
    var showGoalMenu   by remember { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    val goalOptions   = listOf("Fat Loss", "Muscle Gain", "Maintain Fitness", "Improve Stamina")

    val api = remember {
        Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }

    fun doSignup() {
        if (name.isBlank() || email.isBlank() || password.isBlank() ||
            age.isBlank() || gender.isBlank() || height.isBlank() || weight.isBlank()) {
            errorMsg = "Please fill all fields"; return
        }
        if (!termsAccepted) { errorMsg = "Accept Terms & Conditions"; return }
        isLoading = true; errorMsg = null

        val req = UserSignupRequest(
            name     = name.trim(),
            email    = email.trim(),
            password = password.trim(),
            age      = age.toIntOrNull() ?: 0,
            gender   = gender,
            height   = height.toDoubleOrNull() ?: 0.0,
            weight   = weight.toDoubleOrNull() ?: 0.0
        )

        api.signupUser(req).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    onSuccess()
                } else {
                    errorMsg = "Signup failed: ${response.code()}"
                }
            }
            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                isLoading = false; errorMsg = "Network error: ${t.message}"
            }
        })
    }

    Box(Modifier.fillMaxSize().background(SIDeepBlack)) {

        // Holo blobs
        val inf   = rememberInfiniteTransition(label = "su_blobs")
        val drift by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
        Box(Modifier.size(300.dp).offset(x = (160 + drift * 10).dp, y = (-60).dp).blur(90.dp)
            .background(Brush.radialGradient(listOf(SIHoloPink.copy(0.45f), SIAccentViolet.copy(0.2f), Color.Transparent)), CircleShape))
        Box(Modifier.size(220.dp).offset(x = (-60).dp, y = (600 - drift * 12).dp).blur(80.dp)
            .background(Brush.radialGradient(listOf(SIAccentViolet.copy(0.4f), Color.Transparent)), CircleShape))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Title
            Box(
                Modifier.size(64.dp)
                    .background(Brush.linearGradient(listOf(SIAccentViolet, SIHoloPink)), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("CX", color = SIPureWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(16.dp))
            Text("Create Account", color = SIPureWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(4.dp))
            Text("Join CaloriX today", color = SITextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(32.dp))

            // ── Section: Basic Info ───────────────────────────────────────────
            SignupSectionCard(title = "Basic Info") {
                CaloriXTextField(value = name,     onValueChange = { name = it },     label = "Full Name",       imeAction = ImeAction.Next, onNext = { focusMgr.moveFocus(FocusDirection.Down) })
                CaloriXTextField(value = email,    onValueChange = { email = it },    label = "Email address",   keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, onNext = { focusMgr.moveFocus(FocusDirection.Down) })
                CaloriXTextField(value = password, onValueChange = { password = it }, label = "Password",        isPassword = true, showPassword = showPass, onTogglePass = { showPass = !showPass }, imeAction = ImeAction.Next, onNext = { focusMgr.moveFocus(FocusDirection.Down) })
                CaloriXTextField(value = age,      onValueChange = { age = it },      label = "Age",             keyboardType = KeyboardType.Number, imeAction = ImeAction.Next, onNext = { focusMgr.moveFocus(FocusDirection.Down) })
            }

            Spacer(Modifier.height(14.dp))

            // ── Section: Body Stats ───────────────────────────────────────────
            SignupSectionCard(title = "Body Stats") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        CaloriXTextField(value = height, onValueChange = { height = it }, label = "Height (cm)", keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next, onNext = { focusMgr.moveFocus(FocusDirection.Down) })
                    }
                    Box(Modifier.weight(1f)) {
                        CaloriXTextField(value = weight, onValueChange = { weight = it }, label = "Weight (kg)", keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done, onDone = { focusMgr.clearFocus() })
                    }
                }

                // Gender dropdown
                SignupDropdown(
                    label       = "Gender",
                    selected    = gender,
                    expanded    = showGenderMenu,
                    options     = genderOptions,
                    onExpand    = { showGenderMenu = true },
                    onDismiss   = { showGenderMenu = false },
                    onSelect    = { gender = it; showGenderMenu = false }
                )

                // Goal dropdown
                SignupDropdown(
                    label       = "Fitness Goal",
                    selected    = goal,
                    expanded    = showGoalMenu,
                    options     = goalOptions,
                    onExpand    = { showGoalMenu = true },
                    onDismiss   = { showGoalMenu = false },
                    onSelect    = { goal = it; showGoalMenu = false }
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Terms ─────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (termsAccepted) SIAccentViolet else SIPureWhite.copy(0.08f))
                        .border(1.dp, if (termsAccepted) SIAccentViolet else SISubtleGrey.copy(0.3f), RoundedCornerShape(6.dp))
                        .clickable { termsAccepted = !termsAccepted },
                    contentAlignment = Alignment.Center
                ) {
                    if (termsAccepted) Text("✓", color = SIPureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("I agree to Terms & Conditions", color = SITextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))

            errorMsg?.let {
                Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))

            CaloriXButton(text = "Create Account", isLoading = isLoading, onClick = { focusMgr.clearFocus(); doSignup() })

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Already have an account?", color = SITextSecondary, fontSize = 13.sp)
                Text("Sign In", color = SIAccentViolet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onGoToSignIn() })
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Section card ──────────────────────────────────────────────────────────────
@Composable
private fun SignupSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(title, color = SITextSecondary.copy(0.80f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SIPureWhite.copy(0.06f))
                .border(1.dp, Brush.horizontalGradient(listOf(SIAccentViolet.copy(0.35f), SIHoloPink.copy(0.25f))), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
            }
        }
    }
}

// ── Dropdown ──────────────────────────────────────────────────────────────────
@Composable
private fun SignupDropdown(
    label:     String,
    selected:  String,
    expanded:  Boolean,
    options:   List<String>,
    onExpand:  () -> Unit,
    onDismiss: () -> Unit,
    onSelect:  (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = SITextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SIPureWhite.copy(0.08f))
                    .border(1.dp, SISubtleGrey.copy(0.25f), RoundedCornerShape(14.dp))
                    .clickable { onExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    if (selected.isEmpty()) "Select $label" else selected,
                    color    = if (selected.isEmpty()) SITextSecondary.copy(0.5f) else SIPureWhite,
                    fontSize = 14.sp
                )
                Text("▾", color = SIAccentViolet, fontSize = 14.sp)
            }
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = onDismiss,
                modifier         = Modifier.background(Color(0xFF1E1830))
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text    = { Text(opt, color = SIPureWhite, fontSize = 13.sp) },
                        onClick = { onSelect(opt) }
                    )
                }
            }
        }
    }
}