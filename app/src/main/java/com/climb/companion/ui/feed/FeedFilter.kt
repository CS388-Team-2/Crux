package com.climb.companion.ui.feed

import android.content.Context
import com.climb.companion.data.entity.CatalogClimb

data class FeedFilter(
    val minGrade: Int = 0,
    val maxGrade: Int = 9,
    val angle: Int? = 40,
    val popularity: String = MOST_CLIMBED,
    val hideSaved: Boolean = false,
) {
    fun matching(climbs: List<CatalogClimb>, savedIds: Set<String>): List<CatalogClimb> {
        val low = minOf(minGrade, maxGrade)
        val high = maxOf(minGrade, maxGrade)
        val filtered = climbs.filter { climb ->
            val grade = climb.grade.removePrefix("V").toIntOrNull() ?: return@filter false
            val gradeMatches = grade in low..high
            val angleMatches = angle == null || climb.angle == angle
            val savedMatches = !hideSaved || climb.uuid !in savedIds
            gradeMatches && angleMatches && savedMatches
        }
        val ordered = if (popularity == LEAST_CLIMBED) {
            filtered.sortedByDescending { it.position }
        } else {
            filtered.sortedBy { it.position }
        }
        return if (popularity == TOP_CLIMBED) ordered.take(TOP_LIMIT) else ordered
    }

    companion object {
        const val MOST_CLIMBED = "most"
        const val LEAST_CLIMBED = "least"
        const val TOP_CLIMBED = "top"
        const val NEWEST = "newest"
        private const val TOP_LIMIT = 50
    }
}

object FeedFilterStore {
    private const val PREFS = "feed_filter"
    private const val KEY_MIN = "min_grade"
    private const val KEY_MAX = "max_grade"
    private const val KEY_ANGLE = "angle"
    private const val KEY_POPULARITY = "popularity"
    private const val KEY_HIDE_SAVED = "hide_saved"
    private const val ANY_ANGLE = -1

    fun load(context: Context): FeedFilter {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val angle = prefs.getInt(KEY_ANGLE, 40)
        return FeedFilter(
            minGrade = prefs.getInt(KEY_MIN, 0),
            maxGrade = prefs.getInt(KEY_MAX, 9),
            angle = if (angle == ANY_ANGLE) null else angle,
            popularity = prefs.getString(KEY_POPULARITY, FeedFilter.MOST_CLIMBED)
                ?: FeedFilter.MOST_CLIMBED,
            hideSaved = prefs.getBoolean(KEY_HIDE_SAVED, false),
        )
    }

    fun save(context: Context, filter: FeedFilter) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MIN, filter.minGrade)
            .putInt(KEY_MAX, filter.maxGrade)
            .putInt(KEY_ANGLE, filter.angle ?: ANY_ANGLE)
            .putString(KEY_POPULARITY, filter.popularity)
            .putBoolean(KEY_HIDE_SAVED, filter.hideSaved)
            .apply()
    }
}
