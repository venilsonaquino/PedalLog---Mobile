package com.pedallog.app.domain.repository

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import kotlinx.coroutines.flow.Flow

interface PedalRepository {
    suspend fun saveSessionWithPoints(session: PedalSession, points: List<PedalPoint>)
    fun getAllSessions(): Flow<List<PedalSession>>
    fun getPointsForSession(syncUuid: String): Flow<List<PedalPoint>>
    suspend fun sessionExists(syncUuid: String): Boolean
    suspend fun deleteSession(syncUuid: String)
}
