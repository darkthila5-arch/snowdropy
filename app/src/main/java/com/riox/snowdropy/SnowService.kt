package com.riox.snowdropy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.PowerManager
import android.view.WindowManager
import androidx.core.content.ContextCompat

class SnowService : Service() {

    companion object {
        @Volatile var running = false
        const val ACTION_STOP = "com.riox.snowdropy.STOP"
        private const val CHANNEL = "snowdropy"
        private const val NOTIF_ID = 1
        private const val NOTIF_PAUSED = 2
    }

    private var wm: WindowManager? = null
    private var view: SnowView? = null
    private var receiverOn = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, _ ->
        view?.applyPrefs(p)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val reason = Prefs.pauseReason(ctx) ?: return
            val nm = ctx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(
                NOTIF_PAUSED,
                Notification.Builder(ctx, CHANNEL)
                    .setSmallIcon(R.drawable.ic_snowflake)
                    .setContentTitle("Snow paused to save battery")
                    .setContentText(reason)
                    .setAutoCancel(true)
                    .build()
            )
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(
            NOTIF_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        if (view == null) addOverlay()
        if (!receiverOn) {
            val f = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            ContextCompat.registerReceiver(this, batteryReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverOn = true
        }
        running = true
        return START_STICKY
    }

    private fun addOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val v = SnowView(this)
        val prefs = Prefs.get(this)
        v.applyPrefs(prefs)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm?.addView(v, lp)
        view = v
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Snowdropy", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, SnowService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_snowflake)
            .setContentTitle("Snowdropy is snowing ❄")
            .setContentText("Powered by Rio X")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(listener)
        if (receiverOn) runCatching { unregisterReceiver(batteryReceiver) }
        receiverOn = false
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
        running = false
        super.onDestroy()
    }
}
