package com.riox.snowdropy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class SnowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Flake(
        var x: Float, var y: Float, var r: Float,
        var vy: Float, var amp: Float, var phase: Float, var alpha: Int
    )

    private val flakes = ArrayList<Flake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var last = 0L
    private var t = 0f

    var count = 60
        set(v) { field = v; adjustCount() }
    var speed = 1f
    var sizeScale = 1f
    var flakeColor = 0xFFFFFFFF.toInt()

    /** Frame rate cap (battery saver). */
    var fps = 30

    /** Snow cap that sits on top of the status-bar battery icon. */
    var capOn = true
    var capXdp = 22f   // distance from right edge to cap centre
    var capYdp = 8f    // y of the cap's base (top of battery icon)
    var capWdp = 26f   // cap width

    private fun newFlake(randomY: Boolean): Flake {
        val d = resources.displayMetrics.density
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val r = (1.5f + Random.nextFloat() * 3.5f) * d
        return Flake(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else -r * 2,
            r = r,
            vy = (30f + r * 9f / d) * d / 3f,
            amp = (8f + Random.nextFloat() * 22f) * d,
            phase = Random.nextFloat() * 6.28f,
            alpha = 120 + Random.nextInt(135)
        )
    }

    private fun adjustCount() {
        while (flakes.size < count) flakes.add(newFlake(true))
        while (flakes.size > count) flakes.removeAt(flakes.size - 1)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        flakes.clear()
        adjustCount()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        t += dt
        val h = height.toFloat()
        for (f in flakes) {
            f.y += f.vy * speed * dt
            if (f.y - f.r > h) {
                val n = newFlake(false)
                f.x = n.x; f.y = n.y; f.r = n.r; f.vy = n.vy
                f.amp = n.amp; f.phase = n.phase; f.alpha = n.alpha
            }
            val dx = sin(t * 0.8f * speed + f.phase) * f.amp
            paint.color = flakeColor
            paint.alpha = f.alpha
            canvas.drawCircle(f.x + dx, f.y, f.r * sizeScale, paint)
        }
        if (capOn) drawCap(canvas)

        // frame cap: wait for the rest of the frame budget instead of redrawing at once
        val elapsedMs = (System.nanoTime() - now) / 1_000_000
        val delay = (1000L / fps.coerceIn(10, 60) - elapsedMs).coerceAtLeast(1)
        postInvalidateDelayed(delay)
    }

    private fun drawCap(c: Canvas) {
        val d = resources.displayMetrics.density
        val w = capWdp * d
        val h = w * 0.34f
        val cx = width - capXdp * d
        val base = capYdp * d
        mound(c, cx, base, w, h, 0xFFB9D6F0.toInt(), 1.3f * d)  // soft shade
        mound(c, cx, base, w, h, 0xFFFFFFFF.toInt(), 0f)        // snow
    }

    private fun mound(c: Canvas, cx: Float, base: Float, w: Float, h: Float, color: Int, dy: Float) {
        capPaint.color = color
        c.drawCircle(cx - w * 0.27f, base - h * 0.35f + dy, h * 0.50f, capPaint)
        c.drawCircle(cx + w * 0.02f, base - h * 0.45f + dy, h * 0.62f, capPaint)
        c.drawCircle(cx + w * 0.30f, base - h * 0.30f + dy, h * 0.42f, capPaint)
        c.drawRoundRect(
            cx - w / 2, base - h * 0.30f + dy, cx + w / 2, base + h * 0.12f + dy,
            h * 0.15f, h * 0.15f, capPaint
        )
        c.drawCircle(cx - w * 0.30f, base + h * 0.15f + dy, h * 0.14f, capPaint) // drips
        c.drawCircle(cx + w * 0.12f, base + h * 0.20f + dy, h * 0.10f, capPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        last = 0L
    }
}
