package com.pedallog.app.modules.session.infraestructure.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pedallog.app.modules.session.infraestructure.db.models.SessionModel
import kotlinx.coroutines.flow.Flow

/**
 * DAO especializado em operações da entidade Session.
 * 
 * SOLID: Segue o Princípio da Responsabilidade Única (SRP).
 */
@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SessionModel): Long

    @Query("SELECT * FROM sessions WHERE syncUuid = :syncUuid LIMIT 1")
    fun getSessionByUuid(syncUuid: String): SessionModel?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionModel>>

    @Query("DELETE FROM sessions WHERE syncUuid = :syncUuid")
    fun deleteSession(syncUuid: String)

    @Query("DELETE FROM sessions")
    fun deleteAllSessions()
}
