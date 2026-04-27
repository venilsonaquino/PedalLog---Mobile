package com.pedallog.app.domain.model

/**
 * Responsável por calcular métricas de uma sessão de pedal a partir de seus pontos GPS.
 *
 * SRP: Cálculos complexos devem ser isolados do modelo de dados.
 * Funções puras que facilitam testes de unidade.
 */
object SessionMetricsCalculator {

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
