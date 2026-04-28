package com.pedallog.app.modules.session.domain.repositories

import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para o repositório de sessões.
 * 
 * DDD: Apenas a interface reside no Domínio.
 * SOLID: Segue o Princípio da Inversão de Dependência (DIP).
 */
interface SessionRepository {
    fun getAllSessions(): Flow<List<PedalSession>>
    
    fun getSessionById(id: SessionId): Flow<PedalSession?>
    
    suspend fun deleteSession(id: SessionId)
    
    suspend fun save(session: PedalSession)
    
    suspend fun exists(id: SessionId): Boolean
}
