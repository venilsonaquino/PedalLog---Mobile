package com.pedallog.app.modules.tracking.domain.valueobjects

import com.pedallog.app.shared.domain.valueobjects.Speed
import com.pedallog.app.shared.domain.valueobjects.Timestamp

/**
 * Detalhes técnicos de um ponto de GPS.
 */
data class PointDetails(
    val altitude: Double,
    val speed: Speed,
    val timestamp: Timestamp,
    val accuracy: Float
)
