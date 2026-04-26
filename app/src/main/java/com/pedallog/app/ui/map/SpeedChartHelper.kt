package com.pedallog.app.ui.map

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

object SpeedChartHelper {

    private val EMERALD       = Color.parseColor("#54E98A")
    private val EMERALD_ALPHA = Color.parseColor("#3354E98A")
    private val HIGHLIGHT_CLR = Color.parseColor("#80FFFFFF")
    private val GRID_COLOR    = Color.parseColor("#1AFFFFFF") // Subtle grid
    private val AXIS_TEXT     = Color.parseColor("#BBCBBB")
    private val BG            = Color.parseColor("#201F1F")

    /**
     * Configures the chart and wires the value-selected listener.
     *
     * @param chart        The MPAndroidChart LineChart view
     * @param trackPoints  Ordered list of GPS track points
     * @param webView      WebView hosting Leaflet — receives moveMarker() calls
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    fun setup(
        chart: LineChart,
        trackPoints: List<TrackPoint>,
        webView: WebView? = null
    ) {
        if (trackPoints.isEmpty()) {
            chart.visibility = View.GONE
            return
        }
        chart.visibility = View.VISIBLE

        val totalMinutes = trackPoints.last().timeOffsetMs / 60000f
        val longSession  = totalMinutes > 60f   // use h:mm format above 60 min

        // ── Build entries: X = elapsed time, Y = speed km/h ─────────────────
        val entries = trackPoints.map { pt ->
            Entry(pt.timeOffsetMs / 60000f, pt.speedKmH)
        }

        val dataSet = LineDataSet(entries, "Velocidade").apply {
            color     = EMERALD
            lineWidth = 2.2f
            setDrawCircles(false)
            setDrawValues(false)
            mode          = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(EMERALD_ALPHA, Color.TRANSPARENT)
            )
            // Highlight vertical line style
            highLightColor  = HIGHLIGHT_CLR
            highlightLineWidth = 1.5f
        }

        chart.apply {
            data = LineData(dataSet)
            setBackgroundColor(BG)
            description.isEnabled = false
            legend.isEnabled      = false

            // Touch & zoom — pinch-zoom horizontal only
            setTouchEnabled(true)
            setPinchZoom(false)          // false = independent X/Y zoom
            isScaleXEnabled = true
            isScaleYEnabled = false      // lock Y so vertical drag scrolls the sheet
            isDoubleTapToZoomEnabled = true
            isHighlightPerTapEnabled = true
            isHighlightPerDragEnabled = true

            // ── X axis ───────────────────────────────────────────────────────
            xAxis.apply {
                position     = XAxis.XAxisPosition.BOTTOM
                textColor    = AXIS_TEXT
                textSize     = 10f
                gridColor    = GRID_COLOR
                axisLineColor = GRID_COLOR
                granularity  = if (longSession) 10f else 1f
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (longSession) {
                            val h = (value / 60).toInt()
                            val m = (value % 60).toInt()
                            "${h}h ${"%02d".format(m)}m"
                        } else {
                            "${value.toInt()}m"
                        }
                    }
                }
            }

            // ── Left axis (speed) ─────────────────────────────────────────────
            axisLeft.apply {
                textColor     = AXIS_TEXT
                textSize      = 10f
                gridColor     = GRID_COLOR
                axisLineColor = GRID_COLOR
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        "${value.toInt()} km/h"
                }
            }
            axisRight.isEnabled = false

            // ── Item 1: Chart ↔ Map sync ──────────────────────────────────────
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e == null || webView == null) return
                    // Find the nearest track point by time offset
                    val targetMin = e.x
                    val nearest = trackPoints.minByOrNull { pt ->
                        kotlin.math.abs(pt.timeOffsetMs / 60000f - targetMin)
                    } ?: return
                    // Move the marker on the Leaflet map
                    webView.evaluateJavascript(
                        "javascript:moveMarker(${nearest.lat}, ${nearest.lng}, ${nearest.speedKmH});",
                        null
                    )
                }

                override fun onNothingSelected() {
                    webView?.evaluateJavascript("javascript:hideMarker();", null)
                }
            })

            // ── Item 2: Prevent chart horizontal drag from closing BottomSheet ─
            // Only disallow parent interception when the user is performing a
            // horizontal swipe (chart pan/zoom). Vertical swipes are passed up
            // so the BottomSheet can still be dragged.
            onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(
                    me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {}

                override fun onChartGestureEnd(
                    me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                    // Release parent interception when gesture ends
                    chart.parent?.requestDisallowInterceptTouchEvent(false)
                }

                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}

                override fun onChartSingleTapped(me: MotionEvent?) {
                    // Single tap: let the value-selected listener handle it
                    chart.parent?.requestDisallowInterceptTouchEvent(false)
                }

                override fun onChartFling(
                    me1: MotionEvent?, me2: MotionEvent?, vX: Float, vY: Float
                ) {}

                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                    // Pinch-zoom: prevent parent from stealing touch
                    chart.parent?.requestDisallowInterceptTouchEvent(true)
                }

                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                    // Horizontal pan: disallow parent scroll; vertical: allow
                    val horizontal = kotlin.math.abs(dX) > kotlin.math.abs(dY)
                    chart.parent?.requestDisallowInterceptTouchEvent(horizontal)
                }
            }

            animateX(600)
            invalidate()
        }
    }

    /** Call this when the Fragment is destroyed to avoid leaking the listener. */
    fun clear(chart: LineChart) {
        chart.setOnChartValueSelectedListener(null)
        chart.onChartGestureListener = null
        chart.clear()
    }
}
