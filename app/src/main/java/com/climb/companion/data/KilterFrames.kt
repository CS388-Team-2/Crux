package com.climb.companion.data

data class FrameHold(
    val placementId: Int,
    val roleId: Int,
)

data class BoardPoint(
    val x: Int,
    val y: Int,
)

data class BoardPlacement(
    val point: BoardPoint,
    val setId: Int,
)

data class BoardLayout(
    val placements: Map<Int, BoardPlacement>,
    val roles: Map<Int, String>,
) {
    fun grid(setId: Int): List<BoardPoint> {
        return placements.values
            .asSequence()
            .filter { it.setId == setId && it.point.x in GRID_LEFT..GRID_RIGHT && it.point.y in GRID_BOTTOM..GRID_TOP }
            .map { it.point }
            .distinctBy { it.x to it.y }
            .toList()
    }

    companion object {
        const val GRID_LEFT = 0
        const val GRID_RIGHT = 144
        const val GRID_BOTTOM = 0
        const val GRID_TOP = 156
        const val BOLT_SET = 1
    }
}

private val FRAME_PATTERN = Regex("p(\\d+)r(\\d+)")

fun parseKilterFrames(frames: String): List<FrameHold> =
    FRAME_PATTERN.findAll(frames).map { match ->
        FrameHold(
            placementId = match.groupValues[1].toInt(),
            roleId = match.groupValues[2].toInt(),
        )
    }.toList()

fun nearestBoardHole(point: BoardPoint, grid: List<BoardPoint>, maxDistance: Int = 12): BoardPoint? {
    var best: BoardPoint? = null
    var bestDistance = Int.MAX_VALUE
    val limit = maxDistance * maxDistance
    for (hole in grid) {
        val dx = hole.x - point.x
        val dy = hole.y - point.y
        val distance = dx * dx + dy * dy
        if (distance < bestDistance) {
            bestDistance = distance
            best = hole
        }
    }
    return if (bestDistance <= limit) best else null
}
fun holdRole(roleId: Int, roleNames: Map<Int, String>): String? =
    when (roleNames[roleId]) {
        "start" -> "start"
        "middle" -> "hand"
        "foot" -> "foot"
        "finish" -> "finish"
        else -> null
    }
