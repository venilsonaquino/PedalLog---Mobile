package com.pedallog.app.modules.session.domain.valueobjects

import com.pedallog.app.shared.domain.valueobjects.Distance
import com.pedallog.app.shared.domain.valueobjects.Speed

/**
 * Agrupa as métricas de desempenho da sessão.
 */
data class SessionMetrics(
    val distance: Distance,
    val averageSpeed: Speed,
    val elevation: ElevationMetrics
)
