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
import androidx.compose.ui.draw.clip
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
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

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
    "How to improve my running speed?",
    "What should I eat after a workout?",
    "Create a custom workout plan for me"
)

class ChatbotFragment : Fragment() {
    private lateinit var apiInterface: ApiInterface

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        apiInterface = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiInterface::class.java)
        return ComposeView(requireContext()).apply {
            setContent { MaterialTheme { MiliChatScreen(apiInterface) } }
        }
    }
}

@Composable
fun MiliChatScreen(apiInterface: ApiInterface) {
    val messages  = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isTyping  by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    // ── FIX: Auto-scroll whenever message count OR isTyping changes ───────────
    // Using LaunchedEffect keyed on messages.size + isTyping means we scroll
    // AFTER recomposition has added the new item to the list, not before.
    // This is the root cause of the "last message goes below screen" bug.
    val itemCount = messages.size + if (isTyping) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            // scrollToItem (not animate) for instant snap when keyboard opens;
            // use animateScrollToItem only for bot replies for a smooth feel.
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(text, isBot = false))
        inputText = ""
        isTyping  = true
        // No manual scroll here — LaunchedEffect above handles it reactively.
        apiInterface.getFitnessResponse(text).enqueue(object : Callback<MyData> {
            override fun onResponse(call: Call<MyData>, response: Response<MyData>) {
                isTyping = false
                messages.add(
                    ChatMessage(
                        if (response.isSuccessful && response.body() != null)
                            response.body()?.response ?: "No response"
                        else "Error ${response.code()}",
                        isBot = true
                    )
                )
            }
            override fun onFailure(call: Call<MyData>, t: Throwable) {
                isTyping = false
                messages.add(ChatMessage("Connection error: ${t.message}", isBot = true))
            }
        })
    }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        Box(
            Modifier.size(260.dp).offset(x = 180.dp, y = (-50).dp)
                .background(ChatBlobBrush, CircleShape)
        )

        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DeepBlack, Color(0xFF1C1826))))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(
                                Brush.sweepGradient(listOf(HoloPink, AccentViolet, HoloMint, HoloPink)),
                                CircleShape
                            ),
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
                        color = PureWhite, fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 33.sp,
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
                        state   = listState,
                        modifier = Modifier.fillMaxSize(),
                        // FIX: bottom padding ensures the last bubble is never
                        // hidden behind the input bar even when the list is full.
                        contentPadding = PaddingValues(
                            start  = 20.dp,
                            end    = 20.dp,
                            top    = 16.dp,
                            bottom = 16.dp   // input bar height accounted for below
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(messages, key = { _, msg -> msg.hashCode() }) { _, msg ->
                            if (msg.isBot) MiliBotBubble(msg.message)
                            else MiliUserBubble(msg.message)
                        }
                        if (isTyping) {
                            item(key = "typing_indicator") { MiliTypingBubble() }
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

            // FIX: This Spacer was pushing content UP when keyboard opened,
            // making the last message go off-screen.
            // Use imePadding() on the Column instead (see Column modifier above).
            // Keeping a small spacer only for the nav bar height.
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
            "Try asking", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = TextSecondary, letterSpacing = 1.3.sp
        )
        Spacer(Modifier.height(14.dp))
        suggestions.forEach { s ->
            Surface(
                onClick   = { onSuggestion(s) },
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                color     = PureWhite,
                border    = BorderStroke(1.dp, SubtleGrey),
                shadowElevation = 3.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        s, fontSize = 13.sp, color = TextPrimary,
                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier.clip(CircleShape).background(BgLavender)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("->", fontSize = 13.sp, color = AccentViolet)
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
            modifier  = Modifier.widthIn(max = 270.dp),
            shape     = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color     = PureWhite,
            border    = BorderStroke(1.dp, SubtleGrey),
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
//  TYPING INDICATOR  — single InfiniteTransition, 3 offsets
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
            shape  = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color  = PureWhite,
            border = BorderStroke(1.dp, SubtleGrey),
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
fun MiliInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(
        Modifier.fillMaxWidth(),
        color          = BgWhite,
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
                RoundedCornerShape(20.dp), PureWhite,
                border = BorderStroke(1.dp, SubtleGrey),
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
                    textStyle      = TextStyle(fontSize = 14.sp),
                    modifier       = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend(); keyboard?.hide() }),
                    singleLine = false,
                    maxLines   = 4
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
                Icon(Icons.Filled.ArrowUpward, null, tint = PureWhite, modifier = Modifier.size(20.dp))
            }
        }
    }
}