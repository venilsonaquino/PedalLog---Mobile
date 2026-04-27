package com.pedallog.app.data.repository

import androidx.room.withTransaction
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.mapper.PedalMapper
import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.domain.repository.PedalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PedalRepositoryImpl(
    private val db: AppDatabase
) : PedalRepository {

    override suspend fun saveSessionWithPoints(session: PedalSession, points: List<PedalPoint>) {
        val sessionEntity = PedalMapper.toEntitySession(session)
        val pointEntities = points.map { PedalMapper.toEntityPoint(it) }
        db.withTransaction {
            val sessionId = db.pedalDao().insertSession(sessionEntity)
            val pointsWithSessionId = pointEntities.map { it.copy(sessionId = sessionId) }
            db.pedalDao().insertPoints(pointsWithSessionId)
        }
    }

    override fun getAllSessions(): Flow<List<PedalSession>> {
        return db.pedalDao().getAllSessions().map { entities ->
            entities.map { PedalMapper.toDomainSession(it) }
        }
    }

    override fun getPointsForSession(syncUuid: SessionId): Flow<List<PedalPoint>> {
        return db.pedalDao().getPointsForSession(syncUuid.value).map { entities ->
            entities.map { PedalMapper.toDomainPoint(it) }
        }
    }

    override fun getPointsBySessionId(sessionId: Long): Flow<List<PedalPoint>> {
        return db.pedalDao().getPointsBySessionId(sessionId).map { entities ->
            entities.map { PedalMapper.toDomainPoint(it) }
        }
    }

    override suspend fun getSessionIdByUuid(syncUuid: SessionId): Long? = withContext(Dispatchers.IO) {
        return@withContext db.pedalDao().getSessionByUuid(syncUuid.value)?.id
    }

    override suspend fun sessionExists(syncUuid: SessionId): Boolean = withContext(Dispatchers.IO) {
        return@withContext db.pedalDao().getSessionByUuid(syncUuid.value) != null
    }

    override suspend fun deleteSession(syncUuid: SessionId) = withContext(Dispatchers.IO) {
        db.pedalDao().deleteSession(syncUuid.value)
    }

    override suspend fun deleteAllSessions() = withContext(Dispatchers.IO) {
        db.pedalDao().deleteAllSessions()
    }
}
