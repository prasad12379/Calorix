package com.example.run

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ─── Color Palette (HD Gradient Schema) ────────────────────────────────────────
// Base tones from: Charcoal #272F2D | Sienna #6E2B1E | Steel Blue #8FAEC0
private val BgDeep        = Color(0xFF1C2220)   // deeper charcoal bg
private val BgDark        = Color(0xFF272F2D)   // primary charcoal
private val BgCard        = Color(0xFF2E3836)   // card surface (lighter charcoal)
private val BgCardLight   = Color(0xFF364240)   // elevated card
private val SiennaDeep    = Color(0xFF4A1C12)   // deep sienna
private val SiennaMid     = Color(0xFF6E2B1E)   // primary sienna accent
private val SiennaLight   = Color(0xFF9B4030)   // lighter sienna
private val SiennaGlow    = Color(0xFFBF6050)   // warm sienna glow
private val SteelDeep     = Color(0xFF5A7A8A)   // deep steel blue
private val SteelMid      = Color(0xFF8FAEC0)   // primary steel blue
private val SteelLight    = Color(0xFFB0CCDA)   // lighter steel
private val SteelGlow     = Color(0xFFCCDEE8)   // soft steel glow
private val SubtleBorder  = Color(0xFF3D4D4A)   // subtle dark border
private val TextPrimary   = Color(0xFFEEEEEC)   // off-white primary text
private val TextMuted     = Color(0xFF8A9E9A)   // muted teal-gray text

// ─── Suggestion Data ───────────────────────────────────────────────────────────
private val suggestions = listOf(
    "🏃 How do I improve my running speed?",
    "🥗 What should I eat after a workout?",
    "📋 Create a custom workout plan for me"
)

class ChatbotFragment : Fragment() {

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
                    MiliChatScreen(apiInterface = apiInterface)
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
fun MiliChatScreen(apiInterface: ApiInterface) {
    val messages  = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isTyping  by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    fun scrollToBottom() {
        scope.launch {
            val target = listState.layoutInfo.totalItemsCount
            if (target > 0) listState.animateScrollToItem(target - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(text, isBot = false))
        inputText = ""
        isTyping  = true
        scrollToBottom()

        apiInterface.getFitnessResponse(text).enqueue(object : Callback<MyData> {
            override fun onResponse(call: Call<MyData>, response: Response<MyData>) {
                isTyping = false
                val reply = if (response.isSuccessful && response.body() != null)
                    response.body()?.response ?: "No response from server"
                else "Sorry, I couldn't get a response. Error ${response.code()}"
                messages.add(ChatMessage(reply, isBot = true))
                scope.launch { listState.animateScrollToItem(messages.size - 1) }
            }
            override fun onFailure(call: Call<MyData>, t: Throwable) {
                isTyping = false
                messages.add(ChatMessage(
                    "Connection error: ${t.message ?: "Check your internet"} 🔌",
                    isBot = true
                ))
                scope.launch { listState.animateScrollToItem(messages.size - 1) }
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
            .imePadding()
    ) {
        // ── HD Sienna blob — bottom left ───────────────────────────────────────
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = 480.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SiennaMid.copy(alpha = 0.35f),
                            SiennaDeep.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 500f
                    ),
                    shape = CircleShape
                )
        )
        // ── HD Steel blob — top right ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SteelMid.copy(alpha = 0.2f),
                            SteelDeep.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 420f
                    ),
                    shape = CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            MiliHeader()

            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && !isTyping) {
                    MiliEmptyState(onSuggestionClick = { sendMessage(it) })
                } else {
                    LazyColumn(
                        state    = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start  = 20.dp,
                            end    = 20.dp,
                            top    = 20.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(messages) { _, msg ->
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn(tween(300)) + slideInVertically(
                                    tween(300), initialOffsetY = { it / 3 }
                                )
                            ) {
                                if (msg.isBot) MiliBubble(msg.message)
                                else UserBubble(msg.message)
                            }
                        }
                        if (isTyping) {
                            item { MiliTypingBubble() }
                        }
                    }
                }
            }

            MiliInputBar(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = { sendMessage(inputText) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HEADER
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MiliHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BgDeep,
                        BgDeep.copy(alpha = 0f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Surface(
            shape           = RoundedCornerShape(50),
            color           = Color.Transparent,
            shadowElevation = 0.dp,
            modifier        = Modifier
                .wrapContentWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BgCard, BgCardLight)
                    ),
                    shape = RoundedCornerShape(50)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(SubtleBorder, SteelDeep.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(50)
                )
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val scale by pulse.animateFloat(
                    initialValue  = 0.7f,
                    targetValue   = 1.2f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label         = "scale"
                )
                // Pulsing dot with steel blue gradient
                Box(
                    modifier = Modifier
                        .size((6 * scale).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(SteelLight, SteelMid)
                            ),
                            CircleShape
                        )
                )
                Text(
                    text          = "Mili · Online",
                    color         = TextPrimary,
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text       = "Hey there 👋",
            fontSize   = 14.sp,
            color      = TextMuted,
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text          = "What's on your\nfitness mind?",
            fontSize      = 30.sp,
            fontWeight    = FontWeight.Bold,
            color         = TextPrimary,
            lineHeight    = 36.sp,
            letterSpacing = (-0.8).sp
        )

        Spacer(Modifier.height(16.dp))

        // HD gradient divider — sienna to steel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SiennaMid,
                            SteelMid.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MiliEmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text          = "Quick questions",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextMuted,
            letterSpacing = 1.4.sp
        )
        suggestions.forEach { text ->
            MiliSuggestionChip(text = text, onClick = { onSuggestionClick(text) })
        }
    }
}

