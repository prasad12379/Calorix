package com.example.run

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager

// ═══════════════════════════════════════════════════════════════════════════════
//  CALORIX PALETTE
// ═══════════════════════════════════════════════════════════════════════════════
private val BgWhite      = Color(0xFFFAF9FF)
private val PureWhite    = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF9B8FD4)
private val HoloPink     = Color(0xFFE8B4D8)
private val HoloMint     = Color(0xFFAEE8D8)

// ── Pre-built static Brush objects (module-level) ─────────────────────────────
// Computed once at startup — never recreated on recomposition or draw frame.
private val GlassFillBrush = Brush.verticalGradient(
    listOf(Color(0xFF1E1830).copy(alpha = 0.88f), Color(0xFF0D0B14).copy(alpha = 0.96f))
)
private val GlowHaloBrush = Brush.horizontalGradient(
    listOf(AccentViolet.copy(0.38f), HoloPink.copy(0.26f), HoloMint.copy(0.18f))
)
private val BorderBrush = Brush.horizontalGradient(
    listOf(AccentViolet.copy(0.70f), HoloPink.copy(0.55f), HoloMint.copy(0.40f), AccentViolet.copy(0.70f))
)
private val ShimmerLineBrush = Brush.horizontalGradient(
    listOf(Color.Transparent, PureWhite.copy(0.18f), PureWhite.copy(0.06f), Color.Transparent)
)
private val ActivePillBrush = Brush.horizontalGradient(
    listOf(AccentViolet.copy(0.30f), HoloPink.copy(0.20f))
)
private val ActivePillBorderBrush = Brush.horizontalGradient(
    listOf(AccentViolet.copy(0.55f), HoloPink.copy(0.35f))
)
private val ActiveDotBrush = Brush.radialGradient(
    listOf(AccentViolet, HoloPink.copy(0.6f))
)
private val TransparentBrush = Brush.horizontalGradient(
    listOf(Color.Transparent, Color.Transparent)
)

// ── Door colour constants (used inside Canvas draw, avoid allocation) ─────────
private val DoorColorActive   = Color(0xFF1E1830)
private val DoorColorInactive = Color(0xFF0D0B14)
private val DotColorActive    = Color(0xFF1E1830)
private val DotColorInactive  = Color(0xFF0D0B14)

// ═══════════════════════════════════════════════════════════════════════════════
//  NAV ITEM MODEL
// ═══════════════════════════════════════════════════════════════════════════════
internal data class NavItem(
    val index:    Int,
    val label:    String,
    val icon:     NavIconType,
    val fragment: () -> androidx.fragment.app.Fragment
)

