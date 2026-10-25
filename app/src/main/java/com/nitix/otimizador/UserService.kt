package com.nitix.otimizador

/** Roda dentro do processo do Shizuku (usuário shell). */
class UserService : IUserService.Stub() {
    override fun destroy() { System.exit(0) }

    override fun exec(cmd: String): String = try {
        val p = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }
}
