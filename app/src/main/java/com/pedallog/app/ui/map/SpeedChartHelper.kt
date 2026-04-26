package com.pedallog.app.ui.map

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

object SpeedChartHelper {

    private val EMERALD = Color.parseColor("#2ECC71")
    private val EMERALD_ALPHA = Color.parseColor("#552ECC71")
    private val GRID_COLOR = Color.parseColor("#1e293b")
    private val AXIS_TEXT = Color.parseColor("#94a3b8")
    private val BG = Color.parseColor("#141422")

    fun setup(chart: LineChart, trackPoints: List<TrackPoint>) {
        if (trackPoints.isEmpty()) {
            chart.visibility = View.GONE
            return
        }
        chart.visibility = View.VISIBLE

        // Build entries: X = elapsed minutes, Y = speed km/h
        val entries = trackPoints.map { pt ->
            Entry((pt.timeOffsetMs / 60000f), pt.speedKmH)
        }

        val dataSet = LineDataSet(entries, "Velocidade").apply {
            color = EMERALD
            lineWidth = 2.2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(EMERALD_ALPHA, Color.TRANSPARENT)
            )
        }

        chart.apply {
            data = LineData(dataSet)
            setBackgroundColor(BG)

            // No description label
            description.isEnabled = false
            legend.isEnabled = false

            // Touch & zoom
            setTouchEnabled(true)
            setPinchZoom(true)
            setScaleEnabled(true)
            isDoubleTapToZoomEnabled = true
            isHighlightPerTapEnabled = false

            // X axis — bottom, time in minutes
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = AXIS_TEXT
                textSize = 10f
                gridColor = GRID_COLOR
                axisLineColor = GRID_COLOR
                granularity = 1f
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        "${value.toInt()}m"
                }
            }

            // Left axis — speed
            axisLeft.apply {
                textColor = AXIS_TEXT
                textSize = 10f
                gridColor = GRID_COLOR
                axisLineColor = GRID_COLOR
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        "${value.toInt()} km/h"
                }
            }

            // Disable right axis
            axisRight.isEnabled = false

            // Smooth animation on first load
            animateX(600)
            invalidate()
        }
    }
}
