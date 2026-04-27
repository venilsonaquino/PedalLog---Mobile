package com.pedallog.app.ui.map

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.pedallog.app.databinding.ItemShareCardBinding
import com.pedallog.app.domain.model.PedalPoint
import org.json.JSONArray
import org.json.JSONObject

class ShareCardAdapter(
    private val points: List<PedalPoint>,
    private val distance: String,
    private val time: String,
    private val elevation: String
) : RecyclerView.Adapter<ShareCardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemShareCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = 2

    inner class CardViewHolder(private val binding: ItemShareCardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(styleIndex: Int) {
            binding.tvPreviewDistance.text = distance
            binding.tvPreviewTime.text = time
            binding.tvPreviewElev.text = elevation

            // Reset visibility
            binding.webViewMap.visibility = View.GONE
            binding.ivBackground.visibility = View.VISIBLE
            binding.ivBackground.imageAlpha = 255
            binding.ivFullBackground.visibility = View.GONE
            binding.tvBottomCheckOut.visibility = View.GONE
            binding.ivBottomLogo.visibility = View.GONE
            
            when (styleIndex) {
                0 -> applyTechnicalStyle()
                1 -> applyMapStyle()
            }
            
            // Draw static trace for style 0
            if (styleIndex == 0 && points.isNotEmpty()) {
                val bitmap = createTraceBitmap()
                binding.ivBackground.setImageBitmap(bitmap)
            }
        }

        private fun applyTechnicalStyle() {
            binding.shareCard.setBackgroundColor(Color.parseColor("#0F0F0F"))
            binding.innerCard.setBackgroundResource(com.pedallog.app.R.drawable.card_share_bg)
            binding.glowBorder.visibility = View.VISIBLE
            binding.llMetricsOverlay.setBackgroundResource(com.pedallog.app.R.drawable.gradient_shadow_bottom)
            
            setTextColor(Color.WHITE, Color.parseColor("#B3FFFFFF"))
            setIconTint(Color.parseColor("#B3FFFFFF"))
            binding.ivBrandingBike.setColorFilter(Color.parseColor("#54e98a"))
        }

        private fun applyMapStyle() {
            binding.shareCard.setBackgroundColor(Color.parseColor("#0F0F0F"))
            binding.innerCard.setBackgroundResource(com.pedallog.app.R.drawable.card_share_bg)
            binding.glowBorder.visibility = View.VISIBLE
            binding.llMetricsOverlay.setBackgroundResource(com.pedallog.app.R.drawable.gradient_shadow_bottom)
            
            binding.ivBackground.visibility = View.GONE
            binding.webViewMap.visibility = View.VISIBLE
            binding.webViewMap.apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        injectPoints()
                    }
                }
                loadUrl("file:///android_asset/map_share.html")
            }

            setTextColor(Color.WHITE, Color.parseColor("#B3FFFFFF"))
            setIconTint(Color.parseColor("#B3FFFFFF"))
            binding.ivBrandingBike.setColorFilter(Color.parseColor("#54e98a"))
        }

        private fun injectPoints() {
            val jsonArray = JSONArray()
            points.forEach {
                val obj = JSONObject()
                obj.put("lat", it.latitude)
                obj.put("lng", it.longitude)
                jsonArray.put(obj)
            }
            binding.webViewMap.evaluateJavascript("drawPoints('${jsonArray}')", null)
        }

        private fun setTextColor(main: Int, secondary: Int) {
            binding.tvPreviewTitle.setTextColor(main)
            binding.tvPreviewDistance.setTextColor(main)
            binding.tvPreviewTime.setTextColor(main)
            binding.tvPreviewElev.setTextColor(main)
            
            binding.tvDistLabel.setTextColor(secondary)
            binding.tvTimeLabel.setTextColor(secondary)
            binding.tvElevLabel.setTextColor(secondary)
            binding.tvBrandingText.setTextColor(secondary)
        }
        
        private fun setIconTint(color: Int) {
            binding.ivDistIcon.setColorFilter(color)
            binding.ivTimeIcon.setColorFilter(color)
            binding.ivElevIcon.setColorFilter(color)
        }

        private fun createTraceBitmap(): Bitmap {
            val width = 1000
            val height = 1200
            val padding = 150f
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            // Grid (only for tech style)
            val paintGrid = Paint().apply {
                color = Color.parseColor("#1AFFFFFF")
                strokeWidth = 1f
            }
            for (i in 0..width step 100) canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paintGrid)
            for (j in 0..height step 100) canvas.drawLine(0f, j.toFloat(), width.toFloat(), j.toFloat(), paintGrid)

            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            
            // Fix "amassado" look by accounting for latitude scale (Mercator-ish)
            val avgLat = (minLat + maxLat) / 2.0
            val cosFactor = Math.cos(Math.toRadians(avgLat))
            
            val latRange = Math.max(maxLat - minLat, 0.0001)
            val lonRange = Math.max((maxLon - minLon) * cosFactor, 0.0001)
            val scale = Math.min((width - 2 * padding) / lonRange, (height - 2 * padding) / latRange)
            
            fun getX(lon: Double) = (width / 2f + (lon - (minLon + maxLon) / 2.0) * cosFactor * scale).toFloat()
            fun getY(lat: Double) = (height / 2f - (lat - (minLat + maxLat) / 2.0) * scale).toFloat()
            
            val path = Path()
            path.moveTo(getX(points[0].longitude), getY(points[0].latitude))
            for (i in 1 until points.size) path.lineTo(getX(points[i].longitude), getY(points[i].latitude))
            
            val paintShadow = Paint().apply {
                color = Color.parseColor("#40000000")
                strokeWidth = 12f
                style = Paint.Style.STROKE
                isAntiAlias = true
                maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.save()
            canvas.translate(4f, 4f)
            canvas.drawPath(path, paintShadow)
            canvas.restore()

            val paintTrace = Paint().apply {
                color = Color.parseColor("#54e98a")
                strokeWidth = 8f
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, paintTrace)
            
            return bitmap
        }
    }
}
