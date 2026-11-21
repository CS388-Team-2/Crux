package com.climb.companion.ui.session

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

// Draws an x y graph of a climb
// x is meters sideways from where the attempt started, y is meters of height
// The points come from AttemptTracker as text like "0,0;0.1,0.5"
class PathGraphView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Each point is x meters and height meters
    private var points = listOf<Pair<Double, Double>>()

    // Turns the saved text back into points and redraws
    fun setPath(text: String?) {
        points = (text ?: "").split(";").mapNotNull { part ->
            val numbers = part.split(",")
            val x = numbers.getOrNull(0)?.toDoubleOrNull()
            val y = numbers.getOrNull(1)?.toDoubleOrNull()
            if (x == null || y == null) null else Pair(x, y)
        }
        invalidate()
    }

    // Everything is drawn in dp so it looks the same on every phone
    private val dp = resources.displayMetrics.density

    private val linePaint = Paint().apply {
        color = Color.parseColor("#212121")
        strokeWidth = 3 * dp
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Only the way up is drawn
        val topIndex = points.indices.maxByOrNull { points[it].second } ?: return
        val shown = points.take(topIndex + 1)
        if (shown.size < 2) return

        // The path fills the pane
        val inset = 16 * dp
        val left = inset
        val right = width - inset
        val top = inset
        val bottom = height - inset

        val minX = shown.minOf { it.first }
        val maxX = shown.maxOf { it.first }
        val minY = shown.minOf { it.second }
        val maxY = shown.maxOf { it.second }
        val rangeX = max(maxX - minX, 0.01)
        val rangeY = max(maxY - minY, 0.01)
        val midX = (minX + maxX) / 2
        val midY = (minY + maxY) / 2
        val pixelsPerMeter = minOf((right - left) / rangeX, (bottom - top) / rangeY)

        // Turn meters into pixels
        fun pixelX(meters: Double) = ((left + right) / 2 + (meters - midX) * pixelsPerMeter).toFloat()
        fun pixelY(meters: Double) = ((top + bottom) / 2 - (meters - midY) * pixelsPerMeter).toFloat()

        // The path
        val path = Path()
        shown.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(pixelX(p.first), pixelY(p.second)) else path.lineTo(pixelX(p.first), pixelY(p.second))
        }
        canvas.drawPath(path, linePaint)

        // A dot where it started and a dot at the highest point
        val start = shown.first()
        val highest = shown.last()
        canvas.drawCircle(pixelX(start.first), pixelY(start.second), 5 * dp, linePaint)
        canvas.drawCircle(pixelX(highest.first), pixelY(highest.second), 5 * dp, linePaint)
    }
}
