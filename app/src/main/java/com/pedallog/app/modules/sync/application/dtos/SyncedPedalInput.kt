package com.pedallog.app.modules.sync.application.dtos

import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.tracking.domain.entities.PedalPoint

/**
 * Objeto de transferência de dados (DTO) para entrada de sincronização.
 */
data class SyncedPedalInput(
    val session: PedalSession,
    val points: List<PedalPoint>
)
