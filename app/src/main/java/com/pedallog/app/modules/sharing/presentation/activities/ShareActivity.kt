package com.pedallog.app.modules.sharing.presentation.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pedallog.app.databinding.ActivityShareBinding
import com.pedallog.app.modules.sharing.presentation.logic.ShareDataBinder
import com.pedallog.app.shared.infraestructure.db.AppDatabase
import com.pedallog.app.modules.tracking.infraestructure.repositories.RoomPointRepository
import com.pedallog.app.shared.utils.BitmapUtils
import com.pedallog.app.shared.utils.ShareIntentUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("SESSION_ID")
        setupUI()
        loadData()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.btnWhatsApp.setOnClickListener { shareTo("whatsapp") }
        binding.btnInstagram.setOnClickListener { shareTo("instagram") }
        binding.btnSaveImage.setOnClickListener { shareTo("save") }
        binding.btnMore.setOnClickListener { shareTo("more") }
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
                ShareDataBinder.bind(binding.cardView, it, pointsJson)
            }
        }
    }

    private fun shareTo(target: String) {
        val bitmap = BitmapUtils.captureView(binding.cardView.innerCard)
        val uri = BitmapUtils.saveBitmapToGallery(this, bitmap, "pedal_log_${sessionId}") ?: return
        
        when (target) {
            "whatsapp" -> ShareIntentUtils.shareToWhatsApp(this, uri)
            "instagram" -> ShareIntentUtils.shareToInstagram(this, uri)
            "save" -> Toast.makeText(this, "Imagem salva na galeria!", Toast.LENGTH_SHORT).show()
            "more" -> ShareIntentUtils.shareImage(this, uri)
        }
    }
}
