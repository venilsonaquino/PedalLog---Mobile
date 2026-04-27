package com.pedallog.app.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedallog.app.databinding.ItemSessionBinding
import com.pedallog.app.domain.model.PedalSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onSessionClick: (PedalSession) -> Unit,
    private val onDeleteClick: (PedalSession) -> Unit,
    private val onDownloadGpx: (PedalSession) -> Unit,
    private val onShareGif: (PedalSession) -> Unit
) : ListAdapter<PedalSession, SessionAdapter.SessionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(private val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSessionClick(getItem(position))
                }
            }
            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
            binding.btnShare.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onShareGif(getItem(position))
                }
            }
            binding.btnDownload.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDownloadGpx(getItem(position))
                }
            }
        }

        fun bind(session: PedalSession) {
            val dateFormatLabel = SimpleDateFormat("MMM dd, yyyy", Locale("pt", "BR")) 
            val date = Date(session.startTime)
            binding.tvDate.text = dateFormatLabel.format(date)
            binding.tvSessionTitle.text = "Sessão de Ciclismo" // Placeholder for name
            
            binding.tvDistance.text = String.format(Locale.US, "%.1f km", session.distanceKm)
            binding.tvDuration.text = session.getFormattedDuration()
            binding.tvUuid.text = "UUID · ${session.syncUuid}"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PedalSession>() {
            override fun areItemsTheSame(oldItem: PedalSession, newItem: PedalSession): Boolean {
                return oldItem.syncUuid == newItem.syncUuid
            }

            override fun areContentsTheSame(oldItem: PedalSession, newItem: PedalSession): Boolean {
                return oldItem == newItem
            }
        }
    }
}
