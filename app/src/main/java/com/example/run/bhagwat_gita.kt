package com.example.run

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// LIGHT THEME COLORS  (positive / sattvic energy)
// ─────────────────────────────────────────────────────────────────────────────
private val BG_PRIMARY      = Color(0xFFFFFBEB)   // warm ivory
private val BG_SURFACE      = Color(0xFFFEF3C7)   // soft amber-white
private val BG_CARD         = Color(0xFFFFFDF5)   // near-white card
private val GOLD            = Color(0xFFB45309)   // deep amber-gold (readable on light)
private val GOLD_LIGHT      = Color(0xFFF59E0B)   // bright gold accent
private val TEXT_PRIMARY    = Color(0xFF451A03)   // deep warm brown
private val TEXT_SECONDARY  = Color(0xFF78350F)   // medium amber-brown
private val TEXT_HINT       = Color(0xFFB45309).copy(alpha = 0.6f)
private val DIVIDER_GOLD    = Color(0xFFD97706).copy(alpha = 0.4f)
private val LOTUS_PINK      = Color(0xFFF43F5E)

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────

enum class MoodType(val label: String, val emoji: String, val color: Color, val lightBg: Color) {
    OVERTHINKING("Overthinking", "🌀", Color(0xFF7C3AED), Color(0xFFF3EEFF)),
    ANGRY       ("Anger",        "🔥", Color(0xFFDC2626), Color(0xFFFFEEEE)),
    ANXIETY     ("Anxiety",      "💨", Color(0xFF0891B2), Color(0xFFE0F7FA)),
    SADNESS     ("Sadness",      "🌧️", Color(0xFF1D4ED8), Color(0xFFEEF2FF)),
    FEAR        ("Fear",         "🌑", Color(0xFF6B7280), Color(0xFFF3F4F6)),
    LONELINESS  ("Loneliness",   "🌿", Color(0xFF059669), Color(0xFFECFDF5)),
    FRUSTRATION ("Frustration",  "⚡", Color(0xFFD97706), Color(0xFFFFFBEB))
}

data class GitaShlok(
    val chapter: String,
    val verse: String,
    val sanskrit: String,
    val transliteration: String,
    val meaning: String,
    val deepMeaning: String,
    val moods: List<MoodType>,
    val stressLevels: List<Int>
)

