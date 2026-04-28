package com.pedallog.app.modules.session.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel responsável pela sincronização com o relógio (WearOS).
 * 
 * Object Calisthenics: Foco único em uma tarefa de infraestrutura/sync.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    fun requestManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            executeSyncRequest()
            _isSyncing.value = false
        }
    }

    private suspend fun executeSyncRequest() {
        try {
            val nodes = Wearable.getNodeClient(getApplication()).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(getApplication())
            nodes.forEach { messageClient.sendMessage(it.id, "/request_sync", null).await() }
        } catch (e: Exception) {
            // Log error or emit UI event
        }
    }
}
