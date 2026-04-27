package com.pedallog.app.ui.map

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.domain.usecase.ExportGpxUseCase
import com.pedallog.app.domain.usecase.LoadSessionPointsUseCase
import com.pedallog.app.utils.AnimationModule
import com.pedallog.app.utils.GpxUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel responsável pelas operações de exportação (GPX, GIF, Imagens).
 * SRP: Isola lógicas de IO e geração de arquivos da UI principal.
 */
class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val exportGpxUseCase = ExportGpxUseCase()
    private lateinit var loadSessionPointsUseCase: LoadSessionPointsUseCase

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun init(loadSessionPointsUseCase: LoadSessionPointsUseCase) {
        this.loadSessionPointsUseCase = loadSessionPointsUseCase
    }

    fun exportSessionToDownloads(session: PedalSession) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val points = loadSessionPointsUseCase(session.syncUuid).firstOrNull() ?: emptyList()
                if (points.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Sem pontos GPS para exportar."))
                    return@launch
                }

                val gpxContent = exportGpxUseCase(session, points)
                val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(session.startTime))
                val fileName = "PedalLog_$dateTag"
                
                val uri = withContext(Dispatchers.IO) {
                    GpxUtils.saveGpxToDownloads(getApplication(), gpxContent, fileName)
                }

                if (uri != null) {
                    _uiEvent.emit(UiEvent.ShowToast("GPX salvo na pasta Downloads!"))
                } else {
                    _uiEvent.emit(UiEvent.ShowToast("Erro ao salvar arquivo."))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast("Erro: ${e.message}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun generateGifForSession(session: PedalSession) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val points = loadSessionPointsUseCase(session.syncUuid).firstOrNull() ?: emptyList()
                if (points.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowToast("Sem pontos GPS para o GIF."))
                    return@launch
                }

                val gifFile = withContext(Dispatchers.IO) {
                    AnimationModule.createGpsTraceGif(getApplication(), points)
                }

                if (gifFile != null) {
                    val uri = FileProvider.getUriForFile(
                        getApplication(),
                        "${getApplication<Application>().packageName}.provider",
                        gifFile
                    )
                    _uiEvent.emit(UiEvent.ShareGif(uri))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast("Erro ao gerar GIF: ${e.message}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Used by ShareActivity to load points for the card
    suspend fun getPointsForSession(syncUuid: SessionId): List<com.pedallog.app.domain.model.PedalPoint> {
        return loadSessionPointsUseCase(syncUuid).firstOrNull() ?: emptyList()
    }
}
