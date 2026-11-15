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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionFragment : Fragment() {

    private lateinit var timerText: TextView
    private lateinit var attemptsTile: TextView
    private lateinit var topsTile: TextView
    private lateinit var attemptsList: RecyclerView

    // Updates the timer once a second while the screen is showing
    private val handler = Handler(Looper.getMainLooper())
    private val timerTick = object : Runnable {
        override fun run() {
            val seconds = (System.currentTimeMillis() - SessionData.startMillis) / 1000
            timerText.text = String.format(
                "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60
            )
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

        timerText = view.findViewById(R.id.timer_text)
        attemptsTile = view.findViewById(R.id.tile_attempts)
        topsTile = view.findViewById(R.id.tile_tops)
        attemptsList = view.findViewById(R.id.attempts_list)
        attemptsList.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<TextView>(R.id.timer_subtitle).text =
            "${SessionData.GYM_NAME} · started ${SessionData.STARTED_AT}"
        view.findViewById<TextView>(R.id.tile_climbed).text = "${SessionData.CLIMBED_METERS} m"
        view.findViewById<TextView>(R.id.route_name).text = SessionData.ROUTE_NAME

        // The Saved route picker and Diary Entry screens are not built yet.
        view.findViewById<View>(R.id.change_route).setOnClickListener {
            Toast.makeText(requireContext(), "Route picker is coming soon", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.end_session).setOnClickListener {
            Toast.makeText(requireContext(), "Diary entry is coming soon", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.log_attempted).setOnClickListener { logAttempt("Attempted") }
        view.findViewById<View>(R.id.log_topped).setOnClickListener { logAttempt("Topped") }
    }

    // Reload when coming back from Edit Attempt so changes show up.
    override fun onResume() {
        super.onResume()
        handler.post(timerTick)
        loadAttempts()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerTick)
    }

    private fun logAttempt(result: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                SessionData.insertAttempt(result)
            }
            loadAttempts()
        }
    }

    private fun loadAttempts() {
        lifecycleScope.launch {
            val attempts = withContext(Dispatchers.IO) {
                SessionData.getAttempts()
            }
            attemptsTile.text = attempts.size.toString()
            topsTile.text = attempts.count { it.result == "Topped" }.toString()
            attemptsList.adapter = AttemptAdapter(attempts) { attempt ->
                val intent = Intent(requireContext(), EditAttemptActivity::class.java)
                intent.putExtra("attemptId", attempt.attemptId)
                startActivity(intent)
            }
        }
    }
}
