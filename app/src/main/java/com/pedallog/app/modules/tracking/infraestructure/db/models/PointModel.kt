package com.pedallog.app.modules.tracking.infraestructure.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa o Schema do Banco de Dados para um ponto de GPS.
 */
@Entity(tableName = "points")
data class PointModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUuid: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val accuracy: Float
)
