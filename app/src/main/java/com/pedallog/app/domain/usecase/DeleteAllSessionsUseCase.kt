package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.repository.SessionRepository

/**
 * Caso de uso responsável por excluir todas as sessões do banco de dados.
 */
class DeleteAllSessionsUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        sessionRepository.deleteAllSessions()
    }
}
