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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by activityViewModels()

    /** GeoJSON waiting to be drawn once the page has fully loaded */
    private var pendingDetails: SessionMetrics? = null

    /** True once onPageFinished has fired for our map.html */
    private var pageLoaded = false

    // Date formatter for the header — e.g. "Dom, 26 abr · 12:47"
    private val headerDateFormat = SimpleDateFormat("EEE, dd MMM · HH:mm", Locale("pt", "BR"))

    // Minimal formatter for the technical UUID area at the bottom
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

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
        binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setSupportZoom(true)

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageLoaded = true
                    // Draw any details that arrived before the page was ready
                    pendingDetails?.let { d ->
                        drawToMap(d)
                        pendingDetails = null
                    }
                }
            }

            addJavascriptInterface(WebAppInterface(requireContext()), "Android")
            loadUrl("file:///android_asset/map.html")
        }

        binding.webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentSessionDetails.collect { details ->
                    if (details != null) {
                        updateUI(details)
                        updateMap(details)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (pageLoaded) {
            binding.webView.evaluateJavascript("javascript:invalidateAndFocus();", null)
        }
    }

    private fun updateUI(details: SessionMetrics) {
        val session = details.session

        // ── Item 1: Human-readable header ─────────────────────────────────────
        val startDate = Date(session.startTime)
        binding.tvDetailTitle.text = headerDateFormat.format(startDate)
            .replaceFirstChar { it.uppercase() }   // capitalize first letter

        // Small subtitle below the date (replaces the old UUID in the header)
        binding.tvDetailUuid.text = "Detalhe da Sessão"

        // Technical UUID at the bottom (discrete)
        binding.tvTechnicalUuid.text = "UUID · ${session.syncUuid}"

        // ── Metrics ────────────────────────────────────────────────────────────
        binding.tvMetricDistance.text = details.formattedDistance
        binding.tvMetricDuration.text = details.formattedDuration
        binding.tvMetricAvgSpeed.text = String.format(Locale.US, "%.1f km/h", details.avgSpeedKmH)
        binding.tvMetricMaxSpeed.text = String.format(Locale.US, "%.1f km/h", details.maxSpeedKmH)
        binding.tvMetricPoints.text = details.gpsPointsCount.toString()

        // ── Item 3: Speed chart ────────────────────────────────────────────────
        SpeedChartHelper.setup(binding.speedChart, details.trackPoints)
    }

    private fun updateMap(details: SessionMetrics) {
        if (details.geoJson == null) return
        if (!pageLoaded) {
            pendingDetails = details
            return
        }
        drawToMap(details)
    }

    /** Sends GeoJSON polyline + speed-point array to Leaflet */
    private fun drawToMap(details: SessionMetrics) {
        val geoJson = details.geoJson ?: return

        // 1. Draw the polyline
        val escapedGeoJson = geoJson.replace("'", "\\'")
        binding.webView.evaluateJavascript("javascript:drawTrack('$escapedGeoJson')", null)

        // 2. Send speed points for the popup
        if (details.trackPoints.isNotEmpty()) {
            val pointsJson = details.trackPoints.joinToString(",", "[", "]") { pt ->
                """{"lat":${pt.lat},"lng":${pt.lng},"spd":${pt.speedKmH}}"""
            }
            val escapedPoints = pointsJson.replace("'", "\\'")
            binding.webView.evaluateJavascript("javascript:drawPoints('$escapedPoints')", null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        pageLoaded = false
        pendingDetails = null
    }
}
