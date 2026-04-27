package com.pedallog.app.ui.map

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.BlurMaskFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.pedallog.app.databinding.ActivityShareBinding
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.repository.PedalRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private var syncUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        syncUuid = intent.getStringExtra("SYNC_UUID")
        val distance = intent.getStringExtra("DISTANCE") ?: "0.0 km"
        val duration = intent.getStringExtra("DURATION") ?: "00:00:00"
        val elev = intent.getStringExtra("ELEVATION") ?: "0 m"

        setupUI(distance, duration, elev)
        loadTrace()
    }

    private fun setupUI(distance: String, duration: String, elev: String) {
        binding.tvPreviewDistance.text = distance
        binding.tvPreviewTime.text = duration
        binding.tvPreviewElev.text = elev
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.btnWhatsApp.setOnClickListener { shareCardImage("com.whatsapp") }
        binding.btnInstagram.setOnClickListener { shareCardImage("com.instagram.android") }
        binding.btnSaveImage.setOnClickListener { saveCardToGallery() }
        binding.btnMore.setOnClickListener { shareCardImage(null) }
    }

    private fun loadTrace() {
        val uuid = syncUuid ?: return
        val database = AppDatabase.getDatabase(application)
        val repository = PedalRepositoryImpl(database)

        CoroutineScope(Dispatchers.IO).launch {
            val points = repository.getPointsForSession(uuid).firstOrNull() ?: emptyList()
            if (points.isNotEmpty()) {
                val bitmap = createStaticTraceBitmap(points)
                withContext(Dispatchers.Main) {
                    binding.ivBackground.setImageBitmap(bitmap)
                    binding.ivBackground.alpha = 1.0f
                }
            }
        }
    }

    private fun createStaticTraceBitmap(points: List<com.pedallog.app.domain.model.PedalPoint>): Bitmap {
        val width = 1000
        val height = 1200
        val padding = 150f
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        // Subtle Grid (10% opacity)
        val paintGrid = Paint().apply {
            color = Color.parseColor("#1AFFFFFF")
            strokeWidth = 1f
        }
        for (i in 0..width step 100) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paintGrid)
        }
        for (j in 0..height step 100) {
            canvas.drawLine(0f, j.toFloat(), width.toFloat(), j.toFloat(), paintGrid)
        }

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        
        val latRange = Math.max(maxLat - minLat, 0.0001)
        val lonRange = Math.max(maxLon - minLon, 0.0001)
        val scale = Math.min((width - 2 * padding) / lonRange, (height - 2 * padding) / latRange)
        
        fun getX(lon: Double) = (padding + (lon - minLon) * scale).toFloat()
        fun getY(lat: Double) = (height - (padding + (lat - minLat) * scale)).toFloat()
        
        val path = Path()
        path.moveTo(getX(points[0].longitude), getY(points[0].latitude))
        for (i in 1 until points.size) {
            path.lineTo(getX(points[i].longitude), getY(points[i].latitude))
        }
        
        // Trace Shadow
        val paintShadow = Paint().apply {
            color = Color.parseColor("#80000000")
            strokeWidth = 12f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.save()
        canvas.translate(4f, 4f)
        canvas.drawPath(path, paintShadow)
        canvas.restore()

        // Main Trace (Emerald)
        val paintTrace = Paint().apply {
            color = Color.parseColor("#54e98a")
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, paintTrace)

        // End Point Glow
        val lastPoint = points.last()
        val ex = getX(lastPoint.longitude)
        val ey = getY(lastPoint.latitude)
        
        val paintGlow = Paint().apply {
            color = Color.parseColor("#8054e98a")
            isAntiAlias = true
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(ex, ey, 12f, paintGlow)
        
        val paintCore = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            isAntiAlias = true
        }
        canvas.drawCircle(ex, ey, 4f, paintCore)
        
        return bitmap
    }

    private fun shareCardImage(packageName: String?) {
        val bitmap = captureView(binding.shareCard)
        val uri = saveBitmapToCache(bitmap) ?: return
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (packageName != null) {
                setPackage(packageName)
            }
        }
        
        try {
            startActivity(Intent.createChooser(intent, "Compartilhar via"))
        } catch (e: Exception) {
            Toast.makeText(this, "App não encontrado", Toast.LENGTH_SHORT).show()
            if (packageName != null) {
                shareCardImage(null)
            }
        }
    }

    private fun saveCardToGallery() {
        val bitmap = captureView(binding.shareCard)
        // Implementation for saving to MediaStore would go here
        Toast.makeText(this, "Salvo na Galeria (Simulado)", Toast.LENGTH_SHORT).show()
    }

    private fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/share_card.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(this, "$packageName.provider", File(cachePath, "share_card.png"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
