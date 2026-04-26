package com.pedallog.app.domain.model

import java.util.Locale

data class PedalSession(
    val syncUuid: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val averageSpeed: Float,
    val totalAscent: Double,
    val totalDescent: Double
) {
    val durationMs: Long
        get() = if (endTime > startTime) endTime - startTime else 0L

    fun getFormattedDuration(): String {
        if (durationMs <= 0) return "00:00:00"
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getFormattedDistance(): String {
        return String.format(Locale.US, "%.2f km", distanceKm)
    }

    fun calculateMaxSpeedKmH(points: List<PedalPoint>): Float {
        if (points.isEmpty()) return 0f
        return points.maxOf { it.speed } * 3.6f
    }

    fun calculateAverageSpeedKmH(points: List<PedalPoint>): Float {
        if (points.isEmpty()) return 0f
        return points.map { it.speed }.average().toFloat() * 3.6f
    }
}
