package com.caogenda

import com.google.firebase.Timestamp
import java.util.Calendar

data class Pet(
    val id: String = "",
    val nome: String = "",
    val especie: String = "",
    val raca: String = "",
    val dataNascimento: Long = 0,
    val peso: Double = 0.0,
    val fotoPath: String? = null,
    val criadoEm: Timestamp = Timestamp.now()
) {
    fun calcularIdade(): String {
        if (dataNascimento == 0L) return "Idade desconhecida"
        
        val birth = Calendar.getInstance().apply { timeInMillis = dataNascimento }
        val now = Calendar.getInstance()
        
        var years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            years--
        }
        
        return if (years > 0) {
            if (years == 1) "1 ano" else "$years anos"
        } else {
            val months = now.get(Calendar.MONTH) - birth.get(Calendar.MONTH) + 
                         (12 * (now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)))
            if (months <= 0) "Recém-nascido" else "$months meses"
        }
    }
}