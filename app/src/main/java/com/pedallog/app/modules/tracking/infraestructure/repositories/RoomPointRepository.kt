package com.pedallog.app.modules.tracking.infraestructure.repositories

import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import com.pedallog.app.modules.tracking.domain.repositories.PointRepository
import com.pedallog.app.modules.tracking.infraestructure.db.PointDao
import com.pedallog.app.modules.tracking.infraestructure.db.mappers.PointMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação do repositório de pontos usando Room.
 */
class RoomPointRepository(
    private val pointDao: PointDao
) : PointRepository {

    override suspend fun savePoints(sessionId: SessionId, points: List<PedalPoint>) {
        val models = points.map { entity ->
            PointMapper.toModel(entity).copy(sessionUuid = sessionId.value)
        }
        pointDao.insertPoints(models)
    }

    override fun getPointsForSession(sessionId: SessionId): Flow<List<PedalPoint>> {
        return pointDao.getPointsForSession(sessionId.value).map { models ->
            models.map { PointMapper.toEntity(it) }
        }
    }
}
