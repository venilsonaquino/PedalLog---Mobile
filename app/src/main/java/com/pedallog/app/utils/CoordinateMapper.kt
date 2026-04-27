package com.pedallog.app.utils

import com.pedallog.app.domain.model.PedalPoint

/**
 * Mapeia coordenadas geográficas para pixels na tela.
 */
class CoordinateMapper(
    private val width: Int,
    private val height: Int,
    private val padding: Float,
    points: List<PedalPoint>
) {
    private val minLat = points.minOf { it.latitude }
    private val minLon = points.minOf { it.longitude }
    private val scale: Double

    init {
        val latRange = points.maxOf { it.latitude } - minLat
        val lonRange = points.maxOf { it.longitude } - minLon
        scale = if (latRange == 0.0 || lonRange == 0.0) 1.0 
                else Math.min((width - 2 * padding) / lonRange, (height - 2 * padding) / latRange)
    }

    fun getX(lon: Double) = (padding + (lon - minLon) * scale).toFloat()
    fun getY(lat: Double) = (height - (padding + (lat - minLat) * scale)).toFloat()
}
