package com.pedallog.app.modules.session.application.use_cases

import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.session.domain.repositories.SessionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para recuperar todas as sessões de pedal.
 */
class GetAllSessionsUseCase(private val repository: SessionRepository) {
    operator fun invoke(): Flow<List<PedalSession>> {
        return repository.getAllSessions()
    }
}
