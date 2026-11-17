package com.climb.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.ceil
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

    private val grayPaint = Paint().apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = dp
        textSize = 10 * dp
    }
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

        // The area the graph is drawn in, the space around it holds the titles
        val left = 16 * dp
        val right = width - 16 * dp
        val top = 28 * dp
        val bottom = height - 32 * dp

        // How much to show, at least 4 m up and 1.5 m to each side
        val maxUp = max(4.0, ceil(points.maxOfOrNull { it.second } ?: 0.0))
        val maxSide = max(1.5, ceil(points.maxOfOrNull { abs(it.first) } ?: 0.0))

        // Turn meters into pixels
        fun pixelX(meters: Double) = (left + (meters + maxSide) / (2 * maxSide) * (right - left)).toFloat()
        fun pixelY(meters: Double) = (bottom - meters / maxUp * (bottom - top)).toFloat()

        // A grid line for every meter
        for (h in 0..maxUp.toInt()) {
            canvas.drawLine(left, pixelY(h.toDouble()), right, pixelY(h.toDouble()), grayPaint)
        }
        for (m in -maxSide.toInt()..maxSide.toInt()) {
            canvas.drawLine(pixelX(m.toDouble()), top, pixelX(m.toDouble()), bottom, grayPaint)
        }
        canvas.drawText("height (m)", left, 16 * dp, grayPaint)
        canvas.drawText("sideways (m)", (left + right) / 2 - 30 * dp, height - 10 * dp, grayPaint)

        if (points.size < 2) return

        // The path
        val path = Path()
        points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(pixelX(p.first), pixelY(p.second)) else path.lineTo(pixelX(p.first), pixelY(p.second))
        }
        canvas.drawPath(path, linePaint)

        // A dot where it started and a dot at the highest point
        val start = points.first()
        val highest = points.maxByOrNull { it.second }!!
        canvas.drawCircle(pixelX(start.first), pixelY(start.second), 5 * dp, linePaint)
        canvas.drawCircle(pixelX(highest.first), pixelY(highest.second), 5 * dp, linePaint)
    }
}
