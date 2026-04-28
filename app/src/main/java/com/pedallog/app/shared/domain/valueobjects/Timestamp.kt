package com.pedallog.app.shared.domain.valueobjects

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representa um ponto no tempo em milissegundos.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 */
@JvmInline
value class Timestamp(val milliseconds: Long) {
    fun toFormattedDate(pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date(milliseconds))
    }
}
