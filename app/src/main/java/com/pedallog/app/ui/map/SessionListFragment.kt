package com.pedallog.app.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import android.content.Intent
import com.pedallog.app.R
import com.pedallog.app.databinding.FragmentSessionListBinding
import com.pedallog.app.domain.model.SessionFormatter
import kotlinx.coroutines.launch
import java.util.Locale

class SessionListFragment : Fragment() {

    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to share data with MapFragment
    private val viewModel: MapViewModel by activityViewModels()

    private lateinit var sessionAdapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSyncButton()
        observeViewModel()
    }

    private fun setupSyncButton() {
        binding.btnSync.setOnClickListener {
            viewModel.requestManualSync()
        }
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onSessionClick = { session ->
                viewModel.loadSessionTrack(session)
                findNavController().navigate(R.id.action_SessionListFragment_to_MapFragment)
            },
            onDeleteClick = { session ->
                viewModel.deleteSession(session.syncUuid)
            },
            onDownloadGpx = { session ->
                viewModel.exportSessionToDownloads(session)
            },
            onShareGif = { session ->
                val intent = Intent(requireContext(), ShareActivity::class.java).apply {
                    putExtra("SYNC_UUID", session.syncUuid)
                    putExtra("DISTANCE", String.format(Locale.US, "%.2f km", session.distanceKm))
                    putExtra("DURATION", SessionFormatter.formatDuration(session.durationMs))
                    putExtra("ELEVATION", String.format(Locale.US, "%.0f m", session.totalAscent))
                }
                startActivity(intent)
            }
        )
        binding.rvSessions.adapter = sessionAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sessions.collect { sessions ->
                        sessionAdapter.submitList(sessions)
                        binding.tvSessionsSubHeader.text = "${sessions.size} pedais registrados"
                    }
                }

                launch {
                    viewModel.totalDistanceKm.collect { totalDistance ->
                        binding.tvTotalDistance.text = String.format(Locale.US, "%.2f km", totalDistance)
                    }
                }

                launch {
                    viewModel.isSyncing.collect { isSyncing ->
                        binding.progressBar.visibility = if (isSyncing) View.VISIBLE else View.GONE
                        binding.btnSync.isEnabled = !isSyncing
                        binding.btnSync.alpha = if (isSyncing) 0.5f else 1.0f
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                android.widget.Toast.makeText(requireContext(), event.message, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            is UiEvent.ShareGpx -> {
                                // GPX sharing is handled by MapFragment — no-op here
                            }
                            is UiEvent.ShareGif -> {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/gif"
                                    putExtra(Intent.EXTRA_STREAM, event.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(Intent.createChooser(intent, "Compartilhar GIF da Rota"))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvSessions.adapter = null
        _binding = null
    }
}
