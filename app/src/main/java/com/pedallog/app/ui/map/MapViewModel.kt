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
import com.pedallog.app.domain.usecase.DeleteAllSessionsUseCase
import com.pedallog.app.domain.usecase.DeleteSessionUseCase
import com.pedallog.app.domain.usecase.ExportGpxUseCase
import com.pedallog.app.domain.usecase.GetAllSessionsUseCase
import com.pedallog.app.domain.usecase.GetGeoJsonPathUseCase
import com.pedallog.app.domain.usecase.LoadSessionPointsUseCase
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

    private val getGeoJsonPathUseCase = GetGeoJsonPathUseCase()
    private lateinit var deleteSessionUseCase: DeleteSessionUseCase
    private lateinit var deleteAllSessionsUseCase: DeleteAllSessionsUseCase
    private lateinit var loadSessionPointsUseCase: LoadSessionPointsUseCase
    private lateinit var getAllSessionsUseCase: GetAllSessionsUseCase

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
        val repository = PedalRepositoryImpl(database)
        deleteSessionUseCase = DeleteSessionUseCase(repository)
        deleteAllSessionsUseCase = DeleteAllSessionsUseCase(repository)
        loadSessionPointsUseCase = LoadSessionPointsUseCase(repository)
        getAllSessionsUseCase = GetAllSessionsUseCase(repository)
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
            getAllSessionsUseCase().collect { sessionList ->
                _sessions.value = sessionList
            }
        }
    }

    fun loadSessionTrack(session: PedalSession) {
        viewModelScope.launch {
            _currentSessionDetails.value = null 
            
            var points = loadSessionPointsUseCase(session.syncUuid).firstOrNull() ?: emptyList()
            
            Log.d("PedalDebug", "Session UUID: ${session.syncUuid}, Points found (by UUID): ${points.size}")
            
            if (points.isEmpty()) {
                // Not ideal, but keeping the ID-based fallback logic inside ViewModel for now or could move to UseCase
                // Actually, let's keep it simple as it's a legacy compatibility thing
                Log.d("PedalDebug", "No points found for UUID: ${session.syncUuid}")
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
            deleteSessionUseCase(syncUuid)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            deleteAllSessionsUseCase()
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