val GITA_SHLOKS = listOf(
    GitaShlok(
        chapter = "Chapter 2", verse = "Verse 47",
        sanskrit = "कर्मण्येवाधिकारस्ते\nमा फलेषु कदाचन।\nमा कर्मफलहेतुर्भूर्\nमा ते सङ्गोऽस्त्वकर्मणि॥",
        transliteration = "Karmanye vadhikaraste\nMa phaleshu kadachana\nMa karma-phala-hetur bhur\nMa te sango 'stv akarmani",
        meaning = "You have a right to perform your duty, but never to its fruits. Let not the fruits of action be your motive, nor let your attachment be to inaction.",
        deepMeaning = "This is the essence of Karma Yoga. When we act without craving the result, we free ourselves from anxiety and overthinking. The mind becomes still when we focus entirely on the present action — not tomorrow's outcome. Your job is to give 100% right now. The rest belongs to the universe.",
        moods = listOf(MoodType.OVERTHINKING, MoodType.ANXIETY, MoodType.FRUSTRATION),
        stressLevels = listOf(1, 2, 3)
    ),
    GitaShlok(
        chapter = "Chapter 2", verse = "Verse 14",
        sanskrit = "मात्रास्पर्शास्तु कौन्तेय\nशीतोष्णसुखदुःखदाः।\nआगमापायिनोऽनित्यास्\nतांस्तितिक्षस्व भारत॥",
        transliteration = "Matra-sparshas tu kaunteya\nShitoshna-sukha-duhkha-dah\nAgamapayino 'nityas\nTams titikshasva bharata",
        meaning = "The contact of the senses with objects brings cold and heat, pleasure and pain. They come and go — they are impermanent. Endure them, O Arjuna.",
        deepMeaning = "Every feeling you have right now — the anger, the sadness, the fear — is temporary. Like a wave on the ocean, it rises and it falls. You are the ocean, not the wave. What hurts today will pass. You have outlasted every bad day so far. This one too shall pass.",
        moods = listOf(MoodType.ANGRY, MoodType.SADNESS, MoodType.FEAR, MoodType.ANXIETY),
        stressLevels = listOf(0, 1, 2, 3)
    ),
    GitaShlok(
        chapter = "Chapter 6", verse = "Verse 5",
        sanskrit = "उद्धरेदात्मनात्मानं\nनात्मानमवसादयेत्।\nआत्मैव ह्यात्मनो बन्धुर्\nआत्मैव रिपुरात्मनः॥",
        transliteration = "Uddhared atmanatmanam\nNatmanam avasadayet\nAtmaiva hy atmano bandhur\nAtmaiva ripur atmanah",
        meaning = "Lift yourself by yourself. Do not degrade yourself. For you alone are your own friend, and you alone are your own enemy.",
        deepMeaning = "No one else can save you from your inner storms — only you can. The voice that tells you 'I am not enough' is not truth, it is just a habit of thought. You have survived every dark moment so far. The warrior inside you is stronger than the storm. Rise.",
        moods = listOf(MoodType.SADNESS, MoodType.LONELINESS, MoodType.FEAR),
        stressLevels = listOf(2, 3)
    ),
    GitaShlok(
        chapter = "Chapter 2", verse = "Verse 56",
        sanskrit = "दुःखेष्वनुद्विग्नमनाः\nसुखेषु विगतस्पृहः।\nवीतरागभयक्रोधः\nस्थितधीर्मुनिरुच्यते॥",
        transliteration = "Duhkhesv anudvigna-manah\nSukheshu vigata-sprihah\nVita-raga-bhaya-krodhah\nSthita-dhir munir ucyate",
        meaning = "One who is not disturbed in mind even amidst the threefold miseries, nor elated when there is happiness, and who is free from attachment, fear, and anger — such a person is called a sage of steady mind.",
        deepMeaning = "The goal is not to eliminate emotion, but to not be ruled by it. Anger and fear are not your enemy — they are messengers. Acknowledge them, breathe, and let them move through you without holding on. The calm you seek is already inside you — beneath the noise.",
        moods = listOf(MoodType.ANGRY, MoodType.ANXIETY, MoodType.FEAR),
        stressLevels = listOf(1, 2, 3)
    ),
    GitaShlok(
        chapter = "Chapter 18", verse = "Verse 66",
        sanskrit = "सर्वधर्मान्परित्यज्य\nमामेकं शरणं व्रज।\nअहं त्वां सर्वपापेभ्यो\nमोक्षयिष्यामि मा शुचः॥",
        transliteration = "Sarva-dharman parityajya\nMam ekam sharanam vraja\nAham tvam sarva-papebhyo\nMokshayishyami ma shucah",
        meaning = "Abandon all varieties of dharma and simply surrender unto Me. I shall deliver you from all sinful reactions. Do not fear.",
        deepMeaning = "Sometimes the bravest thing you can do is let go. You do not have to carry every burden alone. Release the need to control every outcome. Surrender is not weakness — it is the highest form of trust. Let go, breathe, and trust the process unfolding.",
        moods = listOf(MoodType.OVERTHINKING, MoodType.ANXIETY, MoodType.LONELINESS, MoodType.FRUSTRATION),
        stressLevels = listOf(2, 3)
    ),
    GitaShlok(
        chapter = "Chapter 4", verse = "Verse 38",
        sanskrit = "न हि ज्ञानेन सदृशं\nपवित्रमिह विद्यते।\nतत्स्वयं योगसंसिद्धः\nकालेनात्मनि विन्दति॥",
        transliteration = "Na hi jnanena sadrisham\nPavitram iha vidyate\nTat svayam yoga-samsiddhah\nKalenAtmani vindati",
        meaning = "In this world, there is nothing as purifying as knowledge. One who has achieved perfection through yoga finds this knowledge within himself in due course of time.",
        deepMeaning = "You are not behind. You are not broken. Wisdom comes to those who keep walking the path with patience. Every struggle you face right now is teaching you something that cannot be learned any other way. Trust the timing of your own unfolding.",
        moods = listOf(MoodType.FRUSTRATION, MoodType.SADNESS, MoodType.LONELINESS),
        stressLevels = listOf(0, 1, 2)
    ),
    GitaShlok(
        chapter = "Chapter 3", verse = "Verse 42",
        sanskrit = "इन्द्रियाणि पराण्याहुर्\nइन्द्रियेभ्यः परं मनः।\nमनसस्तु परा बुद्धिर्\nयो बुद्धेः परतस्तु सः॥",
        transliteration = "Indriyani parany ahur\nIndriyebhyah param manah\nManasas tu para buddhir\nYo buddheh paratas tu sah",
        meaning = "The senses are said to be superior to matter; the mind is superior to the senses; the intelligence is superior to the mind; and the soul is even higher than the intelligence.",
        deepMeaning = "Your thoughts are not you. Your anger is not you. Your fear is not you. You are the awareness watching all of it. Step back from the storm in your mind and rest in that awareness — that peaceful, unchanging witness. That is your true self.",
        moods = listOf(MoodType.OVERTHINKING, MoodType.ANGRY, MoodType.ANXIETY),
        stressLevels = listOf(1, 2, 3)
    )
)

