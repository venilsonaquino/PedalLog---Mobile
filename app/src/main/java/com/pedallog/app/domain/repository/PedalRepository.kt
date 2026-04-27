package com.pedallog.app.domain.repository

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface PedalRepository : SessionRepository, PointRepository {
    suspend fun saveSessionWithPoints(session: PedalSession, points: List<PedalPoint>)
}

