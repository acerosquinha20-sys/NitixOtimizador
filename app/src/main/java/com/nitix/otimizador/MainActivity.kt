package com.nitix.otimizador

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.animation.OvershootInterpolator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private val pool = Executors.newCachedThreadPool()
    private lateinit var stage: FrameLayout
    private var homeOn = false
    private var statsView: TextView? = null
    private var logView: TextView? = null
    private var shText: TextView? = null
    private var shBtn: TextView? = null
    private val logLines = ArrayDeque<String>()
    private val switches = mutableMapOf<String, Switch>()

    private val binderL = Shizuku.OnBinderReceivedListener { Shell.bind(applicationContext); ui.post { updateShizuku() } }
    private val deadL = Shizuku.OnBinderDeadListener { ui.post { updateShizuku() } }
    private val permL = Shizuku.OnRequestPermissionResultListener { _, res ->
        if (res == PackageManager.PERMISSION_GRANTED) Shell.bind(applicationContext)
        ui.post { updateShizuku() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Store.init(this)
        window.statusBarColor = BG1
        window.navigationBarColor = BG1
        val outer = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(BG1, BG2))
        }
        outer.addView(Aurora(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        outer.addView(View(this).apply { background = noise() }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        stage = FrameLayout(this)
        outer.addView(stage, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(outer)

        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        Shizuku.addBinderReceivedListenerSticky(binderL)
        Shizuku.addBinderDeadListener(deadL)
        Shizuku.addRequestPermissionResultListener(permL)
        Shell.onChange = { ui.post { updateShizuku() } }
        Shell.bind(applicationContext)
        showWelcome()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderL)
        Shizuku.removeBinderDeadListener(deadL)
        Shizuku.removeRequestPermissionResultListener(permL)
        Shell.onChange = null
        homeOn = false
        super.onDestroy()
    }

    override fun onResume() { super.onResume(); if (homeOn) { updateShizuku(); ui.post(statsTick) } }
    override fun onPause() { ui.removeCallbacks(statsTick); super.onPause() }

    private fun setScreen(v: View) { stage.removeAllViews(); stage.addView(v, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // ---------------------------------------------------------------- BOAS-VINDAS
    private fun logo(sizeDp: Int) = ImageView(this).apply {
        setImageResource(R.drawable.ic_logo)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    private fun showWelcome() {
        homeOn = false
        val v = FrameLayout(this)
        v.addView(Sparkles(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(32), 0, dp(32), 0)
        }
        val logo = logo(132)
        val hello = tv("BEM-VINDO AO", 13f, SUB, true).apply { letterSpacing = 0.35f; gravity = Gravity.CENTER }
        val title = tv("NITIX", 54f, TXT, true).apply {
            letterSpacing = 0.22f; gravity = Gravity.CENTER
            post { paint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, ACC, ACC2, Shader.TileMode.CLAMP); invalidate() }
        }
        val sub = tv("OTIMIZADOR GAMER", 13f, ACC2, true).apply { letterSpacing = 0.3f; gravity = Gravity.CENTER }
        val tag = tv("Mais FPS. Menos lag. Mais espaço.", 15f, SUB).apply { gravity = Gravity.CENTER }
        val chips = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            for (s in listOf("⚡ FPS", "📶 PING", "🧹 LIMPEZA")) {
                addView(tv(s, 12f, TXT, true).apply {
                    background = rounded(0x26FFFFFF, 20, STROKE); setPadding(dp(12), dp(6), dp(12), dp(6))
                }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4); rightMargin = dp(4) })
            }
        }
        val go = btn("Começar") { if (Vip.active()) showHome() else showVip() }
        col.addView(logo)
        col.addView(hello, lpm(22)); col.addView(title, lpm(0)); col.addView(sub, lpm(2))
        col.addView(tag, lpm(18)); col.addView(chips, lpm(18)); col.addView(go, lpm(36))
        v.addView(col, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setScreen(v)

        // entrada escalonada
        val items = listOf<View>(logo, hello, title, sub, tag, chips, go)
        items.forEachIndexed { i, view ->
            view.alpha = 0f; view.translationY = dp(24).toFloat()
            view.animate().alpha(1f).translationY(0f).setStartDelay(i * 120L).setDuration(600).start()
        }
        logo.scaleX = 0.5f; logo.scaleY = 0.5f
        logo.animate().scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(2f)).setStartDelay(0).setDuration(800).start()
        ObjectAnimator.ofPropertyValuesHolder(
            logo, PropertyValuesHolder.ofFloat("scaleX", 1f, 1.07f), PropertyValuesHolder.ofFloat("scaleY", 1f, 1.07f)
        ).apply { duration = 1600; startDelay = 1200; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE; start() }
    }

        // ---------------------------------------------------------------- VIP
    private fun showVip() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), 0, dp(24), 0)
        }
        val card = cardBox()
        card.addView(tv("Acesso VIP", 22f, TXT, true))
        card.addView(tv("Digite seu código de ativação.", 13f, SUB).apply { setPadding(0, dp(4), 0, dp(12)) })
        val et = EditText(this).apply {
            hint = "CÓDIGO VIP"; setHintTextColor(SUB); setTextColor(TXT); isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            background = rounded(0x26FFFFFF, 12, STROKE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val msg = tv("", 13f, BAD)
        card.addView(et, lpm())
        card.addView(msg, lpm(6))
        card.addView(btn("Ativar VIP") {
            if (Vip.check(et.text.toString())) { Store.put("vip", true); showHome() } else msg.text = "Código inválido."
        }, lpm(10))
        col.addView(logo(88).apply { (layoutParams as LinearLayout.LayoutParams).bottomMargin = dp(18) })
        col.addView(card, lpm())
        setScreen(col)
    }

    // ---------------------------------------------------------------- HOME
    private fun showHome() {
        homeOn = true; switches.clear()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(40))
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(logo(38).apply { (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(10) })
        head.addView(tv("NITIX", 26f, TXT, true).apply { letterSpacing = 0.2f })
        head.addView(tv("VIP", 11f, 0xFF1A1033.toInt(), true).apply {
            background = rounded(0xFFFBBF24.toInt(), 8); setPadding(dp(8), dp(2), dp(8), dp(2))
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(10) })
        col.addView(head, lpm())

        // Shizuku
        val sh = cardBox()
        shText = tv("", 14f, TXT, true)
        sh.addView(shText)
        sh.addView(tv("Necessário para as otimizações do sistema.", 12f, SUB))
        shBtn = btn("Autorizar", false) { shizukuAction() }
        sh.addView(shBtn, lpm(10))
        col.addView(sh, lpm(14)); updateShizuku()

        // Jogo
        val gc = cardBox()
        val sel = GAMES.firstOrNull { it.pkg == Store.getStr("game") }
        if (sel == null) {
            gc.addView(tv("Nenhum jogo adicionado", 14f, SUB))
            gc.addView(btn("+  Adicionar jogo") { pickGame() }, lpm(10))
        } else {
            val inst = installed(packageManager, sel.pkg)
            gc.addView(tv(sel.name, 18f, TXT, true))
            gc.addView(tv(if (inst) "Instalado ✓" else "Não instalado", 12f, if (inst) OK else BAD))
            gc.addView(btn("⚡  Otimizar e abrir") { boostAll(true) }, lpm(10))
            gc.addView(btn("Trocar jogo", false) { pickGame() }, lpm(8))
        }
        col.addView(gc, lpm(12))

        // Status ao vivo
        val st = cardBox()
        statsView = tv("…", 14f, TXT, true)
        st.addView(tv("AO VIVO", 11f, ACC2, true).apply { letterSpacing = 0.2f })
        st.addView(statsView)
        col.addView(st, lpm(12))

        // Overlay + auto-limpeza
        val sw = cardBox()
        sw.addView(simpleRow("Overlay flutuante", "Ping, FPS, temperatura e RAM por cima do jogo.", "overlay") { on ->
            if (on && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                toast("Permita 'Exibir sobre outros apps' e ative de novo."); false
            } else true
        })
        sw.addView(simpleRow("Auto-limpeza", "A cada 5 min libera RAM e cache (precisa do Shizuku).", "auto") { true })
        col.addView(sw, lpm(12))

        col.addView(btn("⚡  OTIMIZAR TUDO") { boostAll(false) }, lpm(14))
        col.addView(btn("↺  Reverter tudo", false) { revertAll() }, lpm(8))

        // Funções por categoria
        for (cat in ALL.map { it.cat }.distinct()) {
            col.addView(tv(cat.uppercase(), 11f, ACC2, true).apply { letterSpacing = 0.2f }, lpm(18))
            val c = cardBox()
            ALL.filter { it.cat == cat }.forEach { c.addView(row(it)) }
            col.addView(c, lpm(6))
        }

        // Log
        logView = tv(logLines.joinToString("\n").ifEmpty { "Log de ações aparece aqui." }, 11.5f, SUB)
        val lc = cardBox(); lc.addView(logView)
        col.addView(lc, lpm(18))

        setScreen(ScrollView(this).apply { addView(col) })
        ui.removeCallbacks(statsTick); ui.post(statsTick)
    }

    private fun simpleRow(title: String, desc: String, key: String, allow: (Boolean) -> Boolean): View {
        val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(tv(title, 14f, TXT, true)); c.addView(tv(desc, 11.5f, SUB))
        r.addView(c, LinearLayout.LayoutParams(0, -2, 1f))
        val s = Switch(this).apply { isChecked = Store.getBool(key) }
        s.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(b: CompoundButton, on: Boolean) {
                if (allow(on)) { Store.put(key, on); NitixService.sync(applicationContext) }
                else { b.setOnCheckedChangeListener(null); b.isChecked = !on; b.setOnCheckedChangeListener(this) }
            }
        })
        r.addView(s)
        return r
    }

    private fun row(a: Act): View {
        val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(tv(a.title, 14f, TXT, true)); c.addView(tv(a.desc, 11.5f, SUB))
        r.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        if (a.oneShot) {
            r.addView(btn("Rodar", false) { runOne(a) }.apply { setPadding(dp(14), dp(8), dp(14), dp(8)); textSize = 12f })
        } else {
            val s = Switch(this).apply { isChecked = Store.getBool("act_${a.id}") }
            val l = object : CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(b: CompoundButton, on: Boolean) {
                    runToggle(a, on) { ok ->
                        if (!ok) { b.setOnCheckedChangeListener(null); b.isChecked = !on; b.setOnCheckedChangeListener(this) }
                    }
                }
            }
            s.tag = l; s.setOnCheckedChangeListener(l)
            switches[a.id] = s
            r.addView(s)
        }
        return r
    }

    // ---------------------------------------------------------------- AÇÕES
    private fun game() = Store.getStr("game")

    private fun runToggle(a: Act, on: Boolean, done: (Boolean) -> Unit) {
        pool.execute {
            val r = Optimizer.apply(this, a, on, game())
            if (r == "ok") Store.put("act_${a.id}", on)
            ui.post {
                log((if (r == "ok") (if (on) "✓ " else "↺ ") else "✗ ") + a.title + (if (r != "ok") " — $r" else ""))
                done(r == "ok")
            }
        }
    }

    private fun runOne(a: Act) {
        log("Executando: ${a.title}…")
        pool.execute {
            val r = Optimizer.apply(this, a, true, game())
            ui.post { log((if (r == "ok") "✓ " else "✗ ") + a.title + (if (r != "ok") " — $r" else "")) }
        }
    }

    private fun syncSwitches() {
        for ((id, s) in switches) {
            val l = s.tag as? CompoundButton.OnCheckedChangeListener
            s.setOnCheckedChangeListener(null); s.isChecked = Store.getBool("act_$id"); s.setOnCheckedChangeListener(l)
        }
    }

    private fun boostAll(open: Boolean) {
        val pkg = game()
        if (!Shell.ready) { log("Shizuku não conectado — autorize no topo."); return }
        if (pkg.isEmpty()) { log("Adicione o jogo primeiro."); return }
        log("Otimizando…")
        pool.execute {
            Optimizer.boost(this, pkg) { m -> ui.post { log(m) } }
            ui.post {
                log("Pronto ✓"); syncSwitches()
                if (open) packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) } ?: toast("Jogo não instalado")
            }
        }
    }

    private fun revertAll() {
        if (!Shell.ready) { log("Shizuku não conectado."); return }
        pool.execute {
            Optimizer.revert(this, game()) { m -> ui.post { log(m) } }
            ui.post { log("Tudo revertido."); syncSwitches() }
        }
    }

    private fun pickGame() {
        val names = GAMES.map { it.name + if (installed(packageManager, it.pkg)) "  ✓ instalado" else "  (não instalado)" }
        AlertDialog.Builder(this).setTitle("Escolha o jogo")
            .setItems(names.toTypedArray()) { _, i -> Store.put("game", GAMES[i].pkg); showHome() }.show()
    }

    private fun shizukuAction() {
        if (!Shell.available()) {
            packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let { startActivity(it) }
                ?: toast("Instale o Shizuku e inicie o serviço.")
        } else if (!Shell.granted()) Shizuku.requestPermission(100)
        else Shell.bind(applicationContext)
    }

    private fun updateShizuku() {
        shText?.text = when {
            Shell.ready -> "Shizuku conectado ✓"
            Shell.granted() -> "Shizuku autorizado — conectando…"
            Shell.available() -> "Shizuku rodando — falta autorizar"
            else -> "Shizuku não está rodando"
        }
        shText?.setTextColor(if (Shell.ready) OK else BAD)
        shBtn?.visibility = if (Shell.ready) View.GONE else View.VISIBLE
        shBtn?.text = when { !Shell.available() -> "Abrir Shizuku"; !Shell.granted() -> "Autorizar"; else -> "Conectar" }
    }

    private fun log(s: String) {
        logLines.addFirst(s); while (logLines.size > 14) logLines.removeLast()
        logView?.text = logLines.joinToString("\n")
    }

    private val statsTick = object : Runnable {
        override fun run() {
            if (!homeOn) return
            pool.execute {
                val p = Monitor.ping(); val t = Monitor.temp(this@MainActivity)
                val r = Monitor.ramPct(this@MainActivity); val g = Monitor.freeGb()
                ui.post {
                    statsView?.text = "PING ${if (p < 0) "--" else "$p ms"}   •   TEMP ${"%.1f".format(t)}°C\n" +
                        "RAM $r%   •   LIVRE ${"%.1f".format(g)} GB"
                }
            }
            ui.postDelayed(this, 2500)
        }
    }
}
