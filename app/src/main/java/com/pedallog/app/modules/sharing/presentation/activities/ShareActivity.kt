package com.pedallog.app.modules.sharing.presentation.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.pedallog.app.databinding.ActivityShareBinding
import com.pedallog.app.databinding.ItemShareCardBinding
import com.pedallog.app.shared.domain.logic.SessionFormatter
import com.pedallog.app.shared.infraestructure.db.AppDatabase
import com.pedallog.app.modules.tracking.infraestructure.repositories.RoomPointRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("SESSION_ID")
        setupToolbar()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadData() {
        val id = sessionId ?: return
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val session = withContext(Dispatchers.IO) { db.sessionDao().getSessionByUuid(id) }
            val pointRepo = RoomPointRepository(db.pointDao())
            val points = pointRepo.getPointsForSession(com.pedallog.app.modules.session.domain.valueobjects.SessionId(id)).firstOrNull() ?: emptyList()

            session?.let { 
                val pointsJson = Gson().toJson(points.map { mapOf("lat" to it.coordinate.latitude, "lng" to it.coordinate.longitude) })
                setupViewPager(it, pointsJson)
            }
        }
    }

    private fun setupViewPager(session: com.pedallog.app.modules.session.infraestructure.db.models.SessionModel, pointsJson: String) {
        binding.viewPagerStyles.adapter = ShareStylesAdapter(session, pointsJson)
        binding.viewPagerStyles.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.indicator0.alpha = if (position == 0) 1.0f else 0.4f
                binding.indicator1.alpha = if (position == 1) 1.0f else 0.4f
            }
        })
    }

    inner class ShareStylesAdapter(
        private val session: com.pedallog.app.modules.session.infraestructure.db.models.SessionModel,
        private val pointsJson: String
    ) : RecyclerView.Adapter<ShareStylesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = 
            ViewHolder(ItemShareCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(session, pointsJson, position)
        override fun getItemCount() = 2

        inner class ViewHolder(val binding: ItemShareCardBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(session: com.pedallog.app.modules.session.infraestructure.db.models.SessionModel, pointsJson: String, style: Int) {
                binding.tvPreviewTitle.text = "Pedal Matinal"
                binding.tvPreviewDistance.text = String.format(Locale.US, "%.2f km", session.distanceKm)
                binding.tvPreviewTime.text = SessionFormatter.formatDuration(session.activeDurationMs)
                binding.tvPreviewElev.text = String.format(Locale.US, "%.0f m", session.totalAscent)
                
                binding.innerCard.setBackgroundResource(if (style == 1) com.pedallog.app.R.drawable.card_share_bg_emerald else com.pedallog.app.R.drawable.card_share_bg)
                
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
    }
}
