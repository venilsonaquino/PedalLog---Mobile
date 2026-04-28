package com.pedallog.app.shared.infraestructure.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pedallog.app.modules.session.infraestructure.db.SessionDao
import com.pedallog.app.modules.session.infraestructure.db.models.SessionModel
import com.pedallog.app.modules.tracking.infraestructure.db.PointDao
import com.pedallog.app.modules.tracking.infraestructure.db.models.PointModel

/**
 * Agregador técnico do Banco de Dados Room.
 * 
 * Localizado em 'shared' pois serve de configuração global para o motor de persistência.
 */
@Database(entities = [SessionModel::class, PointModel::class], version = 4, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun sessionDao(): SessionDao
    abstract fun pointDao(): PointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pedal_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
