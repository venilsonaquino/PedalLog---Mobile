package com.pedallog.app.modules.sharing.presentation.logic

import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.pedallog.app.databinding.ItemShareCardBinding
import com.pedallog.app.modules.session.infraestructure.db.models.SessionModel
import com.pedallog.app.shared.domain.logic.SessionFormatter
import java.util.Locale

object ShareDataBinder {

    fun bind(binding: ItemShareCardBinding, session: SessionModel, pointsJson: String) {
        binding.tvPreviewTitle.text = SessionFormatter.getSessionPeriodName(session.startTime)
        binding.tvPreviewDistance.text = SessionFormatter.formatDistance(session.distanceKm)
        binding.tvPreviewTime.text = SessionFormatter.formatDuration(session.activeDurationMs)
        binding.tvPreviewElev.text = String.format(Locale.US, "%.0f m", session.totalAscent)
        
        setupMap(binding.webViewMap, pointsJson)
    }

    private fun setupMap(webView: WebView, pointsJson: String) {
        webView.visibility = View.VISIBLE
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webView.evaluateJavascript("drawPoints('$pointsJson')", null)
            }
        }
        webView.loadUrl("file:///android_asset/map_share.html")
    }
}
