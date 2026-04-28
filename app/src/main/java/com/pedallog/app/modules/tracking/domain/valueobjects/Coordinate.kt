package com.pedallog.app.modules.tracking.domain.valueobjects

/**
 * Representa uma coordenada geográfica.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)