fun getShloksForUser(stressScore: Int, mood: MoodType): List<GitaShlok> {
    val stressLevel = when {
        stressScore < 25 -> 0
        stressScore < 50 -> 1
        stressScore < 75 -> 2
        else             -> 3
    }
    return GITA_SHLOKS
        .filter { it.moods.contains(mood) && it.stressLevels.contains(stressLevel) }
        .ifEmpty { GITA_SHLOKS.filter { it.moods.contains(mood) } }
        .ifEmpty { GITA_SHLOKS }
        .take(3)
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY
// ─────────────────────────────────────────────────────────────────────────────
class bhagwat_gita : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stressScore = intent.getIntExtra("STRESS_SCORE", 50)

        setContent {
            var isOmPlaying by remember { mutableStateOf(false) }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = BG_PRIMARY,
                    surface    = BG_SURFACE,
                    onBackground = TEXT_PRIMARY,
                    onSurface    = TEXT_PRIMARY
                )
            ) {
                GitaHealScreen(
                    stressScore = stressScore,
                    isOmPlaying = isOmPlaying,
                    onToggleOm  = {
                        isOmPlaying = !isOmPlaying
                        if (isOmPlaying) startOmSound() else stopOmSound()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun startOmSound() {
        try {
            mediaPlayer?.release()
            // Place "om_background.mp3" in res/raw/ and uncomment:
            // mediaPlayer = MediaPlayer.create(this, R.raw.om_background)
            // mediaPlayer?.isLooping = true
            // mediaPlayer?.start()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopOmSound() { mediaPlayer?.pause() }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN  — 3 phases: MoodSelect → ShlokView → DeepMeaning
// ─────────────────────────────────────────────────────────────────────────────
enum class GitaScreen { MOOD_SELECT, SHLOK_VIEW, DEEP_MEANING }

@Composable
fun GitaHealScreen(
    stressScore: Int,
    isOmPlaying: Boolean,
    onToggleOm: () -> Unit,
    onBack: () -> Unit
) {
    var screen       by remember { mutableStateOf(GitaScreen.MOOD_SELECT) }
    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var shlokIndex   by remember { mutableIntStateOf(0) }
    var shloks       by remember { mutableStateOf(listOf<GitaShlok>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF9E6), Color(0xFFFEF3C7), Color(0xFFFDE8A0))
                )
            )
    ) {
        SunriseBackground()
        LightParticles()

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                fadeIn(tween(600)) + slideInVertically { it / 6 } togetherWith
                        fadeOut(tween(400)) + slideOutVertically { -it / 6 }
            },
            label = "screenTransition"
        ) { currentScreen ->
            when (currentScreen) {
                GitaScreen.MOOD_SELECT -> MoodSelectScreen(
                    stressScore    = stressScore,
                    isOmPlaying    = isOmPlaying,
                    onToggleOm     = onToggleOm,
                    onBack         = onBack,
                    onMoodSelected = { mood ->
                        selectedMood = mood
                        shloks = getShloksForUser(stressScore, mood)
                        shlokIndex = 0
                        screen = GitaScreen.SHLOK_VIEW
                    }
                )
                GitaScreen.SHLOK_VIEW -> ShlokViewScreen(
                    shlok         = shloks.getOrNull(shlokIndex) ?: GITA_SHLOKS.first(),
                    shlokIndex    = shlokIndex,
                    totalShloks   = shloks.size,
                    mood          = selectedMood ?: MoodType.OVERTHINKING,
                    isOmPlaying   = isOmPlaying,
                    onToggleOm    = onToggleOm,
                    onNext        = { if (shlokIndex < shloks.size - 1) shlokIndex++ },
                    onPrev        = { if (shlokIndex > 0) shlokIndex-- },
                    onDeepMeaning = { screen = GitaScreen.DEEP_MEANING },
                    onBack        = { screen = GitaScreen.MOOD_SELECT }
                )
                GitaScreen.DEEP_MEANING -> DeepMeaningScreen(
                    shlok  = shloks.getOrNull(shlokIndex) ?: GITA_SHLOKS.first(),
                    mood   = selectedMood ?: MoodType.OVERTHINKING,
                    onBack = { screen = GitaScreen.SHLOK_VIEW }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MOOD SELECTION SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MoodSelectScreen(
    stressScore: Int,
    isOmPlaying: Boolean,
    onToggleOm: () -> Unit,
    onBack: () -> Unit,
    onMoodSelected: (MoodType) -> Unit
) {
    val haptic  = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val stressLabel = when {
        stressScore < 25 -> Triple("Calm",        Color(0xFF16A34A), Color(0xFFDCFCE7))
        stressScore < 50 -> Triple("Moderate",    Color(0xFFD97706), Color(0xFFFEF3C7))
        stressScore < 75 -> Triple("Stressed",    Color(0xFFEA580C), Color(0xFFFFEDD5))
        else             -> Triple("Overwhelmed", Color(0xFFDC2626), Color(0xFFFFEEEE))
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically { -40 }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OmSymbol()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "गीता सार",
                    fontSize      = 13.sp,
                    color         = GOLD,
                    letterSpacing = 4.sp,
                    fontWeight    = FontWeight.Light
                )
                Text(
                    "Find Your Peace",
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TEXT_PRIMARY,
                    textAlign  = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Let the Gita light your path ✨",
                    fontSize  = 13.sp,
                    color     = TEXT_SECONDARY.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Stress badge — light coloured background
                Row(
                    modifier = Modifier
                        .background(stressLabel.third, RoundedCornerShape(50.dp))
                        .border(1.dp, stressLabel.second.copy(0.35f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(stressLabel.second, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Stress Level: ${stressLabel.first}  •  $stressScore / 100",
                        fontSize   = 12.sp,
                        color      = stressLabel.second,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(700, 200))) {
            Text(
                "What are you feeling right now?",
                fontSize  = 16.sp,
                color     = TEXT_SECONDARY,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        val moods = MoodType.values().toList()
        moods.chunked(2).forEachIndexed { rowIdx, row ->
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(500, 300 + rowIdx * 100)) +
                        slideInVertically(tween(500, 300 + rowIdx * 100)) { 30 }
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { mood ->
                        MoodChip(
                            mood     = mood,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMoodSelected(mood)
                            }
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(700, 600))) {
            OmToggleButton(isPlaying = isOmPlaying, onToggle = onToggleOm)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("← Back", color = TEXT_HINT)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MoodChip(mood: MoodType, modifier: Modifier, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "moodScale"
    )
    val inf = rememberInfiniteTransition(label = "chip")
    val borderAlpha by inf.animateFloat(
        0.25f, 0.6f,
        infiniteRepeatable(tween(1800 + mood.ordinal * 200), RepeatMode.Reverse),
        label = "chipBorder"
    )

    Box(
        modifier = modifier
            .height(84.dp)
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = mood.color.copy(0.15f))
            .background(mood.lightBg, RoundedCornerShape(20.dp))
            .border(1.5.dp, mood.color.copy(borderAlpha), RoundedCornerShape(20.dp))
            .clickable { pressed = true; onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mood.emoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                mood.label,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = mood.color
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHLOK VIEW SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ShlokViewScreen(
    shlok: GitaShlok,
    shlokIndex: Int,
    totalShloks: Int,
    mood: MoodType,
    isOmPlaying: Boolean,
    onToggleOm: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDeepMeaning: () -> Unit,
    onBack: () -> Unit
) {
    val haptic  = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(shlokIndex) { visible = false; delay(50); visible = true }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp, color = TEXT_SECONDARY)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .background(mood.lightBg, RoundedCornerShape(50.dp))
                    .border(1.dp, mood.color.copy(0.3f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mood.emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    mood.label,
                    fontSize   = 12.sp,
                    color      = mood.color,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            OmToggleButton(isPlaying = isOmPlaying, onToggle = onToggleOm, compact = true)
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(500))) {
            Text(
                "${shlok.chapter}  •  ${shlok.verse}",
                fontSize      = 12.sp,
                color         = GOLD,
                letterSpacing = 2.sp,
                fontWeight    = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(700, 100)) + scaleIn(tween(700, 100), 0.92f)) {
            SanskritCard(shlok = shlok, accentColor = mood.color)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Transliteration card
        AnimatedVisibility(visible, enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200)) { 20 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF8E1), RoundedCornerShape(16.dp))
                    .border(1.dp, DIVIDER_GOLD, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    shlok.transliteration,
                    fontSize   = 13.sp,
                    color      = GOLD,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier   = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(600, 300)) + slideInVertically(tween(600, 300)) { 20 }) {
            MeaningCard(meaning = shlok.meaning, accentColor = mood.color, lightBg = mood.lightBg)
        }

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(600, 400))) {
            DeepMeaningButton(accentColor = mood.color, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeepMeaning()
            })
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(500, 500))) {
            ShlokNavigation(
                current = shlokIndex,
                total   = totalShloks,
                onPrev  = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPrev() },
                onNext  = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNext() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SanskritCard(shlok: GitaShlok, accentColor: Color) {
    val inf = rememberInfiniteTransition(label = "sanskrit")
    val glowAlpha by inf.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "sanskritGlow"
    )
    val shimmerX by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFFD97706).copy(0.2f))
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFFDF0), Color(0xFFFFF8DC))),
                RoundedCornerShape(24.dp)
            )
            .border(
                2.dp,
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFD97706).copy(glowAlpha),
                        Color(0xFFFBBF24).copy(glowAlpha * 0.6f),
                        Color(0xFFD97706).copy(glowAlpha)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        // Subtle shimmer sweep
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color(0xFFD97706).copy(0.06f), Color.Transparent),
                    start = Offset(shimmerX * size.width, 0f),
                    end   = Offset((shimmerX + 0.4f) * size.width, size.height)
                )
            )
        }

        Column(
            modifier            = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DIVIDER_GOLD)
                Text("  ॐ  ", fontSize = 18.sp, color = GOLD_LIGHT)
                HorizontalDivider(modifier = Modifier.weight(1f), color = DIVIDER_GOLD)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text       = shlok.sanskrit,
                fontSize   = 19.sp,
                color      = TEXT_PRIMARY,
                textAlign  = TextAlign.Center,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DIVIDER_GOLD)
                Text("  ॥  ", fontSize = 14.sp, color = GOLD.copy(0.5f))
                HorizontalDivider(modifier = Modifier.weight(1f), color = DIVIDER_GOLD)
            }
        }
    }
}

