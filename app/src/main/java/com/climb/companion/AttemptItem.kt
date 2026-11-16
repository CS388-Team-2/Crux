package com.climb.companion

// One attempt in the running session. The first four fields match the Attempts table in
// Entities.kt. When Room is added, the other four need new columns on Attempts.
data class AttemptItem(
    val attemptId: Long,
    val result: String, // "Attempted" or "Topped"
    val route: String,
    val note: String,
    val time: String,
    val isAuto: Boolean, // true = caught by the sensors, false = logged by hand
    val maxHeight: Double, // meters
    val clipSeconds: Int, // 0 = no video
)

// 41 seconds -> "0:41"
fun formatClip(seconds: Int): String = "${seconds / 60}:${"%02d".format(seconds % 60)}"
