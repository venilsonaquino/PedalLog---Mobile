package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Generates a GPX 1.1 document from a [PedalSession] and its [PedalPoint] list.
 *
 * The output is a valid GPX file compatible with Strava, Komoot, Garmin Connect, etc.
 */
class ExportGpxUseCase {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    operator fun invoke(session: PedalSession, points: List<PedalPoint>): String {
        val createdAt = isoFormat.format(Date(session.startTime))
        val name = "PedalLog ${createdAt.substring(0, 10)}" // e.g. "PedalLog 2026-04-26"

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine(
            """<gpx version="1.1" creator="PedalLog Mobile"
  xmlns="http://www.topografix.com/GPX/1/1"
  xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.topografix.com/GPX/1/1
    http://www.topografix.com/GPX/1/1/gpx.xsd">"""
        )

        // Metadata
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${name.escapeXml()}</name>")
        sb.appendLine("    <time>$createdAt</time>")
        sb.appendLine("  </metadata>")

        // Track
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${name.escapeXml()}</name>")
        sb.appendLine("    <type>cycling</type>")
        sb.appendLine("    <trkseg>")

        for (pt in points) {
            val time = isoFormat.format(Date(pt.timestamp))
            val lat = "%.8f".format(pt.latitude)
            val lon = "%.8f".format(pt.longitude)
            val ele = "%.2f".format(pt.altitude)
            // Speed in m/s for GPX extension (pt.speed is in km/h from WearOS)
            val speedMs = "%.4f".format(pt.speed / 3.6f)

            sb.appendLine("""      <trkpt lat="$lat" lon="$lon">""")
            sb.appendLine("        <ele>$ele</ele>")
            sb.appendLine("        <time>$time</time>")
            sb.appendLine("        <extensions>")
            sb.appendLine("          <gpxtpx:TrackPointExtension>")
            sb.appendLine("            <gpxtpx:speed>$speedMs</gpxtpx:speed>")
            sb.appendLine("          </gpxtpx:TrackPointExtension>")
            sb.appendLine("        </extensions>")
            sb.appendLine("      </trkpt>")
        }

        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")

        return sb.toString()
    }

    private fun String.escapeXml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
