package com.pedallog.app.modules.session.domain.valueobjects

import com.pedallog.app.shared.domain.valueobjects.Duration
import com.pedallog.app.shared.domain.valueobjects.Timestamp

/**
 * Encapsula o intervalo de tempo de uma sessão.
 * 
 * Object Calisthenics: Agrupamento para reduzir variáveis de instância.
 */
data class SessionTimeRange(
    val start: Timestamp,
    val end: Timestamp,
    val activeDuration: Duration
) {
    fun calculateTotalDuration(): Duration {
        if (!activeDuration.isZero()) return activeDuration
        
        val delta = end.milliseconds - start.milliseconds
        if (delta > 0) return Duration(delta)
        
        return Duration(0)
    }
}
