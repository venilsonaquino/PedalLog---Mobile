package com.pedallog.app.modules.session.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pedallog.app.modules.analysis.application.use_cases.GetGeoJsonPathUseCase
import com.pedallog.app.modules.session.application.use_cases.LoadSessionPointsUseCase
import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.shared.domain.logic.SessionFormatter
import com.pedallog.app.shared.domain.logic.SessionMetricsCalculator
import com.pedallog.app.shared.infraestructure.db.AppDatabase
import com.pedallog.app.modules.tracking.infraestructure.repositories.RoomPointRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel responsável pelo carregamento e análise dos dados de uma sessão específica.
 * SRP: Orquestra o carregamento de pontos e cálculo de métricas para exibição.
 */
class SessionAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val pointRepository = RoomPointRepository(AppDatabase.getDatabase(application).pointDao())
    private val loadSessionPoints = LoadSessionPointsUseCase(pointRepository)
    private val getGeoJsonPath = GetGeoJsonPathUseCase()

    private val _sessionDetails = MutableStateFlow<SessionMetrics?>(null)
    val sessionDetails: StateFlow<SessionMetrics?> = _sessionDetails

    private var currentJob: kotlinx.coroutines.Job? = null

    fun loadSession(session: PedalSession) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _sessionDetails.value = null
            
            val startTime = session.details.timeRange.start.milliseconds
            
            loadSessionPoints(session.id).collect { points ->
                _sessionDetails.value = SessionMetrics(
                    session = session,
                    geoJson = if (points.isNotEmpty()) getGeoJsonPath(points) else null,
                    gpsPointsCount = points.size,
                    maxSpeedKmH = SessionMetricsCalculator.calculateMaxSpeedKmH(points),
                    avgSpeedKmH = SessionMetricsCalculator.calculateAverageSpeedKmH(points),
                    formattedDuration = SessionFormatter.formatDuration(session.details.timeRange.calculateTotalDuration().milliseconds),
                    formattedDistance = SessionFormatter.formatDistance(session.details.metrics.distance.kilometers),
                    trackPoints = points.map { pt -> 
                        TrackPoint(
                            lat = pt.coordinate.latitude, 
                            lng = pt.coordinate.longitude, 
                            speedKmH = pt.details.speed.kilometersPerHour.toFloat(), 
                            timeOffsetMs = pt.details.timestamp.milliseconds - startTime
                        ) 
                    }
                )
            }
        }
    }
}
