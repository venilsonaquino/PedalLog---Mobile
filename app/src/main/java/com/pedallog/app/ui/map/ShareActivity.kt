package com.pedallog.app.ui.map

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.pedallog.app.R
import com.pedallog.app.databinding.ActivityShareBinding
import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.SessionId
import com.pedallog.app.data.db.AppDatabase
import com.pedallog.app.data.repository.PedalRepositoryImpl
import com.pedallog.app.domain.usecase.LoadSessionPointsUseCase
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private lateinit var viewModel: ExportViewModel
    private var syncUuid: SessionId? = null
    private var points: List<PedalPoint> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        syncUuid = intent.getStringExtra("SYNC_UUID")?.let { SessionId(it) }
        val distance = intent.getStringExtra("DISTANCE") ?: "0.0 km"
        val duration = intent.getStringExtra("DURATION") ?: "00:00:00"
        val elev = intent.getStringExtra("ELEVATION") ?: "0 m"

        setupViewModel()
        setupUI(distance, duration, elev)
        loadDataAndSetupAdapter(distance, duration, elev)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ExportViewModel::class.java]
        val database = AppDatabase.getDatabase(application)
        val repository = PedalRepositoryImpl(database)
        viewModel.init(LoadSessionPointsUseCase(repository))

        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                if (event is UiEvent.ShowToast) {
                    Toast.makeText(this@ShareActivity, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUI(distance: String, duration: String, elev: String) {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.btnWhatsApp.setOnClickListener { shareCurrentCard("com.whatsapp") }
        binding.btnInstagram.setOnClickListener { shareCurrentCard("com.instagram.android") }
        binding.btnSaveImage.setOnClickListener { saveCurrentCardToGallery() }
        binding.btnMore.setOnClickListener { shareCurrentCard(null) }

        binding.viewPagerStyles.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
            }
        })
    }

    private fun updateIndicators(position: Int) {
        val active = R.drawable.indicator_active
        val inactive = R.drawable.indicator_inactive
        binding.indicator0.setBackgroundResource(if (position == 0) active else inactive)
        binding.indicator1.setBackgroundResource(if (position == 1) active else inactive)
    }

    private fun loadDataAndSetupAdapter(dist: String, dur: String, el: String) {
        val uuid = syncUuid ?: return
        lifecycleScope.launch {
            points = viewModel.getPointsForSession(uuid)
            val adapter = ShareCardAdapter(points, dist, dur, el)
            binding.viewPagerStyles.adapter = adapter
        }
    }

    private fun shareCurrentCard(packageName: String?) {
        val currentView = getCurrentCardView() ?: return
        captureViewPixelCopy(currentView) { bitmap ->
            if (bitmap == null) {
                Toast.makeText(this, "Erro ao capturar imagem", Toast.LENGTH_SHORT).show()
                return@captureViewPixelCopy
            }
            val uri = saveBitmapToCache(bitmap) ?: return@captureViewPixelCopy
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (packageName != null) setPackage(packageName)
            }
            
            try {
                startActivity(Intent.createChooser(intent, "Compartilhar via"))
            } catch (e: Exception) {
                Toast.makeText(this, "App não encontrado", Toast.LENGTH_SHORT).show()
                if (packageName != null) shareCurrentCard(null)
            }
        }
    }

    private fun saveCurrentCardToGallery() {
        val currentView = getCurrentCardView() ?: return
        captureViewPixelCopy(currentView) { bitmap ->
            // MediaStore logic omitted for brevity
            Toast.makeText(this, "Salvo na Galeria (Simulado)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentCardView(): View? {
        val recyclerView = binding.viewPagerStyles.getChildAt(0) as? RecyclerView
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(binding.viewPagerStyles.currentItem)
        return viewHolder?.itemView?.findViewById(R.id.innerCard)
    }

    private fun captureViewPixelCopy(view: View, callback: (Bitmap?) -> Unit) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val location = IntArray(2)
        view.getLocationInWindow(location)
        
        try {
            android.view.PixelCopy.request(
                window,
                android.graphics.Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                bitmap,
                { copyResult ->
                    if (copyResult == android.view.PixelCopy.SUCCESS) {
                        callback(bitmap)
                    } else {
                        callback(null)
                    }
                },
                android.os.Handler(android.os.Looper.getMainLooper())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "share_card.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
