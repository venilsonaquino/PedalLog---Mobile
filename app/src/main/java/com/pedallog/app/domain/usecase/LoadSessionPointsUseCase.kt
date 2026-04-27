package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.domain.repository.PointRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso responsável por carregar os pontos GPS de uma sessão específica.
 * SRP: Isola a recuperação de dados geográficos.
 */
class LoadSessionPointsUseCase(
    private val pointRepository: PointRepository
) {
    operator fun invoke(syncUuid: SessionId): Flow<List<PedalPoint>> {
        return pointRepository.getPointsForSession(syncUuid)
    }
}
