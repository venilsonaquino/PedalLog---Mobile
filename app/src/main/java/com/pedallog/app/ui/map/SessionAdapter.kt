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
    private val onDeleteClick: (PedalSession) -> Unit
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
        }

        fun bind(session: PedalSession) {
            val dateFormat = SimpleDateFormat("dd 'de' MMM. 'de' yyyy", Locale("pt", "BR"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val date = Date(session.startTime)
            binding.tvDate.text = dateFormat.format(date)
            binding.tvTime.text = timeFormat.format(date)
            
            binding.tvDistance.text = String.format(Locale.US, "%.2f km", session.distanceKm)
            
            // Show start time in top right (tvTime) and duration in metrics row (tvDuration)
            binding.tvDuration.text = session.getFormattedDuration()
            
            binding.tvUuid.text = "ID: ${session.syncUuid}"
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
