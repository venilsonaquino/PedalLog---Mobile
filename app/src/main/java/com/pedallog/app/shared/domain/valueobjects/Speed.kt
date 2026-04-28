package com.pedallog.app.shared.domain.valueobjects

import java.util.Locale

/**
 * Representa uma velocidade em quilômetros por hora.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 */
@JvmInline
value class Speed(val kilometersPerHour: Float) {
    init {
        require(kilometersPerHour >= 0) { "A velocidade não pode ser negativa" }
    }

    val metersPerSecond: Float get() = kilometersPerHour / 3.6f

    fun toFormattedString(): String {
        return String.format(Locale.getDefault(), "%.1f km/h", kilometersPerHour)
    }
}
