package com.pedallog.app.modules.session.domain.valueobjects

import com.pedallog.app.shared.domain.valueobjects.Distance

/**
 * Encapsula métricas de altimetria.
 */
data class ElevationMetrics(
    val totalAscent: Distance,
    val totalDescent: Distance
)
