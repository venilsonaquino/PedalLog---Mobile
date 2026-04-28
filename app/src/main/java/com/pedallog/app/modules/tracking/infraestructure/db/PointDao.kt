package com.pedallog.app.modules.tracking.infraestructure.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pedallog.app.modules.tracking.infraestructure.db.models.PointModel
import kotlinx.coroutines.flow.Flow

/**
 * DAO especializado em operações da entidade Point.
 */
@Dao
interface PointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoints(points: List<PointModel>): List<Long>

    @Query("SELECT * FROM points WHERE sessionUuid = :syncUuid ORDER BY timestamp ASC")
    fun getPointsForSession(syncUuid: String): Flow<List<PointModel>>

    @Query("DELETE FROM points WHERE sessionUuid = :sessionUuid")
    fun deletePointsBySessionUuid(sessionUuid: String)
    
    @Query("DELETE FROM points")
    fun deleteAllPoints()
}
