package com.pedallog.app.shared.domain.valueobjects

import java.util.concurrent.TimeUnit

/**
 * Representa uma duração de tempo em milissegundos.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 */
@JvmInline
value class Duration(val milliseconds: Long) {
    init {
        require(milliseconds >= 0) { "A duração não pode ser negativa" }
    }

    fun isZero(): Boolean = milliseconds <= 0L

    fun toMinutes(): Long = TimeUnit.MILLISECONDS.toMinutes(milliseconds)

    fun toFormattedString(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        
        if (hours > 0) return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        
        return String.format("%02dm %02ds", minutes, seconds)
    }
}
