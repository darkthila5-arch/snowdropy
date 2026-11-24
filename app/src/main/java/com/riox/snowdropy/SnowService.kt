package com.riox.snowdropy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager

class SnowService : Service() {

    companion object {
        @Volatile var running = false
        const val ACTION_STOP = "com.riox.snowdropy.STOP"
        private const val CHANNEL = "snowdropy"
        private const val NOTIF_ID = 1
    }

    private var wm: WindowManager? = null
    private var view: SnowView? = null
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, _ ->
        view?.applyPrefs(p)
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
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
        running = false
        super.onDestroy()
    }
}
