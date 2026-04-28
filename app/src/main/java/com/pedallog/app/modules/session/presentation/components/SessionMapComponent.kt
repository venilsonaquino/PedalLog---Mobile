package com.pedallog.app.modules.session.presentation.components

import android.webkit.WebView
import com.pedallog.app.modules.session.presentation.viewmodels.SessionMetrics
import com.pedallog.app.modules.session.presentation.viewmodels.TrackPoint

/**
 * Componente responsável pela abstração da renderização do mapa no WebView.
 * 
 * Object Calisthenics: Isola a complexidade do JS e WebView do Fragment.
 */
class SessionMapComponent(private val webView: WebView) {
    
    fun setup(onPageLoaded: () -> Unit) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                onPageLoaded()
            }
        }
        webView.loadUrl("file:///android_asset/map.html")
    }

    fun render(metrics: SessionMetrics) {
        renderTrack(metrics.geoJson)
        renderPoints(metrics.trackPoints)
    }

    private fun renderTrack(geoJson: String?) {
        val json = geoJson ?: return
        val escaped = json.replace("'", "\\'")
        webView.evaluateJavascript("javascript:drawTrack('$escaped')", null)
    }

    private fun renderPoints(points: List<TrackPoint>) {
        if (points.isEmpty()) return
        
        val pointsJson = points.joinToString(",", "[", "]") { pt ->
            """{"lat":${pt.lat},"lng":${pt.lng},"spd":${pt.speedKmH}}"""
        }
        val escaped = pointsJson.replace("'", "\\'")
        webView.evaluateJavascript("javascript:drawPoints('$escaped');", null)
    }

    fun invalidate() {
        webView.evaluateJavascript("javascript:invalidateAndFocus();", null)
    }

    fun destroy() {
        webView.stopLoading()
        webView.clearCache(false)
        webView.destroy()
    }
}
