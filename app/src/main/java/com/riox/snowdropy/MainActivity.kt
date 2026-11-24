package com.riox.snowdropy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var preview: SnowView
    private lateinit var toggle: MaterialButton
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = Prefs.get(this)
        preview = findViewById(R.id.preview)
        toggle = findViewById(R.id.btnToggle)
        status = findViewById(R.id.tvStatus)
        preview.applyPrefs(prefs)

        bindSlider(R.id.slDensity, R.id.tvDensity, Prefs.DENSITY, 60, "Snow amount") { "$it" }
        bindSlider(R.id.slSpeed, R.id.tvSpeed, Prefs.SPEED, 10, "Fall speed") { "%.1fx".format(it / 10f) }
        bindSlider(R.id.slSize, R.id.tvSize, Prefs.SIZE, 10, "Flake size") { "%.1fx".format(it / 10f) }

        val group = findViewById<MaterialButtonToggleGroup>(R.id.colorGroup)
        val ids = intArrayOf(R.id.c0, R.id.c1, R.id.c2)
        group.check(ids[prefs.getInt(Prefs.COLOR, 0).coerceIn(0, 2)])
        group.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                prefs.edit().putInt(Prefs.COLOR, ids.indexOf(id)).apply()
                preview.applyPrefs(prefs)
            }
        }

        toggle.setOnClickListener {
            if (SnowService.running) stopSnow() else startSnow()
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    private fun bindSlider(
        sliderId: Int, labelId: Int, key: String, def: Int,
        title: String, fmt: (Int) -> String
    ) {
        val prefs = Prefs.get(this)
        val s = findViewById<Slider>(sliderId)
        val l = findViewById<TextView>(labelId)
        val v = prefs.getInt(key, def)
        s.value = v.toFloat()
        l.text = "$title  •  ${fmt(v)}"
        s.addOnChangeListener { _, value, fromUser ->
            val iv = value.toInt()
            l.text = "$title  •  ${fmt(iv)}"
            if (fromUser) {
                prefs.edit().putInt(key, iv).apply()
                preview.applyPrefs(prefs)
            }
        }
    }

    private fun startSnow() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow \"Display over other apps\" for Snowdropy", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, SnowService::class.java))
        toggle.postDelayed({ refreshUi() }, 300)
    }

    private fun stopSnow() {
        stopService(Intent(this, SnowService::class.java))
        toggle.postDelayed({ refreshUi() }, 300)
    }

    private fun refreshUi() {
        if (SnowService.running) {
            toggle.text = "Stop Snow"
            status.text = "❄  Snow is falling on your phone"
        } else {
            toggle.text = "Start Snow"
            status.text = "Snow is off"
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }
}
