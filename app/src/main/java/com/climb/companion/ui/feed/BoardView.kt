package com.climb.companion.ui.feed

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
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

    init {
        // Dashed circles are not drawn by the hardware renderer on older phones.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun show(layout: BoardLayout, frames: String) {
        holes.clear()
        holes.addAll(layout.holes)
        holds.clear()
        for (mark in parseKilterFrames(frames)) {
            val point = layout.placements[mark.placementId] ?: continue
            val role = holdRole(mark.roleId, layout.roles) ?: continue
            holds += point to role
        }
        val xs = holes.map { it.x }
        val ys = holes.map { it.y }
        minX = xs.minOrNull() ?: 0
        maxX = xs.maxOrNull() ?: 1
        minY = ys.minOrNull() ?: 0
        maxY = ys.maxOrNull() ?: 1
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
        val dotRadius = (gap * scale * 0.16f).coerceAtLeast(2f)
        val holdRadius = (gap * scale * 0.42f).coerceAtLeast(dotRadius * 2.2f)
        strokePaint.strokeWidth = holdRadius * 0.16f
        dashPaint.strokeWidth = holdRadius * 0.14f
        dashPaint.pathEffect = DashPathEffect(
            floatArrayOf(holdRadius * 0.55f, holdRadius * 0.4f),
            0f,
        )

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
                "foot" -> canvas.drawCircle(cx, cy, holdRadius, dashPaint)
                "finish" -> canvas.drawCircle(cx, cy, holdRadius, fillPaint)
            }
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
