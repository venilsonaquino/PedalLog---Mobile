package com.pedallog.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.core.content.FileProvider
import com.pedallog.app.data.model.PointEntity
import java.io.File
import java.io.FileOutputStream

object AnimationModule {
    
    fun createGpsTraceGif(context: Context, points: List<PointEntity>): File? {
        if (points.isEmpty()) return null
        
        val width = 400
        val height = 400
        val padding = 40f
        
        // Find bounds
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon
        val scale = Math.min((width - 2 * padding) / lonRange, (height - 2 * padding) / latRange)
        
        fun getX(lon: Double) = (padding + (lon - minLon) * scale).toFloat()
        fun getY(lat: Double) = (height - (padding + (lat - minLat) * scale)).toFloat()
        
        val gifFile = File(context.cacheDir, "trace_animation.gif")
        val encoder = GifEncoder()
        encoder.start(FileOutputStream(gifFile))
        encoder.setSize(width, height)
        encoder.setDelay(50) // 20 fps approx
        
        val paintTrace = Paint().apply {
            color = Color.parseColor("#333333")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        
        val paintMarker = Paint().apply {
            color = Color.parseColor("#54e98a") // Esmeralda
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val paintGlow = Paint().apply {
            color = Color.parseColor("#54e98a")
            alpha = 100
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Generate frames (decimate points if too many for GIF)
        val frameStep = Math.max(1, points.size / 40) // Target ~40 frames
        
        for (i in 0 until points.size step frameStep) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)
            
            // Draw full path up to point i
            val path = Path()
            path.moveTo(getX(points[0].longitude), getY(points[0].latitude))
            for (j in 1..i) {
                path.lineTo(getX(points[j].longitude), getY(points[j].latitude))
            }
            canvas.drawPath(path, paintTrace)
            
            // Draw marker at current point
            val cx = getX(points[i].longitude)
            val cy = getY(points[i].latitude)
            canvas.drawCircle(cx, cy, 8f, paintGlow)
            canvas.drawCircle(cx, cy, 4f, paintMarker)
            
            encoder.addFrame(bitmap)
        }
        
        encoder.finish()
        return gifFile
    }

    fun getShareIntent(context: Context, file: File): android.content.Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/gif"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
