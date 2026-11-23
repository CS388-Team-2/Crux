package com.climb.companion.data

import android.content.Context

object PermissionChoices {
    private const val PREFS = "permission_choices"
    private const val KEY_CHOICE = "choice"
    private const val ALLOWED = "allowed"
    private const val SKIPPED = "skipped"

    fun hasChoice(context: Context): Boolean = choice(context) != null

    fun allowed(context: Context): Boolean = choice(context) == ALLOWED

    fun saveAllowed(context: Context) {
        save(context, ALLOWED)
    }

    fun saveSkipped(context: Context) {
        save(context, SKIPPED)
    }

    private fun choice(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CHOICE, null)

    private fun save(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHOICE, value)
            .apply()
    }
}
