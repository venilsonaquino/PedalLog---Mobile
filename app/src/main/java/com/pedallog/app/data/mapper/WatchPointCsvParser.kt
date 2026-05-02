package com.pedallog.app.data.mapper

import com.pedallog.app.domain.model.PedalPoint

/**
 * Parser especializado para o formato de pontos GPS simplificado enviado pelo WearOS.
 * Formato: lat, lon, speed, dist, ts
 */
object WatchPointCsvParser {

    fun parse(csv: String): List<PedalPoint> {
        val lines = csv.split("\n").filter { it.isNotBlank() }
        val points = mutableListOf<PedalPoint>()

        for (line in lines) {
            val parts = line.split(",")
            if (parts.size >= 5) {
                points.add(
                    PedalPoint(
                        latitude = parts[0].toDoubleOrNull() ?: 0.0,
                        longitude = parts[1].toDoubleOrNull() ?: 0.0,
                        speed = parts[2].toFloatOrNull() ?: 0f,
                        timestamp = parts[4].toLongOrNull() ?: 0L,
                        altitude = 0.0,
                        accuracy = 0f,
                        segmentBreak = parts.getOrNull(5)?.toIntOrNull() ?: 0
                    )
                )
            }
        }
        return points
    }
}
