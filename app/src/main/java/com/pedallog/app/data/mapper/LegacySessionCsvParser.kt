package com.pedallog.app.data.mapper

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId

/**
 * Parser para o formato CSV completo (Sessão + Pontos) com delimitadores de seção.
 * Formato: 
 * ===SESSION===
 * uuid, startTime, endTime, distanceKm, avgSpeed, totalAscent, totalDescent
 * ===POINTS===
 * timestamp, lat, lon, alt, speed, accuracy
 */
object LegacySessionCsvParser {

    fun parse(csv: String): Pair<PedalSession, List<PedalPoint>> {
        val lines = csv.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) throw IllegalArgumentException("CSV is empty")

        var session: PedalSession? = null
        val points = mutableListOf<PedalPoint>()

        var isSessionSection = false
        var isPointSection = false

        for (line in lines) {
            when {
                line.startsWith("===SESSION===") -> {
                    isSessionSection = true
                    isPointSection = false
                }
                line.startsWith("===POINTS===") -> {
                    isSessionSection = false
                    isPointSection = true
                }
                isSessionSection -> {
                    val parts = line.split(",")
                    if (parts.size >= 7 && session == null) {
                        session = PedalSession(
                            syncUuid = SessionId(parts[0]),
                            startTime = parts[1].toLongOrNull() ?: 0L,
                            endTime = parts[2].toLongOrNull() ?: 0L,
                            distanceKm = parts[3].toDoubleOrNull() ?: 0.0,
                            averageSpeed = parts[4].toFloatOrNull() ?: 0f,
                            totalAscent = parts[5].toDoubleOrNull() ?: 0.0,
                            totalDescent = parts[6].toDoubleOrNull() ?: 0.0
                        )
                    }
                }
                isPointSection -> {
                    val parts = line.split(",")
                    if (parts.size >= 6) {
                        points.add(
                            PedalPoint(
                                timestamp = parts[0].toLongOrNull() ?: 0L,
                                latitude = parts[1].toDoubleOrNull() ?: 0.0,
                                longitude = parts[2].toDoubleOrNull() ?: 0.0,
                                altitude = parts[3].toDoubleOrNull() ?: 0.0,
                                speed = parts[4].toFloatOrNull() ?: 0f,
                                accuracy = parts[5].toFloatOrNull() ?: 0f
                            )
                        )
                    }
                }
            }
        }

        if (session == null) {
            throw IllegalArgumentException("No session data found in CSV")
        }

        return Pair(session, points)
    }
}
