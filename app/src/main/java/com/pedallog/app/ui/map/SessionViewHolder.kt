package com.pedallog.app.ui.map

import androidx.recyclerview.widget.RecyclerView
import com.pedallog.app.databinding.ItemSessionBinding
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionFormatter

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
        binding.root.setOnClickListener { currentSession?.let(onSessionClick) }
        binding.btnDelete.setOnClickListener { currentSession?.let(onDeleteClick) }
        binding.btnShare.setOnClickListener { currentSession?.let(onShareGif) }
        binding.btnDownload.setOnClickListener { currentSession?.let(onDownloadGpx) }
    }

    fun bind(session: PedalSession) {
        currentSession = session
        binding.tvDate.text = SessionFormatter.formatDate(session.startTime)
        binding.tvSessionTitle.text = "Sessão de Ciclismo"
        binding.tvDistance.text = SessionFormatter.formatDistance(session.distanceKm)
        binding.tvDuration.text = SessionFormatter.formatDuration(session.durationMs)
        binding.tvUuid.text = "UUID · ${session.syncUuid}"
    }
}
