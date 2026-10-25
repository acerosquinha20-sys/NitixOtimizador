package com.nitix.otimizador

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.roundToInt

object Monitor {
    /** Ping aproximado: tempo de conexão TCP até a Cloudflare (1.1.1.1). */
    fun ping(): Int = try {
        val s = Socket()
        val t = System.nanoTime()
        s.connect(InetSocketAddress("1.1.1.1", 443), 1500)
        val ms = ((System.nanoTime() - t) / 1_000_000).toInt()
        s.close(); ms
    } catch (_: Throwable) { -1 }

    fun temp(c: Context): Float {
        val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
    }

    fun ramPct(c: Context): Int {
        val mi = ActivityManager.MemoryInfo()
        c.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
        return (100 * (mi.totalMem - mi.availMem) / mi.totalMem).toInt()
    }

    fun freeGb(): Float = StatFs(Environment.getDataDirectory().path).availableBytes / 1073741824f

    private var layer: String? = null
    private var layerPkg = ""
    private var layerAt = 0L

    /** FPS real do jogo, lido do SurfaceFlinger via Shizuku (somente leitura). */
    fun fps(pkg: String): Int? {
        val now = System.currentTimeMillis()
        if (layer == null || layerPkg != pkg || now - layerAt > 15_000) {
            val l = Shell.run("dumpsys SurfaceFlinger --list").lines().map { it.trim() }
            layer = l.firstOrNull { it.contains(pkg) && it.contains("SurfaceView") }
                ?: l.firstOrNull { it.contains(pkg) }
            layerPkg = pkg; layerAt = now
        }
        val name = layer ?: return null
        val out = Shell.run("dumpsys SurfaceFlinger --latency \"$name\"")
        val rows = out.lines().drop(1).mapNotNull {
            val p = it.trim().split(Regex("\\s+"))
            if (p.size >= 3) p[1].toLongOrNull() else null
        }.filter { it > 0 && it < Long.MAX_VALUE / 2 }
        if (rows.size < 2) return null
        val span = (rows.max() - rows.min()) / 1e9
        return if (span > 0) ((rows.size - 1) / span).roundToInt() else null
    }
}
