package com.climb.companion

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sample data for the running session, so the Session and Edit Attempt screens work before
// Room is hooked up. The function names match the ones in AppDao (Dao.kt). The screens call
// them like SignInActivity calls the dao: inside lifecycleScope.launch and withContext(Dispatchers.IO).
// To switch to Room, replace each SessionData.something() call with dao.something().
object SessionData {
    // Sessions table: gym/route, startTime, distance
    const val GYM_NAME = "Randolf Climbing Center"
    const val STARTED_AT = "7:02 PM"
    const val ROUTE_NAME = "Crimp Lord · V5"
    const val ROUTE_DETAIL = "Kilter · 40° · from your Saved list"
    const val CLIMBED_METERS = 38

    // Pretend the session started 32 minutes and 10 seconds ago, like the Figma design.
    val startMillis = System.currentTimeMillis() - (32 * 60 + 10) * 1000L

    // Newest attempt first.
    private val attempts = mutableListOf(
        AttemptItem(7, "Topped", ROUTE_NAME, "", "7:41 PM", true, 3.9, 41),
        AttemptItem(6, "Attempted", ROUTE_NAME, "", "7:35 PM", true, 3.9, 0),
        AttemptItem(5, "Topped", ROUTE_NAME, "", "7:28 PM", false, 0.0, 0),
        AttemptItem(4, "Attempted", ROUTE_NAME, "", "7:22 PM", true, 2.8, 0),
        AttemptItem(3, "Attempted", ROUTE_NAME, "", "7:16 PM", false, 0.0, 0),
        AttemptItem(2, "Attempted", ROUTE_NAME, "", "7:10 PM", true, 2.1, 0),
        AttemptItem(1, "Attempted", ROUTE_NAME, "", "7:05 PM", false, 0.0, 0),
    )

    fun getAttempts(): List<AttemptItem> = attempts.toList()

    fun getAttempt(id: Long): AttemptItem? = attempts.firstOrNull { it.attemptId == id }

    // Logs an attempt by hand (the Attempted and Topped buttons).
    fun insertAttempt(result: String) {
        val newId = (attempts.maxOfOrNull { it.attemptId } ?: 0L) + 1
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        attempts.add(0, AttemptItem(newId, result, ROUTE_NAME, "", time, false, 0.0, 0))
    }

    fun updateAttempt(attempt: AttemptItem) {
        val index = attempts.indexOfFirst { it.attemptId == attempt.attemptId }
        if (index >= 0) attempts[index] = attempt
    }

    fun deleteAttempt(id: Long) {
        attempts.removeAll { it.attemptId == id }
    }
}
