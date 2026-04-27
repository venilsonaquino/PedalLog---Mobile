package com.pedallog.app.domain.model

import java.util.Locale

data class PedalSession(
    val syncUuid: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val averageSpeed: Float,
    val totalAscent: Double,
    val totalDescent: Double,
    val activeDurationMs: Long = 0
) {
    val durationMs: Long
        get() = if (activeDurationMs > 0) activeDurationMs 
                else if (endTime > startTime) endTime - startTime 
                else 0L


    fun calculateMaxSpeedKmH(points: List<PedalPoint>): Float {
        if (points.isEmpty()) return 0f
        // speed já vem em km/h do WearOS — sem conversão adicional
        return points.maxOf { it.speed }
    }

    fun calculateAverageSpeedKmH(points: List<PedalPoint>): Float {
        if (points.isEmpty()) return 0f
        // speed já vem em km/h do WearOS — sem conversão adicional
        return points.map { it.speed }.average().toFloat()
    }
}
