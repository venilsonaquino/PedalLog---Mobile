package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.repository.PedalRepository

/**
 * Caso de uso responsável por salvar uma sessão sincronizada, garantindo a deduplicação.
 * 
 * SRP: Centraliza a regra de negócio de persistência de novas sessões vindas do Wearable.
 */
class SaveSyncedPedalUseCase(
    private val repository: PedalRepository
) {
    suspend operator fun invoke(session: PedalSession, points: List<PedalPoint>) {
        if (!repository.sessionExists(session.syncUuid)) {
            repository.saveSessionWithPoints(session, points)
        }
    }
}

