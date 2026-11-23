package com.climb.companion.ui.feed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.climb.companion.data.BoardLayout
import com.climb.companion.data.BoardPoint
import com.climb.companion.data.holdRole
import com.climb.companion.data.parseKilterFrames
import kotlin.math.min

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val holes = ArrayList<BoardPoint>()
    private val holds = ArrayList<Pair<BoardPoint, String>>()
    private var minX = 0
    private var maxX = 1
    private var minY = 0
    private var maxY = 1

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFBDBDBD.toInt()
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF212121.toInt()
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF424242.toInt()
        style = Paint.Style.FILL
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF212121.toInt()
        style = Paint.Style.STROKE
    }

    fun show(layout: BoardLayout, frames: String) {
        val marks = parseKilterFrames(frames)
        val setId = marks
            .mapNotNull { layout.placements[it.placementId]?.setId }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: BoardLayout.BOLT_SET
        holes.clear()
        holes.addAll(layout.grid(setId))
        holds.clear()
        for (mark in marks) {
            val placement = layout.placements[mark.placementId] ?: continue
            val role = holdRole(mark.roleId, layout.roles) ?: continue
            holds += placement.point to role
        }
        val points = holes + holds.map { it.first }
        val xs = points.map { it.x }
        val ys = points.map { it.y }
        minX = xs.minOrNull() ?: BoardLayout.GRID_LEFT
        maxX = xs.maxOrNull() ?: BoardLayout.GRID_RIGHT
        minY = ys.minOrNull() ?: BoardLayout.GRID_BOTTOM
        maxY = ys.maxOrNull() ?: BoardLayout.GRID_TOP
        if (minX == maxX) maxX = minX + 1
        if (minY == maxY) maxY = minY + 1
        contentDescription = "Hold layout"
        invalidate()
    }

    fun clear() {
        holes.clear()
        holds.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (holes.isEmpty() || width == 0 || height == 0) return
        val pad = 16f * resources.displayMetrics.density
        val spanX = (maxX - minX).toFloat()
        val spanY = (maxY - minY).toFloat()
        val scale = min((width - pad * 2) / spanX, (height - pad * 2) / spanY)
        val extraX = (width - pad * 2 - spanX * scale) / 2f
        val extraY = (height - pad * 2 - spanY * scale) / 2f
        val gap = typicalGap()
        val dotRadius = gap * scale * 0.22f
        val holdRadius = gap * scale * 0.58f
        strokePaint.strokeWidth = holdRadius * 0.18f
        dashPaint.strokeWidth = holdRadius * 0.16f

        for (hole in holes) {
            canvas.drawCircle(mapX(hole.x, pad, extraX, scale), mapY(hole.y, pad, extraY, scale), dotRadius, dotPaint)
        }
        for ((point, role) in holds) {
            val cx = mapX(point.x, pad, extraX, scale)
            val cy = mapY(point.y, pad, extraY, scale)
            when (role) {
                "start" -> canvas.drawCircle(cx, cy, holdRadius, strokePaint)
                "hand" -> {
                    canvas.drawCircle(cx, cy, holdRadius, strokePaint)
                    canvas.drawCircle(cx, cy, holdRadius * 0.45f, strokePaint)
                }
                "foot" -> drawDashedCircle(canvas, cx, cy, holdRadius)
                "finish" -> canvas.drawCircle(cx, cy, holdRadius, fillPaint)
            }
        }
    }

    private fun drawDashedCircle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        var start = 0f
        while (start < 360f) {
            canvas.drawArc(oval, start, 18f, false, dashPaint)
            start += 32f
        }
    }

    private fun mapX(x: Int, pad: Float, extra: Float, scale: Float): Float =
        pad + extra + (x - minX) * scale

    private fun mapY(y: Int, pad: Float, extra: Float, scale: Float): Float =
        height - pad - extra - (y - minY) * scale

    private fun typicalGap(): Float {
        val xs = holes.map { it.x }.distinct().sorted()
        var gap = Int.MAX_VALUE
        for (index in 1 until xs.size) {
            val distance = xs[index] - xs[index - 1]
            if (distance in 1 until gap) gap = distance
        }
        return if (gap == Int.MAX_VALUE) 8f else gap.toFloat()
    }
}
