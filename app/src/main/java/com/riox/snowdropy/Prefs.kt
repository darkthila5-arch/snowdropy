package com.riox.snowdropy

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val FILE = "snowdropy"
    const val DENSITY = "density"   // 10..150
    const val SPEED = "speed"       // 3..30  (x0.1)
    const val SIZE = "size"         // 5..25  (x0.1)
    const val COLOR = "color"       // 0..2

    val COLORS = intArrayOf(0xFFFFFFFF.toInt(), 0xFFA8DCFF.toInt(), 0xFFFFC2E2.toInt())

    fun get(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

fun SnowView.applyPrefs(p: SharedPreferences) {
    speed = p.getInt(Prefs.SPEED, 10) / 10f
    sizeScale = p.getInt(Prefs.SIZE, 10) / 10f
    flakeColor = Prefs.COLORS[p.getInt(Prefs.COLOR, 0).coerceIn(0, 2)]
    count = p.getInt(Prefs.DENSITY, 60)
    invalidate()
}