@Composable
fun MeaningCard(meaning: String, accentColor: Color, lightBg: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = accentColor.copy(0.1f))
            .background(lightBg, RoundedCornerShape(20.dp))
            .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", fontSize = 11.sp, color = accentColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "MEANING",
                    fontSize      = 10.sp,
                    color         = accentColor,
                    letterSpacing = 2.sp,
                    fontWeight    = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                meaning,
                fontSize   = 15.sp,
                color      = TEXT_PRIMARY,
                lineHeight = 26.sp,
                textAlign  = TextAlign.Start
            )
        }
    }
}

@Composable
fun DeepMeaningButton(accentColor: Color, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "deepBtn")
    val scale by inf.animateFloat(
        1f, 1.025f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "deepScale"
    )
    val shadowAlpha by inf.animateFloat(
        0.2f, 0.45f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "shadowA"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(50.dp), ambientColor = accentColor.copy(shadowAlpha))
            .background(
                Brush.horizontalGradient(listOf(accentColor, accentColor.copy(0.85f))),
                RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔮", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Understand Deeper Meaning",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}

@Composable
fun ShlokNavigation(current: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (current > 0) Color(0xFFFEF3C7) else Color(0xFFF5F5F5),
                    CircleShape
                )
                .border(1.dp, if (current > 0) GOLD_LIGHT.copy(0.4f) else Color.LightGray, CircleShape)
                .clickable(enabled = current > 0) { onPrev() },
            contentAlignment = Alignment.Center
        ) {
            Text("←", fontSize = 18.sp, color = if (current > 0) TEXT_SECONDARY else Color.LightGray)
        }

        Spacer(modifier = Modifier.width(20.dp))

        repeat(total) { idx ->
            val isActive = idx == current
            val dotSize by animateDpAsState(if (isActive) 10.dp else 6.dp, label = "dot")
            val dotColor by animateColorAsState(
                if (isActive) GOLD_LIGHT else GOLD_LIGHT.copy(0.3f),
                label = "dotC"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(dotSize)
                    .background(dotColor, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (current < total - 1) Color(0xFFFEF3C7) else Color(0xFFF5F5F5),
                    CircleShape
                )
                .border(1.dp, if (current < total - 1) GOLD_LIGHT.copy(0.4f) else Color.LightGray, CircleShape)
                .clickable(enabled = current < total - 1) { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Text("→", fontSize = 18.sp, color = if (current < total - 1) TEXT_SECONDARY else Color.LightGray)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEEP MEANING SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DeepMeaningScreen(shlok: GitaShlok, mood: MoodType, onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(shlok) {
        displayedText = ""
        delay(400)
        shlok.deepMeaning.forEach { char ->
            displayedText += char
            delay(16)
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(52.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp, color = TEXT_SECONDARY)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Deep Wisdom",
                fontSize      = 14.sp,
                color         = GOLD,
                letterSpacing = 2.sp,
                fontWeight    = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Glowing lotus centerpiece — light gold rings
        AnimatedVisibility(visible, enter = fadeIn(tween(800)) + scaleIn(tween(800), 0.7f)) {
            Box(contentAlignment = Alignment.Center) {
                val inf = rememberInfiniteTransition(label = "lotus")
                val rot by inf.animateFloat(
                    0f, 360f,
                    infiniteRepeatable(tween(20000, easing = LinearEasing)),
                    label = "lotusRot"
                )
                val ringA by inf.animateFloat(
                    0.2f, 0.6f,
                    infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "ringA"
                )
                val ring2 by inf.animateFloat(
                    0.1f, 0.35f,
                    infiniteRepeatable(tween(2800), RepeatMode.Reverse),
                    label = "ring2"
                )
                Canvas(modifier = Modifier.size(160.dp)) {
                    rotate(rot) {
                        drawArc(
                            brush      = Brush.sweepGradient(
                                listOf(Color.Transparent, Color(0xFFD97706).copy(ringA), Color.Transparent)
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            style      = Stroke(width = 2.dp.toPx())
                        )
                    }
                    rotate(-rot * 0.6f) {
                        drawArc(
                            brush      = Brush.sweepGradient(
                                listOf(Color.Transparent, Color(0xFFFBBF24).copy(ring2), Color.Transparent)
                            ),
                            startAngle = 45f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            topLeft    = Offset(size.width * 0.1f, size.height * 0.1f),
                            size       = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.8f),
                            style      = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    drawCircle(
                        brush  = Brush.radialGradient(
                            listOf(Color(0xFFD97706).copy(0.12f), Color.Transparent)
                        ),
                        radius = size.minDimension / 2.2f
                    )
                }
                Text("🪷", fontSize = 56.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(600, 200))) {
            Text(
                "${shlok.chapter}  •  ${shlok.verse}",
                fontSize      = 11.sp,
                color         = GOLD,
                letterSpacing = 2.sp,
                fontWeight    = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(600, 300))) {
            Text(
                shlok.sanskrit.lines().take(2).joinToString("\n"),
                fontSize   = 14.sp,
                color      = TEXT_SECONDARY.copy(0.55f),
                textAlign  = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Deep meaning card — light mood-tinted
        AnimatedVisibility(visible, enter = fadeIn(tween(700, 400))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = mood.color.copy(0.15f))
                    .background(mood.lightBg, RoundedCornerShape(24.dp))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(mood.color.copy(0.45f), mood.color.copy(0.1f))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .background(mood.color.copy(0.12f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mood.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "For your ${mood.label}",
                            fontSize      = 11.sp,
                            color         = mood.color,
                            letterSpacing = 1.sp,
                            fontWeight    = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text       = displayedText,
                        fontSize   = 16.sp,
                        color      = TEXT_PRIMARY,
                        lineHeight = 28.sp,
                        textAlign  = TextAlign.Start
                    )
                    if (displayedText.length < shlok.deepMeaning.length) {
                        val inf = rememberInfiniteTransition(label = "cursor")
                        val cursorAlpha by inf.animateFloat(
                            1f, 0f,
                            infiniteRepeatable(tween(500), RepeatMode.Reverse),
                            label = "cur"
                        )
                        Text("▌", fontSize = 16.sp, color = mood.color.copy(cursorAlpha))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(600, 800))) {
            BreathingCard()
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Positive affirmation footer
        AnimatedVisibility(visible, enter = fadeIn(tween(600, 1000))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF8E1), RoundedCornerShape(16.dp))
                    .border(1.dp, DIVIDER_GOLD, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🌸  You are light. You are peace. You are enough.  🌸",
                    fontSize  = 13.sp,
                    color     = GOLD,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(500, 900))) {
            TextButton(onClick = onBack) {
                Text("← Back to Shlok", color = TEXT_HINT)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BREATHING REMINDER CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BreathingCard() {
    val inf = rememberInfiniteTransition(label = "breath")
    val breathScale by inf.animateFloat(
        1f, 1.18f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val breathAlpha by inf.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "bAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFF059669).copy(0.1f))
            .background(Color(0xFFECFDF5), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF10B981).copy(0.4f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(breathScale)
                        .background(Color(0xFF10B981).copy(breathAlpha * 0.15f), CircleShape)
                )
                Text("🫁", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    "Breathe with this",
                    fontSize   = 13.sp,
                    color      = Color(0xFF047857),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Inhale 4s  ·  Hold 4s  ·  Exhale 6s",
                    fontSize = 12.sp,
                    color    = Color(0xFF059669).copy(0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Let each breath carry you home 🍃",
                    fontSize = 11.sp,
                    color    = Color(0xFF047857).copy(0.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OM TOGGLE BUTTON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OmToggleButton(isPlaying: Boolean, onToggle: () -> Unit, compact: Boolean = false) {
    val haptic = LocalHapticFeedback.current
    val inf    = rememberInfiniteTransition(label = "om")
    val glow by inf.animateFloat(
        0.3f, 0.9f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "omGlow"
    )

    val bgColor    = if (isPlaying) Color(0xFF7C3AED) else Color(0xFFFEF3C7)
    val borderCol  = if (isPlaying) Color(0xFF7C3AED).copy(glow) else GOLD_LIGHT.copy(0.5f)
    val textColor  = if (isPlaying) Color.White else TEXT_SECONDARY
    val label      = if (compact) "ॐ" else (if (isPlaying) "ॐ  Stop Om Sound" else "ॐ  Play Om Sound")

    Box(
        modifier = Modifier
            .then(
                if (compact) Modifier.size(40.dp)
                else Modifier.fillMaxWidth().height(52.dp)
            )
            .shadow(if (isPlaying) 8.dp else 3.dp, if (compact) CircleShape else RoundedCornerShape(50.dp),
                ambientColor = if (isPlaying) Color(0xFF7C3AED).copy(0.3f) else Color.Transparent)
            .background(bgColor, if (compact) CircleShape else RoundedCornerShape(50.dp))
            .border(1.5.dp, borderCol, if (compact) CircleShape else RoundedCornerShape(50.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize   = if (compact) 18.sp else 15.sp,
            color      = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OM SYMBOL ANIMATED
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OmSymbol() {
    val inf = rememberInfiniteTransition(label = "omSym")
    val scale by inf.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "omScale"
    )
    val ringScale by inf.animateFloat(
        1f, 1.8f,
        infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "omRing"
    )
    val ringAlpha by inf.animateFloat(
        0.45f, 0f,
        infiniteRepeatable(tween(2500), RepeatMode.Restart),
        label = "omRingA"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        // Expanding warm ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .background(Color(0xFFD97706).copy(0.12f), CircleShape)
                .border(1.dp, Color(0xFFD97706).copy(ringAlpha * 0.5f), CircleShape)
        )
        // Inner circle — warm ivory with gold border
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(scale)
                .shadow(6.dp, CircleShape, ambientColor = Color(0xFFD97706).copy(0.2f))
                .background(
                    Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))),
                    CircleShape
                )
                .border(2.dp, Color(0xFFD97706).copy(0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("ॐ", fontSize = 42.sp, color = GOLD_LIGHT)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUNRISE BACKGROUND  (replaces dark mandala)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SunriseBackground() {
    val inf = rememberInfiniteTransition(label = "sunrise")
    val rot by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "sRot"
    )
    val pulse by inf.animateFloat(
        0.88f, 1f,
        infiniteRepeatable(tween(4500), RepeatMode.Reverse),
        label = "sPulse"
    )
    val haloAlpha by inf.animateFloat(
        0.06f, 0.16f,
        infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "halo"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.22f

        // Soft sunrise halo
        drawCircle(
            brush  = Brush.radialGradient(
                listOf(Color(0xFFFCD34D).copy(haloAlpha * 1.5f), Color.Transparent),
                center = Offset(cx, cy),
                radius = size.width * 0.65f
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.65f
        )

        // Mandala rings — golden on light bg (slightly stronger alpha)
        listOf(70f, 115f, 165f, 215f).forEachIndexed { i, radius ->
            val r = radius * pulse
            drawArc(
                color      = Color(0xFFD97706).copy(alpha = 0.09f - i * 0.015f),
                startAngle = rot + i * 15f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = Offset(cx - r, cy - r),
                size       = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style      = Stroke(width = 1.2f)
            )
            // Petal dots
            repeat(8) { j ->
                val angle = Math.toRadians((rot + j * 45.0 + i * 22.5))
                val px = cx + cos(angle).toFloat() * r
                val py = cy + sin(angle).toFloat() * r
                drawCircle(
                    color  = Color(0xFFD97706).copy(0.12f - i * 0.02f),
                    radius = 4.5f,
                    center = Offset(px, py)
                )
            }
        }

        // Inner petal flowers (lotus petals in background)
        repeat(12) { k ->
            val angle = Math.toRadians((rot * 0.3 + k * 30.0))
            val r = 50f * pulse
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            drawCircle(
                color  = LOTUS_PINK.copy(0.04f),
                radius = 12f,
                center = Offset(px, py)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIGHT PARTICLES  (warm sunlit specks replacing dark particles)
// ─────────────────────────────────────────────────────────────────────────────
data class Pt(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, val r: Float)

@Composable
fun LightParticles() {
    val particles = remember {
        List(30) {
            Pt(
                x     = Random.nextFloat(),
                y     = Random.nextFloat(),
                vx    = (Random.nextFloat() - 0.5f) * 0.00012f,
                vy    = -Random.nextFloat() * 0.00018f - 0.00004f,
                color = listOf(
                    Color(0xFFD97706),  // warm amber
                    Color(0xFFFBBF24),  // gold
                    Color(0xFF10B981),  // mint green
                    Color(0xFF60A5FA),  // sky blue
                    Color(0xFFF472B6),  // soft pink
                    Color(0xFF34D399)   // jade
                ).random(),
                r = Random.nextFloat() * 2.8f + 0.8f
            )
        }
    }

    val inf = rememberInfiniteTransition(label = "pts")
    val time by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "ptTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val px = ((p.x + p.vx * time * 100000f) % 1f)
                .let { if (it < 0f) it + 1f else it } * size.width
            val py = ((p.y + p.vy * time * 100000f) % 1f)
                .let { if (it < 0f) it + 1f else it } * size.height
            // Soft glow dot
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(p.color.copy(0.55f), p.color.copy(0f)),
                    center = Offset(px, py),
                    radius = p.r * density * 3
                ),
                radius = p.r * density * 3,
                center = Offset(px, py)
            )
        }
    }
}