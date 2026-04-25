package com.pedallog.app.domain.model

data class PedalSession(
    val syncUuid: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val averageSpeed: Float,
    val totalAscent: Double,
    val totalDescent: Double
)
