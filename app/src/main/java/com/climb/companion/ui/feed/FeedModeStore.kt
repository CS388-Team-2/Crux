package com.climb.companion.ui.feed

import android.content.Context

object FeedModeStore {
    private const val PREFS = "feed_mode"
    private const val KEY_OUTDOOR = "outdoor"

    fun isOutdoor(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_OUTDOOR, false)

    fun setOutdoor(context: Context, outdoor: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OUTDOOR, outdoor)
            .apply()
    }
}
