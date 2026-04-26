package com.pedallog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncUuid: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val averageSpeed: Float,
    val totalAscent: Double,
    val totalDescent: Double,
    val activeDurationMs: Long = 0
)