@Composable
fun MiliSuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick         = onClick,
        modifier        = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(BgCard, BgCardLight)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.Transparent,
        border          = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(SubtleBorder, SteelDeep.copy(alpha = 0.2f))
            )
        ),
        shadowElevation = 4.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = text,
                fontSize   = 13.sp,
                color      = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f)
            )
            // Arrow badge with sienna gradient
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SiennaMid, SiennaLight)
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "→", fontSize = 13.sp, color = TextPrimary)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BUBBLES
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun UserBubble(text: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SiennaDeep, SiennaMid, SiennaLight),
                        start  = Offset(0f, 0f),
                        end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(SiennaGlow.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 13.dp)
        ) {
            Text(
                text       = text,
                color      = TextPrimary,
                fontSize   = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MiliBubble(text: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar with steel gradient
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(SteelDeep, SteelMid)
                    ),
                    CircleShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(SteelLight.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Row(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
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
                    shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
                )
        ) {
            // Steel blue accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SteelMid, SteelDeep)
                        )
                    )
            )
            Text(
                text      = text,
                modifier  = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                color     = TextPrimary,
                fontSize  = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TYPING INDICATOR
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MiliTypingBubble() {
    Row(verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(SteelDeep, SteelMid)
                    ),
                    CircleShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(SteelLight.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
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
                    shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
                )
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { i -> BouncingDot(delayMs = i * 160) }
            }
        }
    }
}

@Composable
fun BouncingDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "dot$delayMs")
    val offsetY by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = -7f,
        animationSpec = infiniteRepeatable(
            animation          = tween(420, easing = FastOutSlowInEasing),
            repeatMode         = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs)
        ),
        label = "bounce"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .offset(y = offsetY.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(SiennaGlow, SiennaMid)
                ),
                CircleShape
            )
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  INPUT BAR
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MiliInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BgDeep.copy(alpha = 0.95f),
                        BgDeep
                    )
                )
            )
            .border(
                width = 0.dp,
                color = Color.Transparent
            )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text field with gradient border
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BgCard, BgCardLight)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SubtleBorder, SteelDeep.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                TextField(
                    value         = value,
                    onValueChange = onValueChange,
                    placeholder   = {
                        Text(
                            "Ask Mili anything…",
                            color    = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = SiennaGlow,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary
                    ),
                    textStyle       = TextStyle(fontSize = 14.sp),
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { onSend(); keyboard?.hide() }
                    ),
                    singleLine = false,
                    maxLines   = 4
                )
            }

            // Send button — sienna gradient with glow border
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SiennaDeep, SiennaMid, SiennaLight),
                            start  = Offset(0f, 0f),
                            end    = Offset(50f, 50f)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SiennaGlow.copy(alpha = 0.6f), SiennaDeep)
                        ),
                        shape = CircleShape
                    )
                    .clickable { onSend(); keyboard?.hide() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Filled.ArrowUpward,
                    contentDescription = "Send",
                    tint               = TextPrimary,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}