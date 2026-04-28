package com.pedallog.app.modules.session.presentation.components

import androidx.recyclerview.widget.RecyclerView
import com.pedallog.app.databinding.ItemSessionBinding
import com.pedallog.app.modules.session.domain.entities.PedalSession
import com.pedallog.app.shared.domain.logic.SessionFormatter

/**
 * ViewHolder para exibir um resumo de uma sessão de pedal.
 * SRP: Gerencia apenas a vinculação de dados a uma View específica.
 */
class SessionViewHolder(
    private val binding: ItemSessionBinding,
    private val onSessionClick: (PedalSession) -> Unit,
    private val onDeleteClick: (PedalSession) -> Unit,
    private val onDownloadGpx: (PedalSession) -> Unit,
    private val onShareGif: (PedalSession) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var currentSession: PedalSession? = null

    init {
        binding.root.setOnClickListener { currentSession?.let { onSessionClick(it) } }
        binding.btnDelete.setOnClickListener { currentSession?.let { onDeleteClick(it) } }
        binding.btnShare.setOnClickListener { currentSession?.let { onShareGif(it) } }
        binding.btnDownload.setOnClickListener { currentSession?.let { onDownloadGpx(it) } }
    }

    fun bind(session: PedalSession) {
        currentSession = session
        val details = session.details
        
        binding.tvDate.text = SessionFormatter.formatDate(details.timeRange.start.milliseconds)
        binding.tvSessionTitle.text = "Sessão de Ciclismo"
        binding.tvDistance.text = SessionFormatter.formatDistance(details.metrics.distance.kilometers)
        binding.tvDuration.text = SessionFormatter.formatDuration(details.timeRange.calculateTotalDuration().milliseconds)
        binding.tvUuid.text = "UUID · ${session.id.value}"
    }
}
