package com.pedallog.app.modules.session.presentation.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.pedallog.app.R
import com.pedallog.app.databinding.FragmentMapBinding
import com.pedallog.app.modules.session.presentation.components.SessionMapComponent
import com.pedallog.app.modules.session.presentation.components.SessionMetricsComponent
import com.pedallog.app.modules.session.presentation.components.SpeedChartHelper
import com.pedallog.app.modules.session.presentation.viewmodels.SessionAnalysisViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Análise de Sessão.
 * 
 * Object Calisthenics: Reduzido para < 50 linhas através da componentização.
 */
class MapFragment : Fragment(R.layout.fragment_map) {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val factory by lazy { com.pedallog.app.shared.presentation.ViewModelFactory(requireActivity().application) }
    private val analysisViewModel: SessionAnalysisViewModel by activityViewModels { factory }
    
    private lateinit var mapComp: SessionMapComponent
    private lateinit var metricsComp: SessionMetricsComponent

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)
        mapComp = SessionMapComponent(binding.webView).apply { setup { observeData() } }
        metricsComp = SessionMetricsComponent(binding)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            analysisViewModel.sessionDetails.collect { details ->
                details?.let { 
                    metricsComp.bind(it)
                    mapComp.render(it)
                    SpeedChartHelper.setup(binding.speedChart, it.trackPoints, binding.webView)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SpeedChartHelper.clear(binding.speedChart)
        mapComp.destroy()
        _binding = null
    }
}
