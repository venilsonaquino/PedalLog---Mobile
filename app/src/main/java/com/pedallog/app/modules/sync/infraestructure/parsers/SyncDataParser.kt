package com.pedallog.app.modules.sync.infraestructure.parsers

import com.google.android.gms.wearable.DataMap
import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.session.domain.valueobjects.SessionDetails
import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import com.pedallog.app.modules.session.domain.valueobjects.SessionMetrics
import com.pedallog.app.modules.session.domain.valueobjects.ElevationMetrics
import com.pedallog.app.modules.session.domain.valueobjects.SessionTimeRange
import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import com.pedallog.app.shared.domain.valueobjects.*
import com.pedallog.app.shared.utils.GzipCsvUtils

/**
 * Payload que contém os dados parseados de uma sincronização.
 */
data class SyncPayload(
    val session: PedalSession,
    val points: List<PedalPoint>
)

/**
 * Responsável por converter os dados brutos recebidos do Wearable DataMap em objetos de domínio.
 * SRP: Isola a lógica de mapeamento de dados externos.
 */
object SyncDataParser {

    fun parse(dataMap: DataMap): SyncPayload? {
        val syncUuid = dataMap.getString("sync_uuid") ?: return null
        val startTime = dataMap.getLong("start_time")
        val endTime = dataMap.getLong("end_time")
        val totalDistance = dataMap.getFloat("total_distance")
        val activeDurationMs = dataMap.getLong("active_duration_ms")
        val pointsGzip = dataMap.getByteArray("points_gz") ?: return null

        val points = GzipCsvUtils.decompressAndParsePoints(pointsGzip)
        
        // Correção de Timestamps baseada nos pontos reais do GPS
        val realStartTime = if (points.isNotEmpty()) points.first().details.timestamp.milliseconds else startTime
        val realEndTime = if (points.isNotEmpty()) points.last().details.timestamp.milliseconds else endTime

        val durationSec = (realEndTime - realStartTime) / 1000.0
        val avgSpeed = if (durationSec > 0) {
            (totalDistance / durationSec).toFloat()
        } else 0f

        val session = PedalSession(
            id = SessionId(syncUuid),
            details = SessionDetails(
                timeRange = SessionTimeRange(
                    start = Timestamp(realStartTime),
                    end = Timestamp(realEndTime),
                    activeDuration = Duration(activeDurationMs)
                ),
                metrics = SessionMetrics(
                    distance = Distance(totalDistance.toDouble()),
                    averageSpeed = Speed(avgSpeed),
                    elevation = ElevationMetrics(Distance(0.0), Distance(0.0))
                )
            )
        )

        return SyncPayload(session, points)
    }
}
