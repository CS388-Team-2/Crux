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
        const val GRID_BOTTOM = 12
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

fun holdRole(roleId: Int, roleNames: Map<Int, String>): String? =
    when (roleNames[roleId]) {
        "start" -> "start"
        "middle" -> "hand"
        "foot" -> "foot"
        "finish" -> "finish"
        else -> null
    }
