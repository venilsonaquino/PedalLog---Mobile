package com.pedallog.app.ui.map

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.repository.PedalRepositoryImpl
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.domain.repository.PedalRepository
import com.pedallog.app.domain.usecase.ExportGpxUseCase
import com.pedallog.app.domain.usecase.GetGeoJsonPathUseCase
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.pedallog.app.utils.AnimationModule
import com.pedallog.app.utils.GpxUtils
import com.pedallog.app.domain.model.SessionFormatter
import com.pedallog.app.domain.model.SessionMetricsCalculator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PedalRepository
    private val getGeoJsonPathUseCase = GetGeoJsonPathUseCase()
    private val exportGpxUseCase = ExportGpxUseCase()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _currentSessionDetails = MutableStateFlow<SessionMetrics?>(null)
    val currentSessionDetails: StateFlow<SessionMetrics?> = _currentSessionDetails

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _sessions = MutableStateFlow<List<PedalSession>>(emptyList())
    val sessions: StateFlow<List<PedalSession>> = _sessions

    val totalDistanceKm: StateFlow<Double> = _sessions
        .map { sessions -> sessions.sumOf { it.distanceKm } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PedalRepositoryImpl(database)
        loadAllSessions()
    }

    fun requestManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val nodeClient = Wearable.getNodeClient(getApplication<Application>())
                val messageClient = Wearable.getMessageClient(getApplication<Application>())
                
                val nodes = nodeClient.connectedNodes.await()
                Log.d("MapViewModel", "Relógios encontrados: ${nodes.size}")
                if (nodes.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Nenhum relógio conectado via Bluetooth"))
                    return@launch
                }
                
                var sentCount = 0
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/request_sync", null).await()
                    sentCount++
                    Log.d("MapViewModel", "Solicitação enviada para: ${node.displayName}")
                }
                
                if (sentCount > 0) {
                    _uiEvent.emit(UiEvent.ShowToast("Sincronização solicitada ao relógio. Aguarde..."))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast("Erro ao solicitar sincronização: ${e.message}"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun loadAllSessions() {
        viewModelScope.launch {
            repository.getAllSessions().collect { sessionList ->
                _sessions.value = sessionList
            }
        }
    }

    fun loadSessionTrack(session: PedalSession) {
        viewModelScope.launch {
            _currentSessionDetails.value = null // Limpa estado anterior
            
            var points = repository.getPointsForSession(session.syncUuid).firstOrNull() ?: emptyList()
            
            Log.d("PedalDebug", "Session UUID: ${session.syncUuid}, Points found (by UUID): ${points.size}")
            
            if (points.isEmpty()) {
                val sessionId = repository.getSessionIdByUuid(session.syncUuid)
                if (sessionId != null) {
                    points = repository.getPointsBySessionId(sessionId).firstOrNull() ?: emptyList()
                    Log.d("PedalDebug", "Fallback - Session ID: $sessionId, Points found (by ID): ${points.size}")
                }
            }

            val sessionStartTime = session.startTime
            val geoJson = if (points.isNotEmpty()) getGeoJsonPathUseCase(points) else null
            val trackPoints = points.map { pt ->
                TrackPoint(
                    lat = pt.latitude,
                    lng = pt.longitude,
                    speedKmH = pt.speed,
                    timeOffsetMs = pt.timestamp - sessionStartTime
                )
            }
            
            _currentSessionDetails.value = SessionMetrics(
                session = session,
                geoJson = geoJson,
                gpsPointsCount = points.size,
                maxSpeedKmH = SessionMetricsCalculator.calculateMaxSpeedKmH(points),
                avgSpeedKmH = SessionMetricsCalculator.calculateAverageSpeedKmH(points),
                formattedDuration = SessionFormatter.formatDuration(session.durationMs),
                formattedDistance = SessionFormatter.formatDistance(session.distanceKm),
                trackPoints = trackPoints
            )
        }
    }

    fun deleteSession(syncUuid: SessionId) {
        viewModelScope.launch {
            repository.deleteSession(syncUuid)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            repository.deleteAllSessions()
        }
    }

    fun exportSessionToDownloads(session: PedalSession) {
        viewModelScope.launch {
            try {
                val points = repository.getPointsForSession(session.syncUuid).firstOrNull() ?: emptyList()
                if (points.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Sem pontos GPS para exportar."))
                    return@launch
                }

                // Convert domain points to data entities for the exporter (or update exporter to take domain)
                val entities = points.map { pt ->
                    com.pedallog.app.data.model.PointEntity(
                        id = 0, sessionId = 0, latitude = pt.latitude, longitude = pt.longitude,
                        altitude = pt.altitude, speed = pt.speed, timestamp = pt.timestamp,
                        accuracy = pt.accuracy
                    )
                }

                val gpxContent = exportGpxUseCase(session, points)
                val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(session.startTime))
                val fileName = "PedalLog_$dateTag"
                
                val uri = withContext(Dispatchers.IO) {
                    GpxUtils.saveGpxToDownloads(getApplication(), gpxContent, fileName)
                }

                if (uri != null) {
                    _uiEvent.emit(UiEvent.ShowToast("GPX salvo na pasta Downloads!"))
                } else {
                    _uiEvent.emit(UiEvent.ShowToast("Erro ao salvar arquivo."))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast("Erro: ${e.message}"))
            }
        }
    }

    fun generateGifForSession(session: PedalSession) {
        viewModelScope.launch {
            _isSyncing.value = true // Show progress
            try {
                val points = repository.getPointsForSession(session.syncUuid).firstOrNull() ?: emptyList()
                if (points.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Sem pontos GPS para o GIF."))
                    return@launch
                }

                val entities = points.map { pt ->
                    com.pedallog.app.data.model.PointEntity(
                        id = 0, sessionId = 0, latitude = pt.latitude, longitude = pt.longitude,
                        altitude = pt.altitude, speed = pt.speed, timestamp = pt.timestamp,
                        accuracy = pt.accuracy
                    )
                }

                val gifFile = withContext(Dispatchers.IO) {
                    AnimationModule.createGpsTraceGif(getApplication(), entities)
                }

                if (gifFile != null) {
                    val uri = FileProvider.getUriForFile(
                        getApplication(),
                        "${getApplication<Application>().packageName}.provider",
                        gifFile
                    )
                    _uiEvent.emit(UiEvent.ShareGif(uri))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast("Erro ao gerar GIF: ${e.message}"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun exportCurrentSessionAsGpx() {
        // ... (existing code or updated to use the same logic)
    }
}

data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val speedKmH: Float,
    val timeOffsetMs: Long
)

data class SessionMetrics(
    val session: PedalSession,
    val geoJson: String?,
    val gpsPointsCount: Int,
    val maxSpeedKmH: Float,
    val avgSpeedKmH: Float,
    val formattedDuration: String,
    val formattedDistance: String,
    val trackPoints: List<TrackPoint> = emptyList()
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShareGpx(val uri: Uri) : UiEvent()
    data class ShareGif(val uri: Uri) : UiEvent()
}
