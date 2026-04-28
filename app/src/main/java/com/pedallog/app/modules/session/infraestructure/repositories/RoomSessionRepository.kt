package com.pedallog.app.modules.session.infraestructure.repositories

import com.pedallog.app.modules.session.infraestructure.db.SessionDao
import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.session.domain.repositories.SessionRepository
import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import com.pedallog.app.modules.session.infraestructure.db.mappers.SessionMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação do repositório de sessões usando Room.
 * 
 * Camada de Infraestrutura.
 */
class RoomSessionRepository(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<PedalSession>> {
        return sessionDao.getAllSessions().map { models ->
            models.map { SessionMapper.toEntity(it) }
        }
    }

    override fun getSessionById(id: SessionId): Flow<PedalSession?> {
        // Como o DAO atual não tem getSessionByUuid que retorne Flow, 
        // usamos o getAllSessions filtrado para manter a reatividade básica.
        return sessionDao.getAllSessions().map { list ->
            list.find { it.syncUuid == id.value }?.let { SessionMapper.toEntity(it) }
        }
    }

    override suspend fun deleteSession(id: SessionId) {
        sessionDao.deleteSession(id.value)
    }

    override suspend fun save(session: PedalSession) {
        sessionDao.insertSession(SessionMapper.toModel(session))
    }

    override suspend fun exists(id: SessionId): Boolean {
        return sessionDao.getSessionByUuid(id.value) != null
    }
}
