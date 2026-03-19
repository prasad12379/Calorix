package com.example.run

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import kotlin.math.min
import kotlin.math.sin

class WaterGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var fillPercent: Float = 0f
        private set

    private var targetPercent: Float = 0f
    private var waveOffset: Float = 0f
    private var accentColor: Int = Color.parseColor("#00BCD4")

    private val glassPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint   = Paint(Paint.ANTI_ALIAS_FLAG)

    private val waterPath = Path()
    private val clipPath  = Path()

    private val waveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration    = 1800
        repeatCount = ValueAnimator.INFINITE
        repeatMode  = ValueAnimator.RESTART
        addUpdateListener {
            waveOffset = it.animatedValue as Float
            invalidate()
        }
    }

    private data class Bubble(
        var x: Float,
        var y: Float,
        var r: Float,
        var speed: Float,
        var alpha: Float
    )

    private val bubbles = mutableListOf<Bubble>()

    init {
        waveAnimator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bubbles.clear()
        repeat(8) {
            bubbles.add(
                Bubble(
                    // ✅ Fixed: use nextInt() on IntRange directly — no custom extension needed
                    x     = if (w > 40) (20..(w - 20)).randomFloat() else w / 2f,
                    y     = h * 0.3f + (Math.random() * (h * 0.6f)).toFloat(),
                    r     = (3..7).randomFloat(),
                    speed = (1..3).randomFloat(),
                    alpha = 0.2f + (Math.random() * 0.4f).toFloat()
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w   = width.toFloat()
        val h   = height.toFloat()
        val cx  = w / 2f
        val rad = min(w, h) / 2f - 6f

        // Outer glass ring
        rimPaint.style       = Paint.Style.STROKE
        rimPaint.strokeWidth = 3.5f
        rimPaint.color       = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.3f)
        rimPaint.alpha       = 80
        canvas.drawCircle(cx, cx, rad, rimPaint)

        // Dark glass background
        glassPaint.style = Paint.Style.FILL
        glassPaint.color = Color.parseColor("#0D1F35")
        canvas.drawCircle(cx, cx, rad, glassPaint)

        // Water fill with wave
        if (fillPercent > 0f) {
            val waterTop = cx + rad - (fillPercent * 2f * rad)

            waterPath.reset()
            waterPath.moveTo(-10f, waterTop)

            val waveAmp    = rad * 0.06f
            val waveLength = w * 0.7f
            var x = -10f
            while (x < w + 10f) {
                val y = waterTop +
                        waveAmp * sin((x / waveLength * 2f * Math.PI + waveOffset).toDouble()).toFloat() +
                        waveAmp * 0.5f * sin((x / waveLength * 4f * Math.PI + waveOffset * 1.3f).toDouble()).toFloat()
                waterPath.lineTo(x, y)
                x += 4f
            }

            waterPath.lineTo(w + 10f, h + 10f)
            waterPath.lineTo(-10f, h + 10f)
            waterPath.close()

            clipPath.reset()
            clipPath.addCircle(cx, cx, rad - 2f, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clipPath)

            waterPaint.shader = LinearGradient(
                cx, waterTop, cx, h,
                intArrayOf(
                    ColorUtils.blendARGB(accentColor, Color.WHITE, 0.25f),
                    accentColor,
                    ColorUtils.blendARGB(accentColor, Color.BLACK, 0.3f)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            waterPaint.alpha = 210
            canvas.drawPath(waterPath, waterPaint)

            if (fillPercent > 0.05f) {
                for (bubble in bubbles) {
                    bubble.y -= bubble.speed * 0.5f
                    if (bubble.y < waterTop) {
                        bubble.y = cx + rad - 4f
                        // ✅ Fixed: safe random for bubble repositioning
                        bubble.x = if (width > 20) (10..(width - 10)).randomFloat() else width / 2f
                    }
                    if (bubble.y > waterTop) {
                        bubblePaint.color = Color.WHITE
                        bubblePaint.alpha = (bubble.alpha * 120).toInt()
                        bubblePaint.style = Paint.Style.FILL
                        canvas.drawCircle(bubble.x, bubble.y, bubble.r, bubblePaint)
                        bubblePaint.alpha = (bubble.alpha * 60).toInt()
                        bubblePaint.style = Paint.Style.STROKE
                        bubblePaint.strokeWidth = 1f
                        canvas.drawCircle(bubble.x, bubble.y, bubble.r, bubblePaint)
                    }
                }
            }

            val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shinePaint.color = Color.WHITE
            shinePaint.alpha = 40
            shinePaint.style = Paint.Style.FILL
            canvas.drawOval(
                cx - rad * 0.4f, waterTop + 2f,
                cx + rad * 0.1f, waterTop + 7f,
                shinePaint
            )

            canvas.restore()
        }

        // Glass shine
        val shinePaint2 = Paint(Paint.ANTI_ALIAS_FLAG)
        shinePaint2.color       = Color.WHITE
        shinePaint2.alpha       = 25
        shinePaint2.style       = Paint.Style.STROKE
        shinePaint2.strokeWidth = 6f
        val shineRect = RectF(cx - rad * 0.6f, cx - rad * 0.8f, cx, cx - rad * 0.1f)
        canvas.drawArc(shineRect, 200f, 60f, false, shinePaint2)

        // Accent ring
        rimPaint.color       = accentColor
        rimPaint.alpha       = if (fillPercent > 0f) 160 else 60
        rimPaint.strokeWidth = 2.5f
        canvas.drawCircle(cx, cx, rad, rimPaint)

        // Percentage text
        textPaint.color     = Color.WHITE
        textPaint.textSize  = rad * 0.38f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface  = Typeface.DEFAULT_BOLD
        textPaint.alpha     = 200
        val pct  = (fillPercent * 100).toInt()
        val textY = cx + textPaint.textSize * 0.4f
        canvas.drawText("$pct%", cx, textY, textPaint)
    }

    fun animateFill(newPercent: Float, color: Int) {
        accentColor   = color
        targetPercent = newPercent.coerceIn(0f, 1f)

        ValueAnimator.ofFloat(fillPercent, targetPercent).apply {
            duration     = 800
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fillPercent = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun shake() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration    = 500
            repeatCount = 0
            addUpdateListener {
                val t    = it.animatedFraction
                val shakeX = sin(t * Math.PI * 8).toFloat() * 18f * (1f - t)
                translationX = shakeX
            }
            start()
        }
    }

    fun reset(color: Int) {
        accentColor = color
        animateFill(0f, color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel()
    }

    // ✅ Fixed extension — uses this.first / this.last correctly
    // Returns a random Float within the IntRange
    private fun IntRange.randomFloat(): Float =
        (this.first + (Math.random() * (this.last - this.first + 1))).toFloat()
}