package com.pedallog.app.shared.utils

import com.pedallog.app.modules.tracking.infraestructure.db.models.PointModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitário para geração de arquivos GPX a partir de modelos de persistência.
 */
object GpxExporter {
    fun generateGpx(points: List<PointModel>, sessionName: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xml.append("<gpx version=\"1.1\" creator=\"PedalLog\"\n")
        xml.append("  xmlns=\"http://www.topografix.com/GPX/1/1\"\n")
        xml.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
        xml.append("  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")
        
        xml.append("  <metadata>\n")
        xml.append("    <name>$sessionName</name>\n")
        xml.append("    <time>${sdf.format(Date())}</time>\n")
        xml.append("  </metadata>\n")

        xml.append("  <trk>\n")
        xml.append("    <name>$sessionName</name>\n")
        xml.append("    <trkseg>\n")

        for (point in points) {
            xml.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
            xml.append("        <ele>${point.altitude}</ele>\n")
            xml.append("        <time>${sdf.format(Date(point.timestamp))}</time>\n")
            xml.append("        <extensions>\n")
            xml.append("          <speed>${point.speed}</speed>\n")
            xml.append("        </extensions>\n")
            xml.append("      </trkpt>\n")
        }

        xml.append("    </trkseg>\n")
        xml.append("  </trk>\n")
        xml.append("</gpx>")

        return xml.toString()
    }
}
