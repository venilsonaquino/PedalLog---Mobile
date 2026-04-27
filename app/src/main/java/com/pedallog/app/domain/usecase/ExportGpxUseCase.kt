package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ExportGpxUseCase {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    operator fun invoke(session: PedalSession, points: List<PedalPoint>): String {
        val createdAt = isoFormat.format(Date(session.startTime))
        val name = "PedalLog ${createdAt.substring(0, 10)}".escapeXml()
        val builder = GpxTrackBuilder(isoFormat)

        return """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="PedalLog Mobile" xmlns="http://www.topografix.com/GPX/1/1"
  xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
  <metadata>
    <name>$name</name>
    <time>$createdAt</time>
  </metadata>
  <trk>
    <name>$name</name>
    <type>cycling</type>
    <trkseg>
${points.joinToString("\n") { builder.buildTrackPoint(it) }}
    </trkseg>
  </trk>
</gpx>"""
    }
}
