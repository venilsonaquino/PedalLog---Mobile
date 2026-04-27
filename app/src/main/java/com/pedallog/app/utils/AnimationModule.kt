package com.pedallog.app.utils

import android.content.Context
import com.pedallog.app.domain.model.PedalPoint
import java.io.File
import java.io.FileOutputStream

/**
 * Módulo para geração de animações (GIF) a partir de pontos GPS.
 * SRP: Orquestra a criação do arquivo GIF usando o TracePainter.
 */
object AnimationModule {
    
    private const val GIF_WIDTH = 400
    private const val GIF_HEIGHT = 400
    private const val PADDING = 40f
    private const val TARGET_FRAMES = 40
    private const val FRAME_DELAY_MS = 50

    fun createGpsTraceGif(context: Context, points: List<PedalPoint>): File? {
        if (points.isEmpty()) return null
        
        val gifFile = File(context.cacheDir, "trace_animation.gif")
        val encoder = GifEncoder().apply {
            start(FileOutputStream(gifFile))
            setSize(GIF_WIDTH, GIF_HEIGHT)
            setDelay(FRAME_DELAY_MS)
        }
        
        val mapper = CoordinateMapper(GIF_WIDTH, GIF_HEIGHT, PADDING, points)
        val painter = TracePainter(GIF_WIDTH, GIF_HEIGHT, mapper)
        val frameStep = Math.max(1, points.size / TARGET_FRAMES)
        
        for (i in points.indices step frameStep) {
            val bitmap = painter.drawFrame(points, i)
            encoder.addFrame(bitmap)
        }
        
        encoder.finish()
        return gifFile
    }
}
