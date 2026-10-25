package com.nitix.otimizador

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/** Ponte com o Shizuku: executa comandos como usuário shell. */
object Shell {
    @Volatile private var svc: IUserService? = null
    @Volatile private var bound = false
    val ready: Boolean get() = svc != null
    var onChange: (() -> Unit)? = null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            svc = if (binder != null && binder.pingBinder()) IUserService.Stub.asInterface(binder) else null
            onChange?.invoke()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            svc = null; bound = false; onChange?.invoke()
        }
    }

    fun available(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun granted(): Boolean = try {
        available() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun bind(ctx: Context) {
        if (ready) return
        if (!granted()) { bound = false; return }
        if (bound) return
        try {
            val args = Shizuku.UserServiceArgs(ComponentName(ctx.packageName, UserService::class.java.name))
                .processNameSuffix("shell").daemon(false).version(1)
            Shizuku.bindUserService(args, conn)
            bound = true
        } catch (_: Throwable) { bound = false }
    }

    fun run(cmd: String): String = try {
        svc?.exec(cmd) ?: "ERR: Shizuku não conectado"
    } catch (e: Throwable) { "ERR: ${e.message}" }
}
