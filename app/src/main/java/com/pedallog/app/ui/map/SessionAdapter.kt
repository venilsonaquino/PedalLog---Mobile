package com.pedallog.app.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.pedallog.app.databinding.ItemSessionBinding
import com.pedallog.app.domain.model.PedalSession

/**
 * Adapter para a lista de sessões de pedal.
 * SRP: Gerencia a criação de ViewHolders e a atualização da lista via DiffUtil.
 */
class SessionAdapter(
    private val onSessionClick: (PedalSession) -> Unit,
    private val onDeleteClick: (PedalSession) -> Unit,
    private val onDownloadGpx: (PedalSession) -> Unit,
    private val onShareGif: (PedalSession) -> Unit
) : ListAdapter<PedalSession, SessionViewHolder>(SessionDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SessionViewHolder(
            binding, onSessionClick, onDeleteClick, onDownloadGpx, onShareGif
        )
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val SessionDiffCallback = object : DiffUtil.ItemCallback<PedalSession>() {
            override fun areItemsTheSame(oldItem: PedalSession, newItem: PedalSession) =
                oldItem.syncUuid == newItem.syncUuid

            override fun areContentsTheSame(oldItem: PedalSession, newItem: PedalSession) =
                oldItem == newItem
        }
    }
}
