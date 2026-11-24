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
    private var last = 0L
    private var t = 0f

    var count = 60
        set(v) { field = v; adjustCount() }
    var speed = 1f
    var sizeScale = 1f
    var flakeColor = 0xFFFFFFFF.toInt()

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
        val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
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
            val rr = f.r * sizeScale
            paint.color = flakeColor
            paint.alpha = f.alpha
            canvas.drawCircle(f.x + dx, f.y, rr, paint)
        }
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        last = 0L
    }
}
