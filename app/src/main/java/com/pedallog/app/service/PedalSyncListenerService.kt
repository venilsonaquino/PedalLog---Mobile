package com.pedallog.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pedallog.app.MainActivity
import com.pedallog.app.R
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

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        val repository = PedalRepositoryImpl(database)
        saveSyncedPedalUseCase = SaveSyncedPedalUseCase(repository)
        createNotificationChannel()
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
                            sendNotification("Novo pedal de $distanceFormatted km sincronizado!")
                        } catch (e: Exception) {
                            Log.e("SyncService", "Erro na sincronização de $syncUuid: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun sendNotification(message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("PedalLog Sincronizado")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sincronização"
            val descriptionText = "Notificações de sincronização com o relógio"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "pedal_sync_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
