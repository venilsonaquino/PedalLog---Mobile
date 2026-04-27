package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.domain.repository.SessionRepository

/**
 * Caso de uso responsável por excluir uma sessão de pedal.
 * SRP: Centraliza a regra de exclusão de dados de sessão.
 */
class DeleteSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(syncUuid: SessionId) {
        sessionRepository.deleteSession(syncUuid)
    }
}
