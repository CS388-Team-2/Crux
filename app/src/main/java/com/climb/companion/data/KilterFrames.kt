package com.climb.companion.data

data class FrameHold(
    val placementId: Int,
    val roleId: Int,
)

data class BoardPoint(
    val x: Int,
    val y: Int,
)

data class BoardLayout(
    val holes: List<BoardPoint>,
    val placements: Map<Int, BoardPoint>,
    val roles: Map<Int, String>,
)

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
