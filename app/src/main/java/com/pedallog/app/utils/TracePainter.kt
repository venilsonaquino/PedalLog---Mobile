package com.pedallog.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.pedallog.app.domain.model.PedalPoint

class TracePainter(
    private val width: Int,
    private val height: Int,
    private val mapper: CoordinateMapper
) {
    private val paintTrace = createPaint("#333333", 4f, Paint.Style.STROKE)
    private val paintMarker = createPaint("#54e98a", 0f, Paint.Style.FILL)
    private val paintGlow = createPaint("#54e98a", 0f, Paint.Style.FILL).apply { alpha = 100 }

    private fun createPaint(hexColor: String, width: Float, style: Paint.Style) = Paint().apply {
        color = Color.parseColor(hexColor)
        if (width > 0) strokeWidth = width
        this.style = style
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    fun drawFrame(points: List<PedalPoint>, index: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = Path().apply {
            moveTo(mapper.getX(points[0].longitude), mapper.getY(points[0].latitude))
            for (j in 1..index) {
                if (points[j - 1].segmentBreak == 1) {
                    moveTo(mapper.getX(points[j].longitude), mapper.getY(points[j].latitude))
                } else {
                    lineTo(mapper.getX(points[j].longitude), mapper.getY(points[j].latitude))
                }
            }
        }
        canvas.drawPath(path, paintTrace)
        val cx = mapper.getX(points[index].longitude)
        val cy = mapper.getY(points[index].latitude)
        canvas.drawCircle(cx, cy, 8f, paintGlow)
        canvas.drawCircle(cx, cy, 4f, paintMarker)
        return bitmap
    }
}
