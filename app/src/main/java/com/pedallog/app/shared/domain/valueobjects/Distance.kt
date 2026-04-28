package com.pedallog.app.shared.domain.valueobjects

import java.util.Locale

/**
 * Representa uma distância percorrida em quilômetros.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 */
@JvmInline
value class Distance(val kilometers: Double) {
    init {
        require(kilometers >= 0) { "A distância não pode ser negativa" }
    }

    val meters: Double get() = kilometers * 1000.0

    fun toFormattedString(): String {
        return String.format(Locale.getDefault(), "%.2f km", kilometers)
    }
}
