package com.pedallog.app.modules.sync.infraestructure.services

import android.util.Log
import com.pedallog.app.modules.sync.infraestructure.parsers.SyncDataParser
import com.pedallog.app.modules.sync.infraestructure.services.SyncNotificationManager
import com.pedallog.app.modules.sync.application.use_cases.ProcessSyncedPedalUseCase
import com.pedallog.app.modules.sync.application.dtos.SyncedPedalInput
import com.pedallog.app.modules.session.domain.repositories.SessionRepository
import com.pedallog.app.modules.tracking.domain.repositories.PointRepository
import com.pedallog.app.shared.infraestructure.db.AppDatabase
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.pedallog.app.modules.session.infraestructure.repositories.RoomSessionRepository
import com.pedallog.app.modules.tracking.infraestructure.repositories.RoomPointRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PedalSyncListenerService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var processSyncedPedalUseCase: ProcessSyncedPedalUseCase
    private lateinit var notificationManager: SyncNotificationManager

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        val sessionRepository = RoomSessionRepository(database.sessionDao())
        val pointRepository = RoomPointRepository(database.pointDao())
        processSyncedPedalUseCase = ProcessSyncedPedalUseCase(sessionRepository, pointRepository)
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
                if (syncUuid != null) {
                    scope.launch {
                        try {
                            val payload = SyncDataParser.parse(dataMap) ?: return@launch
                            
                            processSyncedPedalUseCase.execute(SyncedPedalInput(payload.session, payload.points))
                            Log.d("SyncService", "Sessão ${payload.session.id.value} sincronizada com ${payload.points.size} pontos.")
                            
                            val distanceFormatted = String.format("%.2f", payload.session.details.metrics.distance.kilometers)
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

