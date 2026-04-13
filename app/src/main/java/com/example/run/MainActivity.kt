package com.example.run

import android.os.Bundle
import android.view.View
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ═══════════════════════════════════════════════════════════════════════════════
//  NAV SCROLL BUS
// ═══════════════════════════════════════════════════════════════════════════════
object NavScrollBus {
    private val _hidden = MutableStateFlow(false)
    val hidden: StateFlow<Boolean> = _hidden
    fun hide() { _hidden.value = true  }
    fun show() { _hidden.value = false }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PALETTE
// ═══════════════════════════════════════════════════════════════════════════════
private val BgWhite      = Color(0xFFFAF9FF)
private val PureWhite    = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF9B8FD4)
private val HoloPink     = Color(0xFFE8B4D8)
private val HoloMint     = Color(0xFFAEE8D8)

// ── Static Brush constants ────────────────────────────────────────────────────
private val GlassFillBrush        = Brush.verticalGradient(listOf(Color(0xFF1E1830).copy(0.92f), Color(0xFF0D0B14).copy(0.98f)))
private val BorderBrush           = Brush.horizontalGradient(listOf(AccentViolet.copy(0.70f), HoloPink.copy(0.55f), HoloMint.copy(0.40f), AccentViolet.copy(0.70f)))
private val ShimmerLineBrush      = Brush.horizontalGradient(listOf(Color.Transparent, PureWhite.copy(0.18f), PureWhite.copy(0.06f), Color.Transparent))
private val ActivePillBrush       = Brush.horizontalGradient(listOf(AccentViolet.copy(0.30f), HoloPink.copy(0.20f)))
private val ActivePillBorderBrush = Brush.horizontalGradient(listOf(AccentViolet.copy(0.55f), HoloPink.copy(0.35f)))
private val ActiveDotBrush        = Brush.radialGradient(listOf(AccentViolet, HoloPink.copy(0.6f)))
private val TransparentBrush      = Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))

// ─── FIX: static gradient replacing blur() glow halo ─────────────────────────
// blur() forces a full GPU render pass every frame on budget phones.
// A radial gradient achieves the same soft glow look at zero GPU cost.
private val GlowHaloBrush = Brush.radialGradient(
    listOf(AccentViolet.copy(0.32f), HoloPink.copy(0.18f), Color.Transparent),
    radius = 600f
)

private val DoorColorActive   = Color(0xFF1E1830)
private val DoorColorInactive = Color(0xFF0D0B14)
private val DotBgActive       = Color(0xFF1E1830)
private val DotBgInactive     = Color(0xFF0D0B14)

