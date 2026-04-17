package com.example.run

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ── Palette ───────────────────────────────────────────────────────────────────
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

private val ChatBlobBrush = Brush.radialGradient(
    listOf(HoloPink.copy(0.20f), HoloMint.copy(0.10f), Color.Transparent),
    radius = 500f
)

private val suggestions = listOf(
    "How much water did I drink yesterday?",
    "Tell me about my workouts",
    "Create a custom workout plan for me"
)

// ─────────────────────────────────────────────────────────────────────────────
//  FRAGMENT
// ─────────────────────────────────────────────────────────────────────────────
class ChatbotFragment : Fragment() {

    private lateinit var apiInterface: ApiInterface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // ── FIX: Custom OkHttpClient with 90-second timeouts ─────────────────
        // Render free tier cold starts + Gemini API call can take 30-60 seconds.
        // Retrofit's default timeout is 10 seconds — causing the 502/timeout error.
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)   // time to establish connection
            .readTimeout(90, TimeUnit.SECONDS)       // time to wait for response body
            .writeTimeout(30, TimeUnit.SECONDS)      // time to send request body
            .build()

        apiInterface = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .client(okHttpClient)                    // ← attach custom client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    MiliChatScreen(apiInterface = apiInterface)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROOT CHAT SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MiliChatScreen(apiInterface: ApiInterface) {

    val context = LocalContext.current

    val email = remember {
        context
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
            .getString("email", "") ?: ""
    }

    val messages  = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isTyping  by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // ── Auto-scroll ───────────────────────────────────────────────────────────
    val itemCount = messages.size + if (isTyping) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    // ── Send message ──────────────────────────────────────────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(text, isBot = false))
        inputText = ""
        isTyping  = true

        apiInterface.getFitnessResponse(prompt = text, email = email)
            .enqueue(object : Callback<MyData> {
                override fun onResponse(call: Call<MyData>, response: Response<MyData>) {
                    isTyping = false
                    val reply = if (response.isSuccessful && response.body() != null)
                        response.body()?.response ?: "No response"
                    else
                        "Error ${response.code()} — please try again"
                    messages.add(ChatMessage(reply, isBot = true))
                }

                override fun onFailure(call: Call<MyData>, t: Throwable) {
                    isTyping = false
                    // Show a friendly error instead of raw exception message
                    val errorMsg = when {
                        t.message?.contains("timeout", ignoreCase = true) == true ->
                            "Mili is taking too long to respond. Please try again 🙏"
                        t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                            "No internet connection. Please check your network 📶"
                        else ->
                            "Something went wrong: ${t.message}"
                    }
                    messages.add(ChatMessage(errorMsg, isBot = true))
                }
            })
    }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        Box(
            Modifier
                .size(260.dp)
                .offset(x = 180.dp, y = (-50).dp)
                .background(ChatBlobBrush, CircleShape)
        )

        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826))))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Box(
                    Modifier.size(130.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp)
                        .background(
                            Brush.radialGradient(listOf(AccentViolet.copy(0.22f), Color.Transparent), radius = 260f),
                            CircleShape
                        )
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp)
                                .background(Brush.sweepGradient(listOf(HoloPink, AccentViolet, HoloMint, HoloPink)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(37.dp).background(DeepBlack, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("M", color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text("Mili", color = PureWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val pulse = rememberInfiniteTransition(label = "miliDot")
                                val sc by pulse.animateFloat(
                                    0.7f, 1.3f,
                                    infiniteRepeatable(tween(800), RepeatMode.Reverse),
                                    label = "sc"
                                )
                                Box(Modifier.size((6 * sc).dp).background(HoloMint, CircleShape))
                                Spacer(Modifier.width(5.dp))
                                Text("Online now", color = Color(0xFF8A86A0), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        "Your personal\nfitness advisor.",
                        color         = PureWhite,
                        fontSize      = 26.sp,
                        fontWeight    = FontWeight.Bold,
                        lineHeight    = 33.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // ── Chat area ─────────────────────────────────────────────────────
            Box(Modifier.weight(1f)) {
                if (messages.isEmpty() && !isTyping) {
                    MiliEmptyState { sendMessage(it) }
                } else {
                    LazyColumn(
                        state          = listState,
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start  = 20.dp,
                            end    = 20.dp,
                            top    = 16.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = messages,
                            key   = { i, _ -> i }
                        ) { _, msg ->
                            if (msg.isBot) MiliBotBubble(msg.message)
                            else           MiliUserBubble(msg.message)
                        }

                        if (isTyping) {
                            item(key = "typing") { MiliTypingBubble() }
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            MiliInputBar(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = { sendMessage(inputText) }
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MiliEmptyState(onSuggestion: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            "Try asking",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextSecondary,
            letterSpacing = 1.3.sp
        )
        Spacer(Modifier.height(14.dp))
        suggestions.forEach { s ->
            Surface(
                onClick         = { onSuggestion(s) },
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(16.dp),
                color           = PureWhite,
                border          = BorderStroke(1.dp, SubtleGrey),
                shadowElevation = 3.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        s, fontSize = 13.sp, color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.weight(1f)
                    )
                    Box(
                        Modifier.clip(CircleShape).background(BgLavender)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("→", fontSize = 13.sp, color = AccentViolet)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BUBBLES
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MiliUserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .widthIn(max = 270.dp)
                .background(DeepBlack, RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                .padding(horizontal = 18.dp, vertical = 13.dp)
        ) {
            Text(text, color = PureWhite, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun MiliBotBubble(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Box(
            Modifier.size(30.dp)
                .background(Brush.linearGradient(listOf(AccentViolet, HoloPink)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Surface(
            modifier        = Modifier.widthIn(max = 270.dp),
            shape           = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color           = PureWhite,
            border          = BorderStroke(1.dp, SubtleGrey),
            shadowElevation = 4.dp
        ) {
            Text(
                text,
                Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                color = TextPrimary, fontSize = 14.sp, lineHeight = 21.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TYPING INDICATOR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MiliTypingBubble() {
    Row(verticalAlignment = Alignment.Bottom) {
        Box(
            Modifier.size(30.dp)
                .background(Brush.linearGradient(listOf(AccentViolet, HoloPink)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Surface(
            shape           = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color           = PureWhite,
            border          = BorderStroke(1.dp, SubtleGrey),
            shadowElevation = 4.dp
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val tr = rememberInfiniteTransition(label = "dots")
                val y0 by tr.animateFloat(0f, -7f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(0)),   label = "y0")
                val y1 by tr.animateFloat(0f, -7f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(160)), label = "y1")
                val y2 by tr.animateFloat(0f, -7f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(320)), label = "y2")
                Box(Modifier.size(7.dp).offset(y = y0.dp).background(AccentViolet, CircleShape))
                Box(Modifier.size(7.dp).offset(y = y1.dp).background(AccentViolet, CircleShape))
                Box(Modifier.size(7.dp).offset(y = y2.dp).background(AccentViolet, CircleShape))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  INPUT BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MiliInputBar(
    value:         String,
    onValueChange: (String) -> Unit,
    onSend:        () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Surface(
        Modifier.fillMaxWidth(),
        color           = BgWhite,
        shadowElevation = 16.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                Modifier.weight(1f),
                RoundedCornerShape(20.dp),
                PureWhite,
                border          = BorderStroke(1.dp, SubtleGrey),
                shadowElevation = 2.dp
            ) {
                TextField(
                    value         = value,
                    onValueChange = onValueChange,
                    placeholder   = {
                        Text("Ask Mili anything...", color = TextSecondary, fontSize = 14.sp)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = AccentViolet,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary
                    ),
                    textStyle       = TextStyle(fontSize = 14.sp),
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend(); keyboard?.hide() }),
                    singleLine      = false,
                    maxLines        = 4
                )
            }

            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(DeepBlack)
                    .clickable { onSend(); keyboard?.hide() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = "Send",
                    tint     = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}