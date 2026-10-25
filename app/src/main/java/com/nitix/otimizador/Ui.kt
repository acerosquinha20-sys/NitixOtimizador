package com.nitix.otimizador

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

val BG1 = 0xFF0A0614.toInt()
val BG2 = 0xFF1C0B3D.toInt()
val CARD = 0x1AFFFFFF
val STROKE = 0x33FFFFFF
val ACC = 0xFF8B5CF6.toInt()
val ACC2 = 0xFF22D3EE.toInt()
val TXT = 0xFFFFFFFF.toInt()
val SUB = 0xB3FFFFFF.toInt()
val OK = 0xFF34D399.toInt()
val BAD = 0xFFF87171.toInt()

fun Context.dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.rounded(fill: Int, r: Int, stroke: Int = 0) = GradientDrawable().apply {
    setColor(fill); cornerRadius = dp(r).toFloat()
    if (stroke != 0) setStroke(dp(1), stroke)
}

fun Context.tv(s: String, size: Float, color: Int = TXT, bold: Boolean = false) = TextView(this).apply {
    text = s; textSize = size; setTextColor(color)
    if (bold) typeface = Typeface.DEFAULT_BOLD
}

fun Context.cardBox() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = rounded(CARD, 18, STROKE)
    setPadding(dp(16), dp(14), dp(16), dp(14))
}

fun Context.btn(label: String, accent: Boolean = true, onClick: () -> Unit) = tv(label, 15f, TXT, true).apply {
    gravity = Gravity.CENTER
    background = if (accent)
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(ACC, ACC2)).apply { cornerRadius = dp(14).toFloat() }
    else rounded(0x26FFFFFF, 14, STROKE)
    setPadding(dp(16), dp(14), dp(16), dp(14))
    setOnClickListener { onClick() }
}

fun Context.lpm(topDp: Int = 0) = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
).apply { topMargin = dp(topDp) }

/** Textura de granulado sutil, repetida pela tela. */
fun Context.noise() = BitmapDrawable(resources, Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).also { b ->
    val r = Random(7)
    for (x in 0 until 96) for (y in 0 until 96) b.setPixel(x, y, Color.argb(r.nextInt(16), 255, 255, 255))
}).apply { setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }

/** Brilhos animados da tela de boas-vindas. */
class Sparkles(c: Context) : View(c) {
    private val r = Random(3)
    private val xs = FloatArray(44) { r.nextFloat() }
    private val ys = FloatArray(44) { r.nextFloat() }
    private val ph = FloatArray(44) { r.nextFloat() * 6.28f }
    private val sz = FloatArray(44) { 1.5f + r.nextFloat() * 3f }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        val t = SystemClock.uptimeMillis() / 1000f
        for (i in xs.indices) {
            val a = 0.5f + 0.5f * sin(t * 1.5f + ph[i])
            p.color = if (i % 2 == 0) ACC2 else ACC
            p.alpha = (a * 255).toInt()
            cv.drawCircle(xs[i] * width, ((ys[i] + t * 0.02f * (1 + i % 3)) % 1f) * height, sz[i] * a + 1f, p)
        }
        postInvalidateOnAnimation()
    }
}

/** Fundo animado: manchas de luz (aurora) que se movem devagar. ~16 fps pra poupar GPU. */
class Aurora(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = intArrayOf(ACC, ACC2, 0xFFEC4899.toInt())

    override fun onDraw(cv: Canvas) {
        if (width > 0 && height > 0) {
            val t = SystemClock.uptimeMillis() / 1000f
            val r = max(width, height) * 0.55f
            for (i in 0..2) {
                val cx = width * (0.5f + 0.42f * sin(t * 0.35f + i * 2.1f))
                val cy = height * (0.5f + 0.38f * cos(t * 0.28f + i * 1.7f))
                p.shader = RadialGradient(cx, cy, r, (cols[i] and 0x00FFFFFF) or 0x55000000, 0x00000000, Shader.TileMode.CLAMP)
                cv.drawCircle(cx, cy, r, p)
            }
        }
        postInvalidateDelayed(60)
    }
}
