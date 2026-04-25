package com.pedallog.app.data.mapper

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.io.BufferedReader

object GzipCsvUtils {

    fun decompressAndParsePoints(compressedData: ByteArray): List<PedalPoint> {
        val decompressedString = try {
            val bais = ByteArrayInputStream(compressedData)
            val gzipIn = GZIPInputStream(bais)
            val reader = BufferedReader(InputStreamReader(gzipIn, "UTF-8"))
            val text = reader.readText()
            reader.close()
            gzipIn.close()
            text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        return parsePointsCsvOnly(decompressedString)
    }

    private fun parsePointsCsvOnly(csv: String): List<PedalPoint> {
        val lines = csv.split("\n").filter { it.isNotBlank() }
        val points = mutableListOf<PedalPoint>()

        for (line in lines) {
            val parts = line.split(",")
            if (parts.size >= 5) {
                // Watch format: lat, lon, speed, dist, ts
                points.add(
                    PedalPoint(
                        latitude = parts[0].toDoubleOrNull() ?: 0.0,
                        longitude = parts[1].toDoubleOrNull() ?: 0.0,
                        speed = parts[2].toFloatOrNull() ?: 0f,
                        timestamp = parts[4].toLongOrNull() ?: 0L,
                        altitude = 0.0, // Watch doesn't send
                        accuracy = 0f  // Watch doesn't send
                    )
                )
            }
        }
        return points
    }

    fun decompressAndParse(compressedData: ByteArray): Pair<PedalSession, List<PedalPoint>> {
        val decompressedString = try {
            val bais = ByteArrayInputStream(compressedData)
            val gzipIn = GZIPInputStream(bais)
            val reader = BufferedReader(InputStreamReader(gzipIn, "UTF-8"))
            val text = reader.readText()
            reader.close()
            gzipIn.close()
            text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        return parseCsv(decompressedString)
    }

    private fun parseCsv(csv: String): Pair<PedalSession, List<PedalPoint>> {
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
                    // uuid, startTime, endTime, distanceKm, avgSpeed, totalAscent, totalDescent
                    val parts = line.split(",")
                    if (parts.size >= 7 && session == null) {
                        session = PedalSession(
                            syncUuid = parts[0],
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
                    // timestamp, lat, lon, alt, speed, accuracy
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
