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
            val geoJson = if (points.isNotEmpty()) getGeoJsonPathUseCase.invoke(points) else null
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
                maxSpeedKmH = session.calculateMaxSpeedKmH(points),
                avgSpeedKmH = session.calculateAverageSpeedKmH(points),
                formattedDuration = session.getFormattedDuration(),
                formattedDistance = session.getFormattedDistance(),
                trackPoints = trackPoints
            )
        }
    }

    fun deleteSession(syncUuid: String) {
        viewModelScope.launch {
            repository.deleteSession(syncUuid)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            repository.deleteAllSessions()
        }
    }

    fun exportCurrentSessionAsGpx() {
        viewModelScope.launch {
            val details = _currentSessionDetails.value ?: run {
                _uiEvent.emit(UiEvent.ShowToast("Nenhuma sessão carregada para exportar."))
                return@launch
            }

            try {
                // Re-fetch points to build GPX (points not stored in SessionMetrics)
                var points = repository.getPointsForSession(details.session.syncUuid)
                    .firstOrNull() ?: emptyList()

                if (points.isEmpty()) {
                    val sessionId = repository.getSessionIdByUuid(details.session.syncUuid)
                    if (sessionId != null) {
                        points = repository.getPointsBySessionId(sessionId)
                            .firstOrNull() ?: emptyList()
                    }
                }

                if (points.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Sem pontos GPS para exportar."))
                    return@launch
                }

                val gpxContent = exportGpxUseCase.invoke(details.session, points)

                val uri = withContext(Dispatchers.IO) {
                    val ctx = getApplication<Application>()
                    val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(Date(details.session.startTime))
                    val fileName = "pedallog_$dateTag.gpx"
                    val gpxDir = File(ctx.cacheDir, "gpx").also { it.mkdirs() }
                    val file = File(gpxDir, fileName)
                    file.writeText(gpxContent, Charsets.UTF_8)
                    FileProvider.getUriForFile(
                        ctx,
                        "${ctx.packageName}.provider",
                        file
                    )
                }

                _uiEvent.emit(UiEvent.ShareGpx(uri))

            } catch (e: Exception) {
                Log.e("MapViewModel", "Erro ao exportar GPX", e)
                _uiEvent.emit(UiEvent.ShowToast("Erro ao exportar GPX: ${e.message}"))
            }
        }
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
}
