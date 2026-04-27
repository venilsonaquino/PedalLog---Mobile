package com.pedallog.app.domain.model

import java.util.Locale

data class PedalSession(
    val syncUuid: SessionId,
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


}

