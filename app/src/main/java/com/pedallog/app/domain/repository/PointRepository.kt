package com.pedallog.app.domain.repository

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

/**
 * Interface segregada para operações relacionadas a Pontos GPS de sessões.
 * ISP: Foca apenas na recuperação e persistência de pontos.
 */
interface PointRepository {
    fun getPointsForSession(syncUuid: SessionId): Flow<List<PedalPoint>>
    fun getPointsBySessionId(sessionId: Long): Flow<List<PedalPoint>>
}
