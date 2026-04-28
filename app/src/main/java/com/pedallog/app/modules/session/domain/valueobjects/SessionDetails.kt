package com.pedallog.app.modules.session.domain.valueobjects

/**
 * Agrupa todos os detalhes técnicos e de desempenho de uma sessão.
 * 
 * Object Calisthenics: Auxilia na regra de máximo de 2 variáveis por instância.
 */
data class SessionDetails(
    val timeRange: SessionTimeRange,
    val metrics: SessionMetrics
)
