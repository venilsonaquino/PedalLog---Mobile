package com.pedallog.app.modules.session.presentation.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pedallog.app.R
import com.pedallog.app.databinding.FragmentSessionListBinding
import com.pedallog.app.modules.session.presentation.components.SessionAdapter
import com.pedallog.app.modules.session.presentation.viewmodels.*
import kotlinx.coroutines.launch

class SessionListFragment : Fragment(R.layout.fragment_session_list) {
    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!

    private val factory by lazy { com.pedallog.app.shared.presentation.ViewModelFactory(requireActivity().application) }
    
    private val historyViewModel: HistoryViewModel by activityViewModels { factory }
    private val syncViewModel: SyncViewModel by activityViewModels { factory }
    private val exportViewModel: ExportViewModel by activityViewModels { factory }
    private val analysisViewModel: SessionAnalysisViewModel by activityViewModels { factory }

    private lateinit var adapter: SessionAdapter
    private var currentSessionIdForShare: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSessionListBinding.bind(view)
        setupRecyclerView()
        observeData()
        
        binding.btnSync.setOnClickListener {
            syncViewModel.requestManualSync()
        }
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            onSessionClick = { session ->
                analysisViewModel.loadSession(session)
                findNavController().navigate(R.id.action_sessionList_to_map)
            },
            onDeleteClick = { /* historyViewModel.deleteSession(it.id) */ },
            onDownloadGpx = { exportViewModel.exportSessionToDownloads(it) },
            onShareGif = { 
                currentSessionIdForShare = it.id.value
                exportViewModel.generateGifForSession(it) 
            }
        )
        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.sessions.collect { adapter.submitList(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.totalDistanceKm.collect { 
                binding.tvTotalDistance.text = String.format("%.1f km", it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            exportViewModel.uiEvent.collect { event ->
                when (event) {
                    is UiEvent.ShowToast -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiEvent.ShareGif -> {
                        try {
                            val intent = Intent().apply {
                                setClassName(requireContext().packageName, "com.pedallog.app.modules.sharing.presentation.activities.ShareActivity")
                                putExtra("SESSION_ID", currentSessionIdForShare)
                                putExtra("GIF_URI", event.uri)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback caso a Activity não seja encontrada
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/gif"
                                putExtra(Intent.EXTRA_STREAM, event.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(intent, "Compartilhar trajeto"))
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
