package com.nitix.otimizador

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

data class Game(val name: String, val pkg: String)

val GAMES = listOf(
    Game("Free Fire", "com.dts.freefireth"),
    Game("Free Fire MAX", "com.dts.freefiremax")
)

fun installed(pm: PackageManager, pkg: String) =
    try { pm.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }

/** Uma função de otimização. %PKG% vira o pacote do jogo escolhido. */
data class Act(
    val id: String, val cat: String, val title: String, val desc: String,
    val on: List<String>, val off: List<String> = emptyList(),
    val oneShot: Boolean = false, val boost: Boolean = false
)

private fun t(id: String, cat: String, title: String, desc: String, on: List<String>, off: List<String>, boost: Boolean = false) =
    Act(id, cat, title, desc, on, off, false, boost)
private fun o(id: String, cat: String, title: String, desc: String, cmds: List<String>, boost: Boolean = false) =
    Act(id, cat, title, desc, cmds, emptyList(), true, boost)

val ALL: List<Act> = listOf(
    // ---------- JOGO ----------
    t("game_perf", "Jogo", "Modo Game: Desempenho", "Pede ao Android o perfil de desempenho pro jogo (Android 12+).",
        listOf("cmd game mode performance %PKG%"), listOf("cmd game mode standard %PKG%"), true),
    t("idle", "Jogo", "Livrar do Doze", "Impede o sistema de suspender o jogo.",
        listOf("cmd deviceidle whitelist +%PKG%"), listOf("cmd deviceidle whitelist -%PKG%"), true),
    t("bg", "Jogo", "Permitir segundo plano", "Jogo não é encerrado ao minimizar.",
        listOf("cmd appops set %PKG% RUN_IN_BACKGROUND allow"), listOf("cmd appops set %PKG% RUN_IN_BACKGROUND default"), true),
    t("bg_any", "Jogo", "Sem limite de bateria", "Remove a restrição de bateria do jogo.",
        listOf("cmd appops set %PKG% RUN_ANY_IN_BACKGROUND allow"), listOf("cmd appops set %PKG% RUN_ANY_IN_BACKGROUND default"), true),
    t("bucket", "Jogo", "Prioridade ativa", "Coloca o jogo no grupo de maior prioridade.",
        listOf("am set-standby-bucket %PKG% active"), listOf("am set-standby-bucket %PKG% working_set"), true),
    t("fixed", "Jogo", "Desempenho fixo", "Trava o perfil de desempenho (nem todo aparelho suporta).",
        listOf("cmd power set-fixed-performance-mode-enabled true"), listOf("cmd power set-fixed-performance-mode-enabled false")),
    o("dexopt", "Jogo", "Compilar o jogo", "Recompila o jogo (speed-profile). Pode levar um tempo.",
        listOf("cmd package compile -m speed-profile -f %PKG%")),

    // ---------- SISTEMA E TELA ----------
    t("refresh", "Sistema e tela", "Taxa de atualização alta", "Força 90–120 Hz quando a tela suporta.",
        listOf("settings put system min_refresh_rate 90.0", "settings put system peak_refresh_rate 120.0"),
        listOf("settings delete system min_refresh_rate", "settings delete system peak_refresh_rate"), true),
    t("anim05", "Sistema e tela", "Animações 0.5x", "Interface do sistema mais ágil.",
        listOf("settings put global window_animation_scale 0.5", "settings put global transition_animation_scale 0.5", "settings put global animator_duration_scale 0.5"),
        listOf("settings put global window_animation_scale 1.0", "settings put global transition_animation_scale 1.0", "settings put global animator_duration_scale 1.0"), true),
    t("anim0", "Sistema e tela", "Animações desligadas", "Zero animações (substitui o 0.5x).",
        listOf("settings put global window_animation_scale 0", "settings put global transition_animation_scale 0", "settings put global animator_duration_scale 0"),
        listOf("settings put global window_animation_scale 1.0", "settings put global transition_animation_scale 1.0", "settings put global animator_duration_scale 1.0")),
    t("touch", "Sistema e tela", "Toque mais rápido", "Reduz o tempo de toque longo.",
        listOf("settings put secure long_press_timeout 250", "settings put secure multi_press_timeout 250"),
        listOf("settings put secure long_press_timeout 400", "settings put secure multi_press_timeout 300"), true),
    t("headsup", "Sistema e tela", "Sem pop-ups de notificação", "Notificações não cobrem a tela do jogo.",
        listOf("settings put global heads_up_notifications_enabled 0"), listOf("settings put global heads_up_notifications_enabled 1"), true),
    t("dnd", "Sistema e tela", "Não Perturbe", "Silencia interrupções enquanto joga.",
        listOf("cmd notification set_dnd priority"), listOf("cmd notification set_dnd off")),
    t("adaptive", "Sistema e tela", "Sem bateria adaptável", "Evita que o sistema limite apps por bateria.",
        listOf("settings put global adaptive_battery_management_enabled 0"), listOf("settings put global adaptive_battery_management_enabled 1")),
    t("lowpower", "Sistema e tela", "Desligar economia de bateria", "Garante o desempenho máximo.",
        listOf("settings put global low_power 0"), emptyList()),
    t("stayon", "Sistema e tela", "Tela ligada no carregador", "A tela não apaga enquanto carrega.",
        listOf("settings put global stay_on_while_plugged_in 3"), listOf("settings put global stay_on_while_plugged_in 0")),

    // ---------- REDE ----------
    t("wifi_ll", "Rede", "Wi-Fi baixa latência", "Prioriza latência no Wi-Fi (Android 11+).",
        listOf("cmd wifi force-low-latency-mode enabled"), listOf("cmd wifi force-low-latency-mode disabled"), true),
    t("wifi_hp", "Rede", "Wi-Fi alto desempenho", "Mantém o Wi-Fi em modo de alto desempenho.",
        listOf("cmd wifi force-hi-perf-mode enabled"), listOf("cmd wifi force-hi-perf-mode disabled")),
    t("wifiscan", "Rede", "Sem busca Wi-Fi em 2º plano", "Menos interferência e consumo.",
        listOf("settings put global wifi_scan_always_enabled 0"), listOf("settings put global wifi_scan_always_enabled 1"), true),
    t("blescan", "Rede", "Sem busca Bluetooth em 2º plano", "Menos processamento em segundo plano.",
        listOf("settings put global ble_scan_always_enabled 0"), listOf("settings put global ble_scan_always_enabled 1")),
    t("dns", "Rede", "DNS privado Cloudflare", "Pode ajudar em algumas redes; o ping não é garantido.",
        listOf("settings put global private_dns_mode hostname", "settings put global private_dns_specifier one.one.one.one"),
        listOf("settings put global private_dns_mode opportunistic")),

    // ---------- LIMPEZA ----------
    o("kill", "Limpeza", "Encerrar processos em 2º plano", "Libera RAM sem fechar o app que está aberto.",
        listOf("am kill-all"), true),
    o("trim", "Limpeza", "Liberar cache de todos os apps", "Libera armazenamento limpando o cache do sistema.",
        listOf("pm trim-caches 999999999999"), true),
    o("owncache", "Limpeza", "Limpar cache do Nitix", "Apaga o cache deste app.", emptyList()),
    o("killapps", "Limpeza", "Fechar apps de terceiros", "Força parada em tudo, menos jogo, teclado e launcher. Pode parar notificações até reabrir os apps.", emptyList()),
    o("trimfs", "Limpeza", "Manutenção do armazenamento", "Executa o TRIM/manutenção do sistema de arquivos.",
        listOf("sm idle-maint run")),
    o("bgdex", "Limpeza", "Otimizar apps (dexopt)", "Roda o job de otimização de apps do sistema.",
        listOf("cmd package bg-dexopt-job")),
    o("logs", "Limpeza", "Limpar logs do sistema", "Esvazia o logcat.", listOf("logcat -c"))
)

