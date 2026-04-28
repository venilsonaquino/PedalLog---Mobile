package com.pedallog.app.modules.session.application.use_cases

import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import com.pedallog.app.modules.tracking.domain.repositories.PointRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para carregar os pontos GPS de uma sessão específica.
 */
class LoadSessionPointsUseCase(private val repository: PointRepository) {
    operator fun invoke(sessionId: SessionId): Flow<List<PedalPoint>> {
        return repository.getPointsForSession(sessionId)
    }
}
