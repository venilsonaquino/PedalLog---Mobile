package com.pedallog.app.domain.repository

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface PedalRepository {
    suspend fun saveSessionWithPoints(session: PedalSession, points: List<PedalPoint>)
    fun getAllSessions(): Flow<List<PedalSession>>
    fun getPointsForSession(syncUuid: SessionId): Flow<List<PedalPoint>>
    fun getPointsBySessionId(sessionId: Long): Flow<List<PedalPoint>>
    suspend fun getSessionIdByUuid(syncUuid: SessionId): Long?
    suspend fun sessionExists(syncUuid: SessionId): Boolean
    suspend fun deleteSession(syncUuid: SessionId)

    suspend fun deleteAllSessions()
}
