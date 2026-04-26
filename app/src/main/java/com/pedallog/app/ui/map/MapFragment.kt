package com.pedallog.app.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.pedallog.app.databinding.FragmentMapBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to share data with SessionListFragment
    private val viewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(WebAppInterface(requireContext()), "Android")
            loadUrl("file:///android_asset/map.html")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentSessionDetails.collect { details ->
                    if (details != null) {
                        updateUI(details)
                        if (details.geoJson != null) {
                            updateMap(details.geoJson)
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(details: SessionMetrics) {
        val session = details.session
        
        binding.tvDetailUuid.text = session.syncUuid
        binding.tvMetricUuid.text = session.syncUuid
        
        binding.tvMetricDistance.text = details.formattedDistance
        binding.tvMetricDuration.text = details.formattedDuration
        
        binding.tvMetricAvgSpeed.text = String.format(Locale.US, "%.1f km/h", details.avgSpeedKmH)
        binding.tvMetricMaxSpeed.text = String.format(Locale.US, "%.1f km/h", details.maxSpeedKmH)
        binding.tvMetricPoints.text = details.gpsPointsCount.toString()
    }

    private fun updateMap(geoJsonString: String) {
        val jsCommand = "javascript:drawTrack('$geoJsonString')"
        binding.webView.evaluateJavascript(jsCommand, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
