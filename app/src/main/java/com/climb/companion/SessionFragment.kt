package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionFragment : Fragment() {

    // The gym and route are placeholders for now
    private val gymName = "Randolf Climbing Center"
    private val routeName = "Crimp Lord · V5"

    private lateinit var dao: AppDao
    private lateinit var tracker: AttemptTracker
    private var userId = -1L

    // The running session (null when none) and its attempts, newest first
    private var session: Sessions? = null
    private var attempts: List<Attempts> = emptyList()
    private var attemptsJob: Job? = null

    private lateinit var startGroup: View
    private lateinit var sessionHeader: View
    private lateinit var endButton: View
    private lateinit var timerText: TextView
    private lateinit var timerSubtitle: TextView
    private lateinit var attemptsTile: TextView
    private lateinit var topsTile: TextView
    private lateinit var climbedTile: TextView
    private lateinit var routeText: TextView
    private lateinit var attemptsList: RecyclerView

    // Updates the timer once a second while the screen is showing
    private val handler = Handler(Looper.getMainLooper())
    private val timerTick = object : Runnable {
        override fun run() {
            val current = session
            if (current != null) {
                val seconds = (System.currentTimeMillis() - current.startMillis) / 1000
                timerText.text = String.format(
                    "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60
                )
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_session, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dao = AppDatabase.getInstance(requireContext()).appDao()
        userId = UserSession.userId(requireContext())
        tracker = AttemptTracker(requireContext()) { height, seconds, path -> saveAutoAttempt(height, seconds, path) }

        startGroup = view.findViewById(R.id.start_group)
        sessionHeader = view.findViewById(R.id.session_header)
        endButton = view.findViewById(R.id.end_session)
        timerText = view.findViewById(R.id.timer_text)
        timerSubtitle = view.findViewById(R.id.timer_subtitle)
        attemptsTile = view.findViewById(R.id.tile_attempts)
        topsTile = view.findViewById(R.id.tile_tops)
        climbedTile = view.findViewById(R.id.tile_climbed)
        routeText = view.findViewById(R.id.route_name)
        attemptsList = view.findViewById(R.id.attempts_list)
        attemptsList.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.start_session).setOnClickListener { startSession() }
        view.findViewById<View>(R.id.end_session).setOnClickListener { endSession() }
        view.findViewById<View>(R.id.log_attempted).setOnClickListener { logAttempt("Attempted") }
        view.findViewById<View>(R.id.log_topped).setOnClickListener { logAttempt("Topped") }

        // route picker is not built yet
        view.findViewById<View>(R.id.change_route).setOnClickListener {
            Toast.makeText(requireContext(), "Route picker is coming soon", Toast.LENGTH_SHORT).show()
        }

        showSession()

        viewLifecycleOwner.lifecycleScope.launch {
            dao.getRunningSession(userId).collect { running ->
                session = running.firstOrNull()
                watchAttempts()
                showSession()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(timerTick)
        updateTracking()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerTick)
        updateTracking()
    }

    // The sensors only run while a session is running and this screen is showing
    // The screen stays on so Android keeps sending sensor readings
    private fun updateTracking() {
        val tracking = session != null && isResumed
        if (tracking) tracker.start() else tracker.stop()
        view?.keepScreenOn = tracking
    }

    // Follows the attempts of the running session so the list and tiles stay up to date
    private fun watchAttempts() {
        attemptsJob?.cancel()
        val current = session
        if (current == null) {
            attempts = emptyList()
            return
        }
        attemptsJob = viewLifecycleOwner.lifecycleScope.launch {
            dao.getAttempt(current.sessionId).collect { list ->
                attempts = list
                showAttempts()
            }
        }
    }

    // Shows the session, or the Start session button when none is running
    private fun showSession() {
        val current = session
        if (current == null) {
            startGroup.visibility = View.VISIBLE
            sessionHeader.visibility = View.GONE
            attemptsList.visibility = View.GONE
            endButton.visibility = View.GONE
            updateTracking()
            return
        }
        startGroup.visibility = View.GONE
        sessionHeader.visibility = View.VISIBLE
        attemptsList.visibility = View.VISIBLE
        endButton.visibility = View.VISIBLE
        timerSubtitle.text = "$gymName · started ${current.startTime}"
        routeText.text = current.route
        showAttempts()
        updateTracking()
    }

    private fun showAttempts() {
        attemptsTile.text = attempts.size.toString()
        topsTile.text = attempts.count { it.result == "Topped" }.toString()
        climbedTile.text = "${"%.1f".format(attempts.sumOf { it.maxHeight })} m"
        attemptsList.adapter = AttemptAdapter(attempts) { attempt, number ->
            val intent = Intent(requireContext(), EditAttemptActivity::class.java)
            intent.putExtra("attemptId", attempt.attemptId)
            intent.putExtra("attemptNumber", number)
            startActivity(intent)
        }
    }

    private fun startSession() {
        if (userId < 0) {
            Toast.makeText(requireContext(), "Sign in to start a session", Toast.LENGTH_SHORT).show()
            return
        }
        val now = Date()
        val newSession = Sessions(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
            startTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
            route = routeName,
            distance = 0.0,
            sessionTime = null,
            startMillis = now.time,
            uId = userId,
        )
        if (!tracker.hasBarometer) {
            Toast.makeText(requireContext(), "No barometer, so log attempts by hand", Toast.LENGTH_LONG).show()
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dao.insertSession(newSession)
            }
        }
    }

    private fun endSession() {
        val current = session ?: return
        val now = Date()
        val ended = current.copy(
            endTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
            sessionTime = (now.time - current.startMillis) / 1000,
            distance = attempts.sumOf { it.maxHeight },
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateSession(ended)
            }
            // The Diary Entry screen is not built yet.
            Toast.makeText(requireContext(), "Diary entry is coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    // Logs an attempt by hand (the Attempted and Topped buttons)
    private fun logAttempt(result: String) {
        val current = session ?: return
        insertAttempt(
            Attempts(
                result = result,
                route = current.route,
                note = "",
                sessionId = current.sessionId,
                time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            )
        )
    }

    // Called by the tracker when the barometer finds a finished attempt
    private fun saveAutoAttempt(maxHeight: Double, seconds: Int, path: String) {
        val current = session ?: return
        insertAttempt(
            Attempts(
                result = "Attempted",
                route = current.route,
                note = "",
                sessionId = current.sessionId,
                time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                isAuto = true,
                maxHeight = maxHeight,
                durationSeconds = seconds,
                path = path,
            )
        )
    }

    private fun insertAttempt(attempt: Attempts) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dao.insertAttempt(attempt)
            }
        }
    }
}
