package com.pedallog.app.service

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.pedallog.app.data.mapper.GzipCsvUtils
import kotlinx.coroutines.tasks.await
import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId

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

    suspend fun parse(context: Context, dataMap: DataMap): SyncPayload? {
        val syncUuid = dataMap.getString("sync_uuid") ?: return null
        val startTime = dataMap.getLong("start_time")
        val endTime = dataMap.getLong("end_time")
        val totalDistance = dataMap.getFloat("total_distance")
        val activeDurationMs = dataMap.getLong("active_duration_ms")
        
        val asset = dataMap.getAsset("points_asset") ?: return null
        val dataClient = Wearable.getDataClient(context)
        val fd = dataClient.getFdForAsset(asset).await()
        val pointsGzip = fd.inputStream.use { it.readBytes() }

        val points = GzipCsvUtils.decompressAndParsePoints(pointsGzip)
        
        // Correção de Timestamps baseada nos pontos reais do GPS
        val realStartTime = if (points.isNotEmpty()) points.first().timestamp else startTime
        val realEndTime = if (points.isNotEmpty()) points.last().timestamp else endTime

        val durationSec = (realEndTime - realStartTime) / 1000.0
        val avgSpeed = if (durationSec > 0) {
            (totalDistance / durationSec).toFloat()
        } else 0f

        val session = PedalSession(
            syncUuid = SessionId(syncUuid),
            startTime = realStartTime,
            endTime = realEndTime,
            distanceKm = totalDistance.toDouble(),
            averageSpeed = avgSpeed,
            totalAscent = 0.0,
            totalDescent = 0.0,
            activeDurationMs = activeDurationMs
        )

        return SyncPayload(session, points)
    }
}
