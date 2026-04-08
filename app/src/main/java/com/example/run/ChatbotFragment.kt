package com.example.run

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

// ─── Color Palette ─────────────────────────────────────────────────────────────
private val BgBlack      = Color(0xFF000000)
private val BgDark       = Color(0xFF111111)
private val BgCard       = Color(0xFF1C1C1C)
private val BgCardLight  = Color(0xFF2A2A2A)
private val GrayDark     = Color(0xFF3D3D3D)
private val GrayMid      = Color(0xFF6B6B6B)
private val GrayLight    = Color(0xFF9E9E9E)
private val BlueVivid    = Color(0xFF0083C9)
private val BlueDark     = Color(0xFF005A8C)
private val BlueDeep     = Color(0xFF003D63)
private val BlueLight    = Color(0xFF29A8E8)
private val SubtleBorder = Color(0xFF2C2C2C)
private val TextPrimary  = Color(0xFFF5F5F5)
private val TextMuted    = Color(0xFF8A8A8A)

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
            setContent { MaterialTheme { MiliChatScreen(apiInterface = apiInterface) } }
        }
    }

    private fun initRetrofit() {
        apiInterface = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }
}

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
                messages.add(ChatMessage("Connection error: ${t.message ?: "Check your internet"} 🔌", isBot = true))
                scope.launch { listState.animateScrollToItem(messages.size - 1) }
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgBlack, BgDark, Color(0xFF0A0A0A))))
            .imePadding()
    ) {
        // Electric blue atmospheric glow — bottom left
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = 480.dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BlueVivid.copy(alpha = 0.18f),
                            BlueDeep.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        radius = 550f
                    ), CircleShape
                )
        )
        // Gray glow — top right
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GrayDark.copy(alpha = 0.25f), Color.Transparent),
                        radius = 350f
                    ), CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            MiliHeader()
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && !isTyping) {
                    MiliEmptyState(onSuggestionClick = { sendMessage(it) })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp,
                            top = 20.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ✅ FIX: Removed AnimatedVisibility — renders bubbles directly
                        itemsIndexed(messages) { _, msg ->
                            if (msg.isBot) MiliBubble(msg.message) else UserBubble(msg.message)
                        }
                        if (isTyping) {
                            item { MiliTypingBubble() }
                        }
                    }
                }
            }
            MiliInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = { sendMessage(inputText) }
            )
        }
    }
}

@Composable
fun MiliHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
                .border(
                    1.dp,
                    Brush.linearGradient(colors = listOf(BlueVivid.copy(alpha = 0.6f), SubtleBorder)),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val scale by pulse.animateFloat(
                    initialValue = 0.7f,
                    targetValue  = 1.3f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "scale"
                )
                Box(
                    modifier = Modifier
                        .size((6 * scale).dp)
                        .background(
                            Brush.radialGradient(colors = listOf(BlueLight, BlueVivid)),
                            CircleShape
                        )
                )
                Text(
                    "Mili · Online",
                    color = GrayLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Hey there 👋", fontSize = 14.sp, color = TextMuted)
        Spacer(Modifier.height(4.dp))
        Text(
            "What's on your\nfitness mind?",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 36.sp,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BlueVivid, GrayDark.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun MiliEmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Quick questions",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(colors = listOf(SubtleBorder, BlueVivid.copy(alpha = 0.3f)))
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(colors = listOf(BlueDark, BlueVivid)))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("→", fontSize = 13.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(BlueDeep, BlueDark, BlueVivid),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(colors = listOf(BlueLight.copy(alpha = 0.4f), BlueDeep)),
                    RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 13.dp)
        ) {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MiliBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.linearGradient(colors = listOf(GrayDark, GrayMid)),
                    CircleShape
                )
                .border(
                    1.dp,
                    Brush.linearGradient(colors = listOf(BlueVivid.copy(alpha = 0.5f), GrayDark)),
                    CircleShape
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
                .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
                .border(
                    1.dp,
                    Brush.linearGradient(colors = listOf(SubtleBorder, Color.Transparent)),
                    RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(colors = listOf(BlueVivid, BlueDark)))
            )
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
fun MiliTypingBubble() {
    Row(verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.linearGradient(colors = listOf(GrayDark, GrayMid)),
                    CircleShape
                )
                .border(
                    1.dp,
                    Brush.linearGradient(colors = listOf(BlueVivid.copy(alpha = 0.5f), GrayDark)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
                .border(
                    1.dp,
                    Brush.linearGradient(colors = listOf(SubtleBorder, Color.Transparent)),
                    RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
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
        initialValue = 0f,
        targetValue  = -7f,
        animationSpec = infiniteRepeatable(
            tween(420, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(delayMs)
        ),
        label = "bounce"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .offset(y = offsetY.dp)
            .background(
                Brush.radialGradient(colors = listOf(BlueLight, BlueVivid)),
                CircleShape
            )
    )
}

@Composable
fun MiliInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, BgBlack.copy(alpha = 0.98f))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(colors = listOf(BgCard, BgCardLight)))
                    .border(
                        1.dp,
                        Brush.linearGradient(colors = listOf(GrayDark, BlueVivid.copy(alpha = 0.4f))),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text("Ask Mili anything…", color = TextMuted, fontSize = 14.sp)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = BlueVivid,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary
                    ),
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier  = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend(); keyboard?.hide() }),
                    singleLine = false,
                    maxLines   = 4
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BlueDark, BlueVivid, BlueLight),
                            start  = Offset(0f, 50f),
                            end    = Offset(50f, 0f)
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(colors = listOf(BlueLight.copy(alpha = 0.5f), BlueDark)),
                        CircleShape
                    )
                    .clickable { onSend(); keyboard?.hide() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = "Send",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}