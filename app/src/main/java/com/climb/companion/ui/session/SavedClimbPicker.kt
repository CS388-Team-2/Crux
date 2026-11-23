package com.climb.companion.ui.session

import android.content.Context
import android.widget.Toast
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.KilterCatalog
import com.climb.companion.data.UserSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Lets the user pick one of their saved climbs to log attempts to.
// Used by the Session screen and by Edit Attempt.
object SavedClimbPicker {

    // Calls onPicked with the climb's label, like "Crimp Lord · V5"
    fun show(context: Context, scope: CoroutineScope, onPicked: (String) -> Unit) {
        val appContext = context.applicationContext
        scope.launch {
            if (UserSession.userId(appContext) <= 0L) {
                Toast.makeText(context, "Sign in to pick a saved climb", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val labels = loadLabels(appContext)
            if (labels.isEmpty()) {
                Toast.makeText(context, "Save a climb from the feed first", Toast.LENGTH_SHORT).show()
                return@launch
            }
            MaterialAlertDialogBuilder(context)
                .setTitle("Log to which saved climb?")
                .setItems(labels.toTypedArray()) { _, which -> onPicked(labels[which]) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // The user's saved climbs, newest first
    private suspend fun loadLabels(appContext: Context): List<String> {
        return withContext(Dispatchers.IO) {
            val userId = UserSession.userId(appContext)
            val favorites = AppDatabase.getInstance(appContext).appDao()
                .getFavorites(userId)
                .first()
                .sortedByDescending { it.favoriteId }
            favorites.mapNotNull { favorite ->
                val angle = favorite.angle
                val climb = if (angle != null && angle >= 0) {
                    KilterCatalog.find(appContext, favorite.boardID, angle)
                } else {
                    null
                }
                val name = climb?.name ?: favorite.climbName?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val grade = climb?.grade ?: favorite.grade?.takeIf { it.isNotBlank() } ?: "Ungraded"
                "$name · $grade"
            }
        }
    }
}
