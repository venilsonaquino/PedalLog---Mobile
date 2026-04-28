package com.pedallog.app.modules.tracking.domain.entities

import com.pedallog.app.modules.tracking.domain.valueobjects.Coordinate
import com.pedallog.app.modules.tracking.domain.valueobjects.PointDetails

/**
 * Entidade de Domínio representando um ponto de rastreamento.
 */
class PedalPoint(
    val coordinate: Coordinate,
    val details: PointDetails
)
