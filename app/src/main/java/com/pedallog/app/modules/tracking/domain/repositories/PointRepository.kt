package com.pedallog.app.modules.tracking.domain.repositories

import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import kotlinx.coroutines.flow.Flow

/**
 * Interface do repositório de pontos GPS.
 */
interface PointRepository {
    suspend fun savePoints(sessionId: SessionId, points: List<PedalPoint>)
    fun getPointsForSession(sessionId: SessionId): Flow<List<PedalPoint>>
}
