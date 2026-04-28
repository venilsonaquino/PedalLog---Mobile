package com.pedallog.app.modules.session.domain.entities

import com.pedallog.app.modules.session.domain.valueobjects.SessionId
import com.pedallog.app.modules.session.domain.valueobjects.SessionDetails

/**
 * Entidade que representa uma sessão de pedal.
 * 
 * Camada de Domínio (DDD).
 * Object Calisthenics: 
 * - Apenas 2 variáveis por instância.
 * - Sem abreviações.
 * - Classes pequenas (< 50 linhas).
 */
data class PedalSession(
    val id: SessionId,
    val details: SessionDetails
)
