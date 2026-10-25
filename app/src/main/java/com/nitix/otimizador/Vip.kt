package com.nitix.otimizador

import java.security.MessageDigest

/**
 * Ativação VIP offline (só guarda o hash dos códigos).
 * ATENÇÃO: qualquer verificação dentro do APK pode ser burlada.
 * Para VIP de verdade, valide o código num servidor seu.
 */
object Vip {
    private val valid = setOf(
        "485094b29e669a3b547076dd6466aef6cd55ee867828700d45dbfb02458973ab",
        "f62f570d23d7206674a259983af20f34d60b3be8869341e75802a87d8d6bf3fd"
    )
    private fun sha(s: String) = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun check(code: String) = sha(code.trim().uppercase()) in valid
    fun active() = Store.getBool("vip")
}
