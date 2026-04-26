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
import com.pedallog.app.R
import com.pedallog.app.databinding.FragmentSessionListBinding
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
        
        binding.btnClearAll.setOnClickListener {
            viewModel.deleteAllSessions()
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
                        
                        val totalDistance = sessions.sumOf { it.distanceKm }
                        binding.tvTotalDistance.text = String.format(Locale.US, "%.2f km", totalDistance)
                        
                        binding.tvSessionsSubHeader.text = "${sessions.size} pedais registrados"
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
