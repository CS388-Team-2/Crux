package com.climb.companion.ui.diary

/** Session `sessionTime` is stored in seconds (see SessionFragment.endSession). */
internal fun sessionSecondsToHoursMinutes(totalSeconds: Long): Pair<Long, Long> {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return hours to minutes
}

internal fun formatClockDuration(totalSeconds: Long): String {
    val (hours, minutes) = sessionSecondsToHoursMinutes(totalSeconds)
    return "${hours}:${minutes.toString().padStart(2, '0')}"
}

internal fun formatShortDuration(totalSeconds: Long): String {
    val (hours, minutes) = sessionSecondsToHoursMinutes(totalSeconds)
    return "${hours}h ${minutes}m"
}