// ═══════════════════════════════════════════════════════════════════════════════
//  NAV MODEL
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
        setContent { MaterialTheme { CaloriXApp(supportFragmentManager) } }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT APP SHELL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun CaloriXApp(fragmentManager: FragmentManager) {

    val containerId = remember { 0x00EAFF01 }
    var selectedTab by remember { mutableIntStateOf(0) }
    var lastTab     by remember { mutableIntStateOf(-1) }

    val navItems = remember {
        listOf(
            NavItem(0, "Home",     NavIconType.HOME,     { HomeFragment() }),
            NavItem(1, "Activity", NavIconType.ACTIVITY, { AF() }),
            NavItem(2, "Mili",     NavIconType.MILI,     { ChatbotFragment() }),
            NavItem(3, "Profile",  NavIconType.PROFILE,  { ProfileFragment() })
        )
    }

    LaunchedEffect(selectedTab) {
        if (lastTab == selectedTab) return@LaunchedEffect
        lastTab = selectedTab
        NavScrollBus.show()
        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(containerId, navItems[selectedTab].fragment())
            .commitAllowingStateLoss()
    }

    val navHidden by NavScrollBus.hidden.collectAsState()

    val navBarTotalDp = 70.dp + 14.dp + 20.dp
    val slideY by animateDpAsState(
        targetValue   = if (navHidden) navBarTotalDp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label         = "navSlide"
    )
    val navAlpha by animateFloatAsState(
        targetValue   = if (navHidden) 0f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label         = "navAlpha"
    )

    Box(Modifier.fillMaxSize()) {

        // ── Fragment host with global scroll detection ─────────────────────────
        AndroidView(
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = containerId
                    var prevScrollY = 0

                    fun resolveScrollTarget(): View {
                        var v: View? = findFocus()
                        while (v != null && v.scrollY == 0 && v.parent is View) {
                            val parent = v.parent as? View ?: break
                            if (parent.scrollY != 0) { v = parent; break }
                            v = parent
                        }
                        return v ?: this
                    }

                    viewTreeObserver.addOnScrollChangedListener {
                        val target  = resolveScrollTarget()
                        val scrollY = target.scrollY
                        val dy      = scrollY - prevScrollY
                        when {
                            dy >  8 -> NavScrollBus.hide()
                            dy < -8 -> NavScrollBus.show()
                        }
                        prevScrollY = scrollY
                    }
                }
            },
            update   = { },
            modifier = Modifier.fillMaxSize()
        )

        // ── Auto-hide glass nav bar ───────────────────────────────────────────
        GlassNavBar(
            items       = navItems,
            selectedTab = selectedTab,
            onSelect    = { selectedTab = it },
            modifier    = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
                .graphicsLayer {
                    translationY = slideY.toPx()
                    alpha        = navAlpha
                }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GLASS NAV BAR  — blur() REMOVED, replaced with radial gradient glow
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
internal fun GlassNavBar(
    items:       List<NavItem>,
    selectedTab: Int,
    onSelect:    (Int) -> Unit,
    modifier:    Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {

        // FIX: was blur(28.dp) — now a static radial gradient.
        // Visually almost identical, GPU cost drops to zero.
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(44.dp))
                .background(GlowHaloBrush)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(GlassFillBrush)
                .border(1.dp, BorderBrush, RoundedCornerShape(44.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp))
                    .background(ShimmerLineBrush)
            )
            Row(
                Modifier.fillMaxSize().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    NavTabItem(item = item, selected = selectedTab == item.index, onClick = { onSelect(item.index) })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  NAV TAB ITEM
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
internal fun NavTabItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val pillWidth by animateDpAsState(
        if (selected) 96.dp else 50.dp,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "pw"
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.15f else 0.95f,
        spring(Spring.DampingRatioMediumBouncy), label = "is"
    )
    val labelAlpha by animateFloatAsState(
        if (selected) 1f else 0f, tween(180), label = "la"
    )

    val iconPath          = remember(item.icon) { Path() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(pillWidth).height(48.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (selected) ActivePillBrush else TransparentBrush)
            .then(if (selected) Modifier.border(0.8.dp, ActivePillBorderBrush, RoundedCornerShape(30.dp)) else Modifier)
            .clickable(indication = null, interactionSource = interactionSource, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            androidx.compose.foundation.Canvas(
                Modifier.size(22.dp).graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            ) {
                drawNavIcon(item.icon, if (selected) AccentViolet else PureWhite.copy(0.40f), this.size, iconPath)
            }
            if (labelAlpha > 0f) {
                Spacer(Modifier.width(5.dp))
                Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentViolet,
                    letterSpacing = 0.3.sp, maxLines = 1, modifier = Modifier.graphicsLayer(alpha = labelAlpha))
            }
        }
        if (selected) {
            Box(Modifier.size(5.dp).align(Alignment.BottomCenter).offset(y = (-5).dp).background(ActiveDotBrush, CircleShape))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CANVAS ICON
// ═══════════════════════════════════════════════════════════════════════════════
private fun DrawScope.drawNavIcon(type: NavIconType, color: Color, size: androidx.compose.ui.geometry.Size, reusable: Path) {
    val w = size.width; val h = size.height; val isActive = color == AccentViolet
    when (type) {
        NavIconType.HOME -> {
            reusable.reset(); reusable.moveTo(w*.5f,h*.08f); reusable.lineTo(w*.02f,h*.52f); reusable.lineTo(w*.98f,h*.52f); reusable.close(); drawPath(reusable,color)
            drawRect(color, topLeft=androidx.compose.ui.geometry.Offset(w*.18f,h*.50f), size=androidx.compose.ui.geometry.Size(w*.64f,h*.44f))
            drawRect(if(isActive)DoorColorActive else DoorColorInactive, topLeft=androidx.compose.ui.geometry.Offset(w*.38f,h*.68f), size=androidx.compose.ui.geometry.Size(w*.24f,h*.26f))
        }
        NavIconType.ACTIVITY -> {
            drawCircle(color,w*.11f,androidx.compose.ui.geometry.Offset(w*.62f,h*.16f))
            reusable.reset(); reusable.moveTo(w*.58f,h*.28f); reusable.lineTo(w*.42f,h*.55f); reusable.lineTo(w*.25f,h*.78f)
            reusable.moveTo(w*.42f,h*.55f); reusable.lineTo(w*.60f,h*.78f)
            reusable.moveTo(w*.58f,h*.28f); reusable.lineTo(w*.38f,h*.40f)
            reusable.moveTo(w*.58f,h*.28f); reusable.lineTo(w*.76f,h*.44f)
            drawPath(reusable,color,style=Stroke(width=w*.13f,cap=StrokeCap.Round,join=StrokeJoin.Round))
        }
        NavIconType.MILI -> {
            reusable.reset(); reusable.addRoundRect(androidx.compose.ui.geometry.RoundRect(0f,0f,w*.90f,h*.72f,w*.18f,w*.18f))
            reusable.moveTo(w*.12f,h*.72f); reusable.lineTo(w*.04f,h*.92f); reusable.lineTo(w*.30f,h*.72f); drawPath(reusable,color)
            val dc=if(isActive)DotBgActive else DotBgInactive
            drawCircle(dc,w*.07f,androidx.compose.ui.geometry.Offset(w*.22f,h*.36f))
            drawCircle(dc,w*.07f,androidx.compose.ui.geometry.Offset(w*.45f,h*.36f))
            drawCircle(dc,w*.07f,androidx.compose.ui.geometry.Offset(w*.68f,h*.36f))
        }
        NavIconType.PROFILE -> {
            drawCircle(color,w*.22f,androidx.compose.ui.geometry.Offset(w*.5f,h*.30f))
            reusable.reset(); reusable.addOval(androidx.compose.ui.geometry.Rect(w*0.04f,h*0.54f,w*0.96f,h*1.18f)); drawPath(reusable,color)
        }
    }
}