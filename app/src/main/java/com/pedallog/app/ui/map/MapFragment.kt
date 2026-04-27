package com.pedallog.app.ui.map

import android.annotation.SuppressLint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pedallog.app.R
import com.pedallog.app.databinding.FragmentMapBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by activityViewModels()

    private var pendingDetails: SessionMetrics? = null
    private var pageLoaded = false

    private val headerDateFormat = SimpleDateFormat("EEE, dd MMM · HH:mm", Locale("pt", "BR"))

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

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Item 3: Show loading indicator while waiting for data
        showLoading(true)

        setupBottomSheet()
        setupWebView()
        observeSession()
    }

    private fun setupBottomSheet() {
        val behavior = BottomSheetBehavior.from(binding.bottomSheetContainer)
        behavior.isFitToContents = false
        behavior.halfExpandedRatio = 0.42f
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.isHideable = false
        behavior.skipCollapsed = false
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
                    pendingDetails?.let { d ->
                        drawToMap(d)
                        pendingDetails = null
                    }
                }
            }

            addJavascriptInterface(WebAppInterface(requireContext()), "Android")
            loadUrl("file:///android_asset/map.html")
        }

        // Let WebView consume pan/pinch without CoordinatorLayout interference
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
                        showLoading(false)   // Item 3: hide spinner once data arrives
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

    private fun showLoading(visible: Boolean) {
        binding.loadingIndicator.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun updateUI(details: SessionMetrics) {
        val session = details.session

        // Header — human-readable date
        binding.tvDetailTitle.text = headerDateFormat
            .format(Date(session.startTime))
            .replaceFirstChar { it.uppercase() }
        binding.tvDetailUuid.text = "Detalhe da Sessão"

        // Technical UUID (bottom)
        binding.tvTechnicalUuid.text = "UUID · ${session.syncUuid}"

        // Metrics
        // Metrics (Split for independent styling)
        val distParts = details.formattedDistance.split(" ")
        binding.tvMetricDistanceValue.text = distParts.getOrNull(0) ?: "0.00"
        binding.tvMetricDistanceUnit.text  = distParts.getOrNull(1) ?: "km"

        binding.tvMetricDuration.text = details.formattedDuration

        binding.tvMetricAvgSpeedValue.text = String.format(Locale.US, "%.1f", details.avgSpeedKmH)
        binding.tvMetricMaxSpeedValue.text = String.format(Locale.US, "%.1f", details.maxSpeedKmH)
        binding.tvMetricPoints.text = details.gpsPointsCount.toString()

        // tvMetricProtocol is static "GZIP+CSV" for now as per reference

        // Item 1+2+3: Chart with live WebView reference for map sync
        SpeedChartHelper.setup(binding.speedChart, details.trackPoints, binding.webView)
    }

    private fun applyTextGradient(textView: TextView, startColorRes: Int, endColorRes: Int) {
        textView.post {
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            val textShader: Shader = LinearGradient(
                0f, 0f, 0f, textView.textSize,
                intArrayOf(
                    ContextCompat.getColor(requireContext(), startColorRes),
                    ContextCompat.getColor(requireContext(), endColorRes)
                ), null, Shader.TileMode.CLAMP
            )
            textView.paint.shader = textShader
            textView.invalidate()
        }
    }

    private fun updateMap(details: SessionMetrics) {
        if (details.geoJson == null) return
        if (!pageLoaded) { pendingDetails = details; return }
        drawToMap(details)
    }

    private fun drawToMap(details: SessionMetrics) {
        val geoJson = details.geoJson ?: return
        val escapedGeoJson = geoJson.replace("'", "\\'")
        binding.webView.evaluateJavascript("javascript:drawTrack('$escapedGeoJson')", null)

        if (details.trackPoints.isNotEmpty()) {
            val pointsJson = details.trackPoints.joinToString(",", "[", "]") { pt ->
                """{"lat":${pt.lat},"lng":${pt.lng},"spd":${pt.speedKmH}}"""
            }
            binding.webView.evaluateJavascript(
                "javascript:drawPoints('${pointsJson.replace("'", "\\'")}');", null
            )
        }
    }

    // ── Item 4: Memory leak prevention ────────────────────────────────────────
    override fun onDestroyView() {
        super.onDestroyView()

        // Clear chart listeners before nulling the binding
        SpeedChartHelper.clear(binding.speedChart)

        // Release WebView resources (clears cache, stops loads, frees memory)
        binding.webView.apply {
            stopLoading()
            clearCache(false)       // false = keep disk cache, clear memory cache
            clearHistory()
            loadUrl("about:blank")  // unload the page so JS is released
            destroy()
        }

        _binding = null
        pageLoaded = false
        pendingDetails = null
    }
}
