package com.climb.companion.ui.feed

data class FeedClimb(
    val uuid: String,
    val name: String,
    val grade: String,
    val angle: String,
    val areaName: String = "",
    val photoUrl: String = "",
    val angleDegrees: Int = 40,
    val frames: String = "",
    val outdoor: Boolean = false,
)
