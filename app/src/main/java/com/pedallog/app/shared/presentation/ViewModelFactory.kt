package com.pedallog.app.shared.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pedallog.app.modules.session.application.use_cases.ExportGpxUseCase
import com.pedallog.app.modules.session.application.use_cases.LoadSessionPointsUseCase
import com.pedallog.app.modules.session.infraestructure.repositories.RoomSessionRepository
import com.pedallog.app.modules.session.presentation.viewmodels.ExportViewModel
import com.pedallog.app.modules.session.presentation.viewmodels.HistoryViewModel
import com.pedallog.app.modules.session.presentation.viewmodels.SessionAnalysisViewModel
import com.pedallog.app.modules.session.presentation.viewmodels.SyncViewModel
import com.pedallog.app.modules.tracking.infraestructure.repositories.RoomPointRepository
import com.pedallog.app.shared.infraestructure.db.AppDatabase

/**
 * Factory centralizada para instanciar ViewModels com suas dependências.
 * SRP: Centraliza a criação de instâncias complexas.
 */
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = AppDatabase.getDatabase(application)
        val sessionRepo = RoomSessionRepository(database.sessionDao())
        val pointRepo = RoomPointRepository(database.pointDao())

        return when {
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(sessionRepo) as T
            }
            modelClass.isAssignableFrom(SyncViewModel::class.java) -> {
                SyncViewModel(application) as T
            }
            modelClass.isAssignableFrom(ExportViewModel::class.java) -> {
                ExportViewModel(application, LoadSessionPointsUseCase(pointRepo)) as T
            }
            modelClass.isAssignableFrom(SessionAnalysisViewModel::class.java) -> {
                SessionAnalysisViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
