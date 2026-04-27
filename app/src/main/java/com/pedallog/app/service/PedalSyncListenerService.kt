package com.pedallog.app.service

import android.util.Log
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.mapper.GzipCsvUtils
import com.pedallog.app.data.repository.PedalRepositoryImpl
import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.domain.usecase.SaveSyncedPedalUseCase
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PedalSyncListenerService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var saveSyncedPedalUseCase: SaveSyncedPedalUseCase
    private lateinit var notificationManager: SyncNotificationManager

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        val repository = PedalRepositoryImpl(database)
        saveSyncedPedalUseCase = SaveSyncedPedalUseCase(repository)
        notificationManager = SyncNotificationManager(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val uri = event.dataItem.uri
            Log.d("SyncService", "Evento recebido: ${event.type} em ${uri.path}")
            
            if (event.type == DataEvent.TYPE_CHANGED && uri.path?.startsWith("/pedal_session") == true) {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val dataMap = dataMapItem.dataMap
                
                val syncUuid = dataMap.getString("sync_uuid")
                val startTime = dataMap.getLong("start_time")
                val endTime = dataMap.getLong("end_time")
                val totalDistance = dataMap.getFloat("total_distance")
                val activeDurationMs = dataMap.getLong("active_duration_ms")
                val pointsGzip = dataMap.getByteArray("points_gz")
                
                if (syncUuid != null && pointsGzip != null) {
                    scope.launch {
                        try {
                            val durationSec = (endTime - startTime) / 1000.0
                            val avgSpeed = if (durationSec > 0) {
                                (totalDistance / durationSec).toFloat()
                            } else 0f

                            val points = GzipCsvUtils.decompressAndParsePoints(pointsGzip)
                            
                            // Correção de Timestamps baseada nos pontos reais do GPS
                            val realStartTime = if (points.isNotEmpty()) points.first().timestamp else startTime
                            val realEndTime = if (points.isNotEmpty()) points.last().timestamp else endTime

                            val session = com.pedallog.app.domain.model.PedalSession(
                                syncUuid = SessionId(syncUuid),
                                startTime = realStartTime,
                                endTime = realEndTime,
                                distanceKm = totalDistance.toDouble(),
                                averageSpeed = avgSpeed,
                                totalAscent = 0.0,
                                totalDescent = 0.0,
                                activeDurationMs = activeDurationMs
                            )
                            
                            saveSyncedPedalUseCase(session, points)
                            Log.d("SyncService", "Sessão $syncUuid sincronizada com ${points.size} pontos.")
                            
                            val distanceFormatted = String.format("%.2f", session.distanceKm)
                            notificationManager.sendSyncNotification("Novo pedal de $distanceFormatted km sincronizado!")
                        } catch (e: Exception) {
                            Log.e("SyncService", "Erro na sincronização de $syncUuid: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

