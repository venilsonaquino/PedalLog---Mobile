package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalPoint
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Auxilia na construção dos segmentos de trilha do GPX.
 */
class GpxTrackBuilder(private val isoFormat: SimpleDateFormat) {

    fun buildTrackPoint(pt: PedalPoint): String {
        val time = isoFormat.format(Date(pt.timestamp))
        val lat = "%.8f".format(pt.latitude)
        val lon = "%.8f".format(pt.longitude)
        val ele = "%.2f".format(pt.altitude)
        val speedMs = "%.4f".format(pt.speed / 3.6f)

        return """      <trkpt lat="$lat" lon="$lon">
        <ele>$ele</ele>
        <time>$time</time>
        <extensions>
          <gpxtpx:TrackPointExtension>
            <gpxtpx:speed>$speedMs</gpxtpx:speed>
          </gpxtpx:TrackPointExtension>
        </extensions>
      </trkpt>"""
    }
}

fun String.escapeXml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
