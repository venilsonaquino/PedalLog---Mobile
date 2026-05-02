package com.pedallog.app.domain.model

data class PedalPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val accuracy: Float,
    val segmentBreak: Int = 0
)
