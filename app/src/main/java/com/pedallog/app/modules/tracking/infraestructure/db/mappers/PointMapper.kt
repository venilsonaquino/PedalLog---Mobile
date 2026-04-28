package com.pedallog.app.modules.tracking.infraestructure.db.mappers

import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import com.pedallog.app.modules.tracking.domain.valueobjects.Coordinate
import com.pedallog.app.modules.tracking.domain.valueobjects.PointDetails
import com.pedallog.app.modules.tracking.infraestructure.db.models.PointModel
import com.pedallog.app.shared.domain.valueobjects.Speed
import com.pedallog.app.shared.domain.valueobjects.Timestamp
import com.pedallog.app.shared.infraestructure.Mapper

/**
 * Mapper responsável pela conversão entre a entidade de domínio e o modelo de banco de dados.
 */
object PointMapper : Mapper<PedalPoint, PointModel> {
    
    override fun toEntity(model: PointModel): PedalPoint {
        return PedalPoint(
            coordinate = Coordinate(model.latitude, model.longitude),
            details = PointDetails(
                altitude = model.altitude,
                speed = Speed(model.speed),
                timestamp = Timestamp(model.timestamp),
                accuracy = model.accuracy
            )
        )
    }

    override fun toModel(entity: PedalPoint): PointModel {
        return PointModel(
            sessionUuid = "", // Deve ser preenchido pelo repositório
            timestamp = entity.details.timestamp.milliseconds,
            latitude = entity.coordinate.latitude,
            longitude = entity.coordinate.longitude,
            altitude = entity.details.altitude,
            speed = entity.details.speed.kilometersPerHour.toFloat(),
            accuracy = entity.details.accuracy
        )
    }
}
