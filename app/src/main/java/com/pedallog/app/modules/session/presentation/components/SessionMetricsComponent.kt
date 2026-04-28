package com.pedallog.app.modules.session.presentation.components

import com.pedallog.app.databinding.FragmentMapBinding
import com.pedallog.app.modules.session.presentation.viewmodels.SessionMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Componente responsável pela atualização dos elementos textuais de métricas.
 * 
 * SOLID: Isola a responsabilidade de "Data Binding" manual.
 */
class SessionMetricsComponent(private val binding: FragmentMapBinding) {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM · HH:mm", Locale("pt", "BR"))

    fun bind(metrics: SessionMetrics) {
        updateHeader(metrics)
        updateMainMetrics(metrics)
    }

    private fun updateHeader(metrics: SessionMetrics) {
        val session = metrics.session
        val time = session.details.timeRange.start.milliseconds
        
        binding.tvDetailTitle.text = dateFormat.format(Date(time))
            .replaceFirstChar { it.uppercase() }
        binding.tvTechnicalUuid.text = "UUID · ${session.id.value}"
    }

    private fun updateMainMetrics(metrics: SessionMetrics) {
        val distParts = metrics.formattedDistance.split(" ")
        binding.tvMetricDistanceValue.text = distParts.getOrNull(0) ?: "0.00"
        binding.tvMetricDistanceUnit.text  = distParts.getOrNull(1) ?: "km"

        binding.tvMetricDuration.text = metrics.formattedDuration
        binding.tvMetricAvgSpeedValue.text = String.format(Locale.US, "%.1f", metrics.avgSpeedKmH)
        binding.tvMetricMaxSpeedValue.text = String.format(Locale.US, "%.1f", metrics.maxSpeedKmH)
        binding.tvMetricPoints.text = metrics.gpsPointsCount.toString()
    }
}
