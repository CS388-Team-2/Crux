package com.climb.companion.data

data class KilterClimb(
    val uuid: String,
    val name: String,
    val grade: String,
    val angle: Int,
    val setter: String,
    val layoutName: String,
    val loggedCount: Int,
    val quality: Double,
    val frames: String = "",
)
