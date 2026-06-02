package com.caogenda

import com.google.firebase.Timestamp

data class Vacina(
    val id: String = "",
    val nome: String = "",
    val dataAplicacao: Long = 0,
    val veterinario: String = "",
    val observacoes: String = "",
    val proximaDose: Long = 0,
    val status: String = "Aplicada",
    val criadoEm: Timestamp = Timestamp.now(),
    // Campos auxiliares para a Home
    var petId: String = "",
    var nomePet: String = "",
    var fotoPetPath: String? = null
) {
    fun calcularDiasRestantes(): Long {
        if (proximaDose == 0L) return 0
        val diff = proximaDose - System.currentTimeMillis()
        return if (diff > 0) diff / (24 * 60 * 60 * 1000) else 0
    }
}