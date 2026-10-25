package com.nitix.otimizador

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Html
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.concurrent.Executors

/** Mantém o overlay (ping/FPS/temp/RAM) e a auto-limpeza rodando. */
class NitixService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private val mon = Executors.newSingleThreadExecutor()
    private val clean = Executors.newSingleThreadExecutor()
    private var wm: WindowManager? = null
    private var box: TextView? = null
    private var alive = false
    private var lastClean = System.currentTimeMillis()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        Store.init(this)
        Shell.bind(applicationContext)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("nitix", "Nitix", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "nitix")
            .setContentTitle("Nitix ativo")
            .setContentText("Monitorando e otimizando")
            .setSmallIcon(android.R.drawable.stat_notify_sync).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        overlay(Store.getBool("overlay") && Settings.canDrawOverlays(this))
        if (!alive) { alive = true; h.post(tick) }
        return START_STICKY
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!alive) return
            if (!Shell.ready) Shell.bind(applicationContext)
            val pkg = Store.getStr("game")
            if (box != null) mon.execute {
                val ping = Monitor.ping()
                val fps = if (pkg.isNotEmpty() && Shell.ready) Monitor.fps(pkg) else null
                val temp = Monitor.temp(this@NitixService)
                val ram = Monitor.ramPct(this@NitixService)
                val pc = if (ping < 0) "#F87171" else if (ping < 60) "#34D399" else if (ping < 120) "#FBBF24" else "#F87171"
                val tc = if (temp >= 42) "#F87171" else "#FFFFFF"
                val html = "<font color='#22D3EE'><b>PING</b></font> <font color='$pc'>${if (ping < 0) "--" else "$ping ms"}</font><br>" +
                    "<font color='#A78BFA'><b>FPS</b></font> ${fps ?: "--"}<br>" +
                    "<font color='#A78BFA'><b>TEMP</b></font> <font color='$tc'>${"%.1f".format(temp)}°C</font><br>" +
                    "<font color='#A78BFA'><b>RAM</b></font> $ram%"
                h.post { box?.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY) }
            }
            if (Store.getBool("auto") && Shell.ready && System.currentTimeMillis() - lastClean > 300_000) {
                lastClean = System.currentTimeMillis()
                clean.execute { Shell.run("am kill-all"); Shell.run("pm trim-caches 999999999999") }
            }
            h.postDelayed(this, 1000)
        }
    }

    private fun overlay(on: Boolean) {
        if (on && box == null) {
            wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val d = resources.displayMetrics.density
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = Store.getInt("ox", (12 * d).toInt()); y = Store.getInt("oy", (40 * d).toInt())
            }
            val tv = TextView(this).apply {
                setTextColor(0xFFFFFFFF.toInt()); textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xB30A0614.toInt()); cornerRadius = 12 * d
                    setStroke((1 * d).toInt(), 0x558B5CF6)
                }
                setPadding((10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
                text = "PING --"
            }
            tv.setOnTouchListener(object : View.OnTouchListener {
                var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
                override fun onTouch(v: View, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY }
                        MotionEvent.ACTION_MOVE -> {
                            lp.x = sx + (e.rawX - tx).toInt(); lp.y = sy + (e.rawY - ty).toInt()
                            wm?.updateViewLayout(v, lp)
                        }
                        MotionEvent.ACTION_UP -> { Store.put("ox", lp.x); Store.put("oy", lp.y) }
                    }
                    return true
                }
            })
            try { wm?.addView(tv, lp); box = tv } catch (_: Throwable) {}
        } else if (!on && box != null) {
            try { wm?.removeView(box) } catch (_: Throwable) {}
            box = null
        }
    }

    override fun onDestroy() {
        alive = false; h.removeCallbacksAndMessages(null); overlay(false); super.onDestroy()
    }

    companion object {
        fun sync(c: Context) {
            Store.init(c)
            val i = Intent(c, NitixService::class.java)
            if (Store.getBool("overlay") || Store.getBool("auto")) c.startForegroundService(i) else c.stopService(i)
        }
    }
}
