package com.pedallog.app.modules.session.application.use_cases

import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Caso de uso para gerar o conteúdo XML de um arquivo GPX.
 */
class ExportGpxUseCase {
    operator fun invoke(session: PedalSession, points: List<PedalPoint>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xml.append("<gpx version=\"1.1\" creator=\"PedalLog\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        xml.append("  <metadata>\n")
        xml.append("    <time>${sdf.format(Date(session.details.timeRange.start.milliseconds))}</time>\n")
        xml.append("  </metadata>\n")
        xml.append("  <trk>\n")
        xml.append("    <name>Sessão de Pedal</name>\n")
        xml.append("    <trkseg>\n")

        points.forEach { point ->
            xml.append("      <trkpt lat=\"${point.coordinate.latitude}\" lon=\"${point.coordinate.longitude}\">\n")
            xml.append("        <ele>${point.details.altitude}</ele>\n")
            xml.append("        <time>${sdf.format(Date(point.details.timestamp.milliseconds))}</time>\n")
            xml.append("        <extensions>\n")
            xml.append("          <speed>${point.details.speed.metersPerSecond}</speed>\n")
            xml.append("        </extensions>\n")
            xml.append("      </trkpt>\n")
        }

        xml.append("    </trkseg>\n")
        xml.append("  </trk>\n")
        xml.append("</gpx>")

        return xml.toString()
    }
}
