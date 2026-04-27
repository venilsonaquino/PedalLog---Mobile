package com.pedallog.app.data.mapper

import com.pedallog.app.data.model.PointEntity
import com.pedallog.app.data.model.SessionEntity
import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId

object PedalMapper {
    fun toDomainSession(entity: SessionEntity): PedalSession {
        return PedalSession(
            syncUuid = SessionId(entity.syncUuid),
            startTime = entity.startTime,
            endTime = entity.endTime,
            distanceKm = entity.distanceKm,
            averageSpeed = entity.averageSpeed,
            totalAscent = entity.totalAscent,
            totalDescent = entity.totalDescent,
            activeDurationMs = entity.activeDurationMs
        )
    }

    fun toEntitySession(domain: PedalSession): SessionEntity {
        return SessionEntity(
            syncUuid = domain.syncUuid.value,
            startTime = domain.startTime,
            endTime = domain.endTime,
            distanceKm = domain.distanceKm,
            averageSpeed = domain.averageSpeed,
            totalAscent = domain.totalAscent,
            totalDescent = domain.totalDescent,
            activeDurationMs = domain.activeDurationMs
        )
    }

    fun toDomainPoint(entity: PointEntity): PedalPoint {
        return PedalPoint(
            timestamp = entity.timestamp,
            latitude = entity.latitude,
            longitude = entity.longitude,
            altitude = entity.altitude,
            speed = entity.speed,
            accuracy = entity.accuracy
        )
    }

    fun toEntityPoint(domain: PedalPoint, sessionId: Long = 0): PointEntity {
        return PointEntity(
            sessionId = sessionId,
            timestamp = domain.timestamp,
            latitude = domain.latitude,
            longitude = domain.longitude,
            altitude = domain.altitude,
            speed = domain.speed,
            accuracy = domain.accuracy
        )
    }
}
