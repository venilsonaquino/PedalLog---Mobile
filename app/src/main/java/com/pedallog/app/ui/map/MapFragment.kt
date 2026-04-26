package com.pedallog.app.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
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

    private val viewModel: MapViewModel by activityViewModels()

    /** GeoJSON waiting to be drawn once the page has fully loaded */
    private var pendingGeoJson: String? = null

    /** True once onPageFinished has fired for our map.html */
    private var pageLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupWebView()
        observeSession()
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        // Hardware acceleration for smooth rendering
        binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Smooth scrolling / pinch-to-zoom
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setSupportZoom(true)

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageLoaded = true
                    // Draw any GeoJSON that arrived before the page was ready
                    pendingGeoJson?.let { json ->
                        drawTrackJs(json)
                        pendingGeoJson = null
                    }
                }
            }

            addJavascriptInterface(WebAppInterface(requireContext()), "Android")
            loadUrl("file:///android_asset/map.html")
        }

        // Allow the WebView to capture touch events (pan / pinch) without
        // the parent ScrollView stealing them.
        binding.webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    // Tell parent(s) not to intercept so pinch & pan work
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // Let WebView handle the event itself
        }
    }

    private fun observeSession() {
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

    override fun onResume() {
        super.onResume()
        // Every time the user enters this screen, force Leaflet to
        // recalculate its container size and re-fit the route bounds.
        if (pageLoaded) {
            binding.webView.evaluateJavascript("javascript:invalidateAndFocus();", null)
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
        if (!pageLoaded) {
            // Page not ready yet — queue the GeoJSON
            pendingGeoJson = geoJsonString
            return
        }
        drawTrackJs(geoJsonString)
    }

    private fun drawTrackJs(geoJsonString: String) {
        // Escape single-quotes inside the JSON so the JS string literal is valid
        val escaped = geoJsonString.replace("'", "\\'")
        binding.webView.evaluateJavascript("javascript:drawTrack('$escaped')", null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        pageLoaded = false
        pendingGeoJson = null
    }
}
