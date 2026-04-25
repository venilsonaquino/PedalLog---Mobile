package com.pedallog.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pedallog.app.data.model.PointEntity
import com.pedallog.app.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PedalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoints(points: List<PointEntity>): List<Long>

    @Query("SELECT * FROM sessions WHERE syncUuid = :syncUuid LIMIT 1")
    fun getSessionByUuid(syncUuid: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM points WHERE sessionId = (SELECT id FROM sessions WHERE syncUuid = :syncUuid) ORDER BY timestamp ASC")
    fun getPointsForSession(syncUuid: String): Flow<List<PointEntity>>

    @Query("DELETE FROM sessions WHERE syncUuid = :syncUuid")
    fun deleteSession(syncUuid: String)

}
