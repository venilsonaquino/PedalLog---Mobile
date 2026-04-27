package com.pedallog.app.service

import android.util.Log
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.repository.PedalRepositoryImpl
import com.pedallog.app.domain.usecase.SaveSyncedPedalUseCase
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
                if (syncUuid != null) {
                    scope.launch {
                        try {
                            val payload = SyncDataParser.parse(dataMap) ?: return@launch
                            
                            saveSyncedPedalUseCase(payload.session, payload.points)
                            Log.d("SyncService", "Sessão ${payload.session.syncUuid} sincronizada com ${payload.points.size} pontos.")
                            
                            val distanceFormatted = String.format("%.2f", payload.session.distanceKm)
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

