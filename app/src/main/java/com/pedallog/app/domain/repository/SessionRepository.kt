package com.pedallog.app.domain.repository

import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

/**
 * Interface segregada para operações relacionadas a Sessões de Pedal.
 * ISP: O cliente não deve ser forçado a depender de métodos que não utiliza.
 */
interface SessionRepository {
    fun getAllSessions(): Flow<List<PedalSession>>
    suspend fun getSessionIdByUuid(syncUuid: SessionId): Long?
    suspend fun sessionExists(syncUuid: SessionId): Boolean
    suspend fun deleteSession(syncUuid: SessionId)
    suspend fun deleteAllSessions()
}
