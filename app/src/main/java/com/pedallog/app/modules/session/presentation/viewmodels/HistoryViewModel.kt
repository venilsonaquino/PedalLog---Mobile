package com.pedallog.app.modules.session.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.modules.session.domain.repositories.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsável pela listagem de sessões e métricas acumuladas.
 * 
 * SOLID: Segue o Princípio da Responsabilidade Única (SRP).
 * Object Calisthenics: Classes pequenas (< 50 linhas).
 */
class HistoryViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val sessions: StateFlow<List<PedalSession>> = sessionRepository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDistanceKm: StateFlow<Double> = sessions.map { list ->
        list.sumOf { it.details.metrics.distance.kilometers }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
