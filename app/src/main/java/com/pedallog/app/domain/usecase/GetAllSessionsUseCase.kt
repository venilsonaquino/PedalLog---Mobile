package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso responsável por listar todas as sessões de pedal.
 */
class GetAllSessionsUseCase(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<List<PedalSession>> {
        return sessionRepository.getAllSessions()
    }
}
