package com.pedallog.app.modules.session.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pedallog.app.shared.infraestructure.db.AppDatabase
import com.pedallog.app.modules.session.infraestructure.repositories.RoomSessionRepository
import com.pedallog.app.modules.session.presentation.viewmodels.HistoryViewModel

/**
 * Factory simples para injeção de dependências nos ViewModels.
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = AppDatabase.getDatabase(context)
        val repository = RoomSessionRepository(database.sessionDao())

        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(repository) as T
        }
        
        throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
    }
}
