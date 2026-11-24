package com.riox.snowdropy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var preview: SnowView
    private lateinit var toggle: MaterialButton
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // draw behind the status bar so the snow cap can be lined up with the real battery icon
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val scroll = findViewById<View>(R.id.scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }

        val prefs = Prefs.get(this)
        preview = findViewById(R.id.preview)
        toggle = findViewById(R.id.btnToggle)
        status = findViewById(R.id.tvStatus)
        preview.applyPrefs(prefs)

        bindSlider(R.id.slDensity, R.id.tvDensity, Prefs.DENSITY, 60, "Snow amount") { "$it" }
        bindSlider(R.id.slSpeed, R.id.tvSpeed, Prefs.SPEED, 10, "Fall speed") { "%.1fx".format(it / 10f) }
        bindSlider(R.id.slSize, R.id.tvSize, Prefs.SIZE, 10, "Flake size") { "%.1fx".format(it / 10f) }
        bindSlider(R.id.slCapX, R.id.tvCapX, Prefs.CAP_X, 22, "Cap position (from right)") { "$it dp" }
        bindSlider(R.id.slCapY, R.id.tvCapY, Prefs.CAP_Y, 8, "Cap height (from top)") { "$it dp" }
        bindSlider(R.id.slCapW, R.id.tvCapW, Prefs.CAP_W, 26, "Cap width") { "$it dp" }

        bindSwitch(R.id.swLow, Prefs.LOW_POWER, false)
        bindSwitch(R.id.swAuto, Prefs.AUTO_STOP, true)
        bindSwitch(R.id.swCap, Prefs.CAP_ON, true)

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

    private fun bindSwitch(id: Int, key: String, def: Boolean) {
        val prefs = Prefs.get(this)
        val s = findViewById<MaterialSwitch>(id)
        s.isChecked = prefs.getBoolean(key, def)
        s.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(key, checked).apply()
            preview.applyPrefs(prefs)
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
        s.value = v.toFloat().coerceIn(s.valueFrom, s.valueTo)
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
        Prefs.pauseReason(this)?.let {
            Toast.makeText(
                this, "$it. Snow stays off. (Turn off \"Auto-stop\" to run anyway)",
                Toast.LENGTH_LONG
            ).show()
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
