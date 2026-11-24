package com.riox.snowdropy

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.PowerManager
import kotlin.math.min

object Prefs {
    const val FILE = "snowdropy"
    const val DENSITY = "density"     // 10..150
    const val SPEED = "speed"         // 3..30  (x0.1)
    const val SIZE = "size"           // 5..25  (x0.1)
    const val COLOR = "color"         // 0..2
    const val LOW_POWER = "low_power" // fewer flakes + 20fps
    const val AUTO_STOP = "auto_stop" // stop on battery saver / <=20%
    const val CAP_ON = "cap_on"       // snow cap on battery icon
    const val CAP_X = "cap_x"         // dp from right
    const val CAP_Y = "cap_y"         // dp from top
    const val CAP_W = "cap_w"         // dp

    val COLORS = intArrayOf(0xFFFFFFFF.toInt(), 0xFFA8DCFF.toInt(), 0xFFFFC2E2.toInt())

    fun get(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Returns a reason if snow should not run right now, else null. */
    fun pauseReason(ctx: Context): String? {
        if (!get(ctx).getBoolean(AUTO_STOP, true)) return null
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isPowerSaveMode) return "Battery saver is on"
        val i: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        if (level >= 0 && scale > 0 && !charging && level * 100 / scale <= 20)
            return "Battery is at 20% or below"
        return null
    }
}

fun SnowView.applyPrefs(p: SharedPreferences) {
    val low = p.getBoolean(Prefs.LOW_POWER, false)
    speed = p.getInt(Prefs.SPEED, 10) / 10f
    sizeScale = p.getInt(Prefs.SIZE, 10) / 10f
    flakeColor = Prefs.COLORS[p.getInt(Prefs.COLOR, 0).coerceIn(0, 2)]
    fps = if (low) 20 else 30
    capOn = p.getBoolean(Prefs.CAP_ON, true)
    capXdp = p.getInt(Prefs.CAP_X, 22).toFloat()
    capYdp = p.getInt(Prefs.CAP_Y, 8).toFloat()
    capWdp = p.getInt(Prefs.CAP_W, 26).toFloat()
    val d = p.getInt(Prefs.DENSITY, 60)
    count = if (low) min(d, 30) else d
    invalidate()
}
