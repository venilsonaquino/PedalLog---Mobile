package com.pedallog.app.modules.session.presentation.viewmodels

import com.pedallog.app.modules.session.domain.entities.PedalSession

/**
 * Representa um ponto de rastro simplificado para exibição no gráfico/mapa.
 */
data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val speedKmH: Float,
    val timeOffsetMs: Long
)

/**
 * DTO para exibir detalhes completos de uma sessão na UI.
 */
data class SessionMetrics(
    val session: PedalSession,
    val geoJson: String?,
    val gpsPointsCount: Int,
    val maxSpeedKmH: Float,
    val avgSpeedKmH: Float,
    val formattedDuration: String,
    val formattedDistance: String,
    val trackPoints: List<TrackPoint> = emptyList()
)

/**
 * Eventos de UI disparados pelas ViewModels.
 */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShareGif(val uri: android.net.Uri) : UiEvent()
}
