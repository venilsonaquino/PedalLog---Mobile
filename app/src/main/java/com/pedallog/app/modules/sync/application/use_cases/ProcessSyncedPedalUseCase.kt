package com.pedallog.app.modules.sync.application.use_cases

import com.pedallog.app.modules.sync.application.dtos.SyncedPedalInput
import com.pedallog.app.modules.session.domain.repositories.SessionRepository
import com.pedallog.app.modules.tracking.domain.repositories.PointRepository

/**
 * Caso de Uso responsável por processar e persistir dados sincronizados do Wearable.
 * 
 * Camada de Aplicação (Orquestrador).
 */
class ProcessSyncedPedalUseCase(
    private val sessionRepository: SessionRepository,
    private val pointRepository: PointRepository
) {
    suspend fun execute(input: SyncedPedalInput) {
        if (sessionRepository.exists(input.session.id)) {
            return
        }
        
        sessionRepository.save(input.session)
        pointRepository.savePoints(input.session.id, input.points)
    }
}
