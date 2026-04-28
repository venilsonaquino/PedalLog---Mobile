package com.pedallog.app.shared.utils

import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import com.pedallog.app.modules.tracking.domain.valueobjects.Coordinate
import com.pedallog.app.modules.tracking.domain.valueobjects.PointDetails
import com.pedallog.app.shared.domain.valueobjects.Speed
import com.pedallog.app.shared.domain.valueobjects.Timestamp
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Utilitário para descompressão GZIP e parsing de dados CSV recebidos do relógio.
 */
object GzipCsvUtils {
    fun decompressAndParsePoints(compressed: ByteArray): List<PedalPoint> {
        val points = mutableListOf<PedalPoint>()
        try {
            val bis = ByteArrayInputStream(compressed)
            val gis = GZIPInputStream(bis)
            val reader = BufferedReader(InputStreamReader(gis))
            
            // Assume format: lat,lng,spd,alt,time
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line?.split(",") ?: continue
                if (parts.size >= 5) {
                    points.add(PedalPoint(
                        coordinate = Coordinate(parts[0].toDouble(), parts[1].toDouble()),
                        details = PointDetails(
                            altitude = parts[3].toDouble(),
                            speed = Speed(parts[2].toFloat()),
                            timestamp = Timestamp(parts[4].toLong()),
                            accuracy = 0f
                        )
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }
}
