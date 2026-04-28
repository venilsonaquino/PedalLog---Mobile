package com.pedallog.app.modules.session.application.use_cases

import com.pedallog.app.modules.session.domain.repositories.SessionRepository
import com.pedallog.app.modules.session.domain.valueobjects.SessionId

/**
 * Caso de uso para deletar uma sessão específica.
 */
class DeleteSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(sessionId: SessionId) {
        repository.deleteSession(sessionId)
    }
}