internal enum class NavIconType { HOME, ACTIVITY, MILI, PROFILE }

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN ACTIVITY
// ═══════════════════════════════════════════════════════════════════════════════
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            MaterialTheme {
                CaloriXApp(supportFragmentManager)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT APP SHELL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CaloriXApp(fragmentManager: FragmentManager) {

    val containerId = remember { 0x00EAFF01 }
    var selectedTab by remember { mutableIntStateOf(0) }

    // navItems is stable — never recreated
    val navItems = remember {
        listOf(
            NavItem(0, "Home",     NavIconType.HOME,     { HomeFragment() }),
            NavItem(1, "Activity", NavIconType.ACTIVITY, { AF() }),
            NavItem(2, "Mili",     NavIconType.MILI,     { ChatbotFragment() }),
            NavItem(3, "Profile",  NavIconType.PROFILE,  { ProfileFragment() })
        )
    }

    LaunchedEffect(selectedTab) {
        fragmentManager.beginTransaction()
            .replace(containerId, navItems[selectedTab].fragment())
            .commitAllowingStateLoss()
    }

    Box(Modifier.fillMaxSize().background(BgWhite)) {

        // Full-screen fragment host
        // update = {} prevents AndroidView from doing anything on recomposition
        AndroidView(
            factory  = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
            update   = { },
            modifier = Modifier.fillMaxSize()
        )

        // Floating glass nav bar overlaid at the bottom
        GlassNavBar(
            items       = navItems,
            selectedTab = selectedTab,
            onSelect    = { selectedTab = it },
            modifier    = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GLASS FLOATING NAV BAR
//
//  Performance notes
//  ─────────────────
//  • GlowHaloBrush is static (module-level) — no allocation on recomposition.
//  • blur() is applied to a STATIC box — no animation drives it, so the GPU
//    only runs the blur effect once, not every frame.
//  • All other Brush objects are module-level constants.
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
internal fun GlassNavBar(
    items:       List<NavItem>,
    selectedTab: Int,
    onSelect:    (Int) -> Unit,
    modifier:    Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {

        // ── Static blurred glow halo — NOT animated, runs blur ONCE ──────────
        Box(
            Modifier
                .matchParentSize()
                .blur(28.dp)                       // static — no glowAlpha driving it
                .clip(RoundedCornerShape(44.dp))
                .background(GlowHaloBrush)         // module-level constant
        )

        // ── Frosted glass pill ────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(GlassFillBrush)        // module-level constant
                .border(1.dp, BorderBrush, RoundedCornerShape(44.dp))  // constant
        ) {
            // Top highlight shimmer line — static, gives glass illusion
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp))
                    .background(ShimmerLineBrush)  // module-level constant
            )

            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    NavTabItem(
                        item     = item,
                        selected = selectedTab == item.index,
                        onClick  = { onSelect(item.index) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  INDIVIDUAL NAV TAB
//
//  Performance notes
//  ─────────────────
//  • ActivePillBrush, ActivePillBorderBrush, ActiveDotBrush — module-level constants.
//  • TransparentBrush — module-level constant (avoids Brush allocation when unselected).
//  • iconPath is remembered per NavIconType — reused across draw frames, no
//    Path allocation inside the draw callback.
//  • MutableInteractionSource is remembered — stable across recompositions.
//  • graphicsLayer{} used for scale animation instead of re-layout.
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
internal fun NavTabItem(
    item:     NavItem,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue   = if (selected) 96.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "pw"
    )
    val iconScale by animateFloatAsState(
        targetValue   = if (selected) 1.15f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "is"
    )
    val labelAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label         = "la"
    )

    // Reusable Path object — allocated once per icon type, reset on each draw
    val iconPath = remember(item.icon) { Path() }

    // Stable interaction source
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(48.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (selected) ActivePillBrush else TransparentBrush)
            .then(
                if (selected) Modifier.border(0.8.dp, ActivePillBorderBrush, RoundedCornerShape(30.dp))
                else Modifier
            )
            .clickable(
                indication        = null,
                interactionSource = interactionSource,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier              = Modifier.padding(horizontal = 12.dp)
        ) {
            // Canvas icon — uses graphicsLayer for scale (no re-layout)
            // iconPath is reused: reset() inside draw, no new Path() per frame
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            ) {
                drawNavIcon(
                    type     = item.icon,
                    color    = if (selected) AccentViolet else PureWhite.copy(0.40f),
                    size     = this.size,
                    reusable = iconPath
                )
            }

            // Label fades in — only composed when visible
            if (labelAlpha > 0f) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text          = item.label,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = AccentViolet,
                    letterSpacing = 0.3.sp,
                    maxLines      = 1,
                    modifier      = Modifier.graphicsLayer(alpha = labelAlpha)
                )
            }
        }

        // Active dot indicator
        if (selected) {
            Box(
                Modifier
                    .size(5.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-5).dp)
                    .background(ActiveDotBrush, CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CANVAS ICON DRAWING
//
//  Performance notes
//  ─────────────────
//  • Accepts a `reusable: Path` parameter — callers pass a remembered Path.
//  • Each branch calls reusable.reset() then reuses it, allocating zero
//    new Path objects per draw frame.
//  • Door/dot colours are module-level constants — no Color() allocation.
// ═══════════════════════════════════════════════════════════════════════════════
private fun DrawScope.drawNavIcon(
    type:     NavIconType,
    color:    Color,
    size:     androidx.compose.ui.geometry.Size,
    reusable: Path
) {
    val w = size.width
    val h = size.height
    val isActive = color == AccentViolet

    when (type) {

        NavIconType.HOME -> {
            // Roof triangle
            reusable.reset()
            reusable.moveTo(w * 0.5f,  h * 0.08f)
            reusable.lineTo(w * 0.02f, h * 0.52f)
            reusable.lineTo(w * 0.98f, h * 0.52f)
            reusable.close()
            drawPath(reusable, color)

            // House body
            drawRect(
                color   = color,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.50f),
                size    = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.44f)
            )
            // Door cutout
            drawRect(
                color   = if (isActive) DoorColorActive else DoorColorInactive,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.68f),
                size    = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.26f)
            )
        }

        NavIconType.ACTIVITY -> {
            // Head
            drawCircle(
                color  = color,
                radius = w * 0.11f,
                center = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.16f)
            )
            // Running body strokes
            reusable.reset()
            reusable.moveTo(w * 0.58f, h * 0.28f)
            reusable.lineTo(w * 0.42f, h * 0.55f)
            reusable.lineTo(w * 0.25f, h * 0.78f)
            reusable.moveTo(w * 0.42f, h * 0.55f)
            reusable.lineTo(w * 0.60f, h * 0.78f)
            reusable.moveTo(w * 0.58f, h * 0.28f)
            reusable.lineTo(w * 0.38f, h * 0.40f)
            reusable.moveTo(w * 0.58f, h * 0.28f)
            reusable.lineTo(w * 0.76f, h * 0.44f)
            drawPath(
                reusable, color,
                style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        NavIconType.MILI -> {
            // Chat bubble
            reusable.reset()
            reusable.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left    = 0f,       top    = 0f,
                    right   = w * 0.90f, bottom = h * 0.72f,
                    radiusX = w * 0.18f, radiusY = w * 0.18f
                )
            )
            // Tail
            reusable.moveTo(w * 0.12f, h * 0.72f)
            reusable.lineTo(w * 0.04f, h * 0.92f)
            reusable.lineTo(w * 0.30f, h * 0.72f)
            drawPath(reusable, color)

            // Three dots — no Path needed, just circles
            val dotColor = if (isActive) DotColorActive else DotColorInactive
            drawCircle(dotColor, w * 0.07f, androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.36f))
            drawCircle(dotColor, w * 0.07f, androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.36f))
            drawCircle(dotColor, w * 0.07f, androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.36f))
        }

        NavIconType.PROFILE -> {
            // Head
            drawCircle(
                color  = color,
                radius = w * 0.22f,
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.30f)
            )
            // Shoulders ellipse
            reusable.reset()
            reusable.addOval(
                androidx.compose.ui.geometry.Rect(
                    left   = w * 0.04f, top    = h * 0.54f,
                    right  = w * 0.96f, bottom = h * 1.18f
                )
            )
            drawPath(reusable, color)
        }
    }
}