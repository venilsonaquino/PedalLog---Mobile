package com.pedallog.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.repository.PedalRepositoryImpl
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.repository.PedalRepository
import com.pedallog.app.domain.usecase.GetGeoJsonPathUseCase
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PedalRepository
    private val getGeoJsonPathUseCase = GetGeoJsonPathUseCase()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _currentSessionDetails = MutableStateFlow<SessionMetrics?>(null)
    val currentSessionDetails: StateFlow<SessionMetrics?> = _currentSessionDetails

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _sessions = MutableStateFlow<List<PedalSession>>(emptyList())
    val sessions: StateFlow<List<PedalSession>> = _sessions

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
                Log.d("MapViewModel", "Nós encontrados: ${nodes.size}")
                if (nodes.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Nenhum relógio conectado via Bluetooth"))
                    return@launch
                }
                
                var sentCount = 0
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/request_sync", null).await()
                    sentCount++
                    Log.d("MapViewModel", "Mensagem enviada para nó: ${node.displayName} (${node.id})")
                }
                
                if (sentCount > 0) {
                    _uiEvent.emit(UiEvent.ShowToast("Sincronização solicitada ($sentCount relógios)"))
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

            val geoJson = if (points.isNotEmpty()) getGeoJsonPathUseCase.invoke(points) else null
            
            _currentSessionDetails.value = SessionMetrics(
                session = session,
                geoJson = geoJson,
                gpsPointsCount = points.size,
                maxSpeedKmH = session.calculateMaxSpeedKmH(points),
                avgSpeedKmH = session.calculateAverageSpeedKmH(points),
                formattedDuration = session.getFormattedDuration(),
                formattedDistance = session.getFormattedDistance()
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
}

data class SessionMetrics(
    val session: PedalSession,
    val geoJson: String?,
    val gpsPointsCount: Int,
    val maxSpeedKmH: Float,
    val avgSpeedKmH: Float,
    val formattedDuration: String,
    val formattedDistance: String
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}