object Optimizer {
    private val err = Regex("(?i)(exception|error|unknown|denied|not found|invalid|ERR:|unsupported)")

    fun apply(ctx: Context, a: Act, on: Boolean, pkg: String): String {
        if (a.id == "owncache") {
            ctx.cacheDir.deleteRecursively(); ctx.externalCacheDir?.deleteRecursively(); return "ok"
        }
        if (!Shell.ready) return "Shizuku não conectado"
        val cmds = if (on) a.on else a.off
        if (cmds.any { it.contains("%PKG%") } && pkg.isEmpty()) return "adicione o jogo primeiro"
        if (a.id == "killapps") return killThirdParty(ctx, pkg)
        var bad = ""
        for (c in cmds) {
            val out = Shell.run(c.replace("%PKG%", pkg))
            if (err.containsMatchIn(out)) bad = out.lines().firstOrNull { it.isNotBlank() }?.take(60) ?: "erro"
        }
        return if (bad.isEmpty()) "ok" else bad
    }

    private fun killThirdParty(ctx: Context, game: String): String {
        val keep = mutableSetOf(ctx.packageName, game, "moe.shizuku.privileged.api")
        ctx.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName?.let { keep.add(it) }
        Settings.Secure.getString(ctx.contentResolver, "default_input_method")
            ?.substringBefore('/')?.let { keep.add(it) }
        Shell.run("pm list packages -3").lines()
            .map { it.removePrefix("package:").trim() }
            .filter { it.contains('.') && !it.contains(' ') && it !in keep }
            .forEach { Shell.run("am force-stop $it") }
        return "ok"
    }

    /** Liga o conjunto "seguro" de otimizações. */
    fun boost(ctx: Context, pkg: String, log: (String) -> Unit) {
        for (a in ALL.filter { it.boost }) {
            val r = apply(ctx, a, true, pkg)
            if (!a.oneShot && r == "ok") Store.put("act_${a.id}", true)
            log((if (r == "ok") "✓ " else "✗ ") + a.title + (if (r != "ok") " — $r" else ""))
        }
    }

    /** Desfaz tudo que está ligado. */
    fun revert(ctx: Context, pkg: String, log: (String) -> Unit) {
        for (a in ALL.filter { !it.oneShot && Store.getBool("act_${a.id}") }) {
            val r = apply(ctx, a, false, pkg)
            if (r == "ok") Store.put("act_${a.id}", false)
            log((if (r == "ok") "↺ " else "✗ ") + a.title + (if (r != "ok") " — $r" else ""))
        }
    }
}
