package com.pedallog.app.shared.domain.logic

import com.pedallog.app.modules.tracking.domain.entities.PedalPoint

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
        return points.maxOf { it.details.speed.kilometersPerHour }
    }

    fun calculateAverageSpeedKmH(points: List<PedalPoint>): Float {
        if (points.isEmpty()) return 0f
        // speed já vem em km/h do WearOS — sem conversão adicional
        return points.map { it.details.speed.kilometersPerHour }.average().toFloat()
    }
}
