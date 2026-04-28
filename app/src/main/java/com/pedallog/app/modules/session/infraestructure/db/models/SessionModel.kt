package com.pedallog.app.modules.session.infraestructure.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa o Schema do Banco de Dados para uma sessão.
 * 
 * Camada de Infraestrutura.
 */
@Entity(tableName = "sessions")
data class SessionModel(
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
