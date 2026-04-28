package com.pedallog.app.modules.session.infraestructure.db.mappers

import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.session.domain.valueobjects.*
import com.pedallog.app.modules.session.infraestructure.db.models.SessionModel
import com.pedallog.app.shared.domain.valueobjects.*
import com.pedallog.app.shared.infraestructure.Mapper

/**
 * Mapper responsável por converter entre PedalSession (Domínio) e SessionModel (DB).
 * 
 * SOLID: Segue o Princípio da Responsabilidade Única (SRP).
 */
object SessionMapper : Mapper<PedalSession, SessionModel> {
    override fun toEntity(model: SessionModel): PedalSession {
        return PedalSession(
            id = SessionId(model.syncUuid),
            details = SessionDetails(
                timeRange = SessionTimeRange(
                    start = Timestamp(model.startTime),
                    end = Timestamp(model.endTime),
                    activeDuration = Duration(model.activeDurationMs)
                ),
                metrics = SessionMetrics(
                    distance = Distance(model.distanceKm),
                    averageSpeed = Speed(model.averageSpeed),
                    elevation = ElevationMetrics(
                        totalAscent = Distance(model.totalAscent),
                        totalDescent = Distance(model.totalDescent)
                    )
                )
            )
        )
    }

    override fun toModel(entity: PedalSession): SessionModel {
        return SessionModel(
            syncUuid = entity.id.value,
            startTime = entity.details.timeRange.start.milliseconds,
            endTime = entity.details.timeRange.end.milliseconds,
            distanceKm = entity.details.metrics.distance.kilometers,
            averageSpeed = entity.details.metrics.averageSpeed.kilometersPerHour,
            totalAscent = entity.details.metrics.elevation.totalAscent.kilometers,
            totalDescent = entity.details.metrics.elevation.totalDescent.kilometers,
            activeDurationMs = entity.details.timeRange.activeDuration.milliseconds
        )
    }
}
