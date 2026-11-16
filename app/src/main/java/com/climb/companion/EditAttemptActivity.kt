package com.climb.companion

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditAttemptActivity : AppCompatActivity() {

    private lateinit var attempt: AttemptItem
    private lateinit var attemptedButton: TextView
    private lateinit var toppedButton: TextView
    private lateinit var videoRow: View
    private lateinit var noteInput: EditText

    // Edits are kept here and only saved when the user taps Save.
    private var result = "Attempted"
    private var clipSeconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_attempt)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        attemptedButton = findViewById(R.id.result_attempted)
        toppedButton = findViewById(R.id.result_topped)
        videoRow = findViewById(R.id.video_attached)
        noteInput = findViewById(R.id.note_input)

        val attemptId = intent.getLongExtra("attemptId", -1L)
        lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {
                SessionData.getAttempt(attemptId)
            }
            if (found == null) {
                finish()
                return@launch
            }
            attempt = found
            showAttempt()
        }
    }

    private fun showAttempt() {
        result = attempt.result
        clipSeconds = attempt.clipSeconds

        findViewById<TextView>(R.id.edit_title).text = "Attempt ${attempt.attemptId}"
        findViewById<TextView>(R.id.edit_subtitle).text = if (attempt.isAuto) {
            "${attempt.time} · max ${attempt.maxHeight} m"
        } else {
            attempt.time
        }
        findViewById<TextView>(R.id.route_name).text = attempt.route
        findViewById<TextView>(R.id.route_detail).text = SessionData.ROUTE_DETAIL
        noteInput.setText(attempt.note)

        // The graph and board only make show when the phone's sensors caught the attempt
        val sensorGroup = findViewById<View>(R.id.sensor_group)
        sensorGroup.visibility = if (attempt.isAuto) View.VISIBLE else View.GONE

        showResult()
        showVideo()

        findViewById<View>(R.id.edit_close).setOnClickListener { finish() }

        attemptedButton.setOnClickListener {
            result = "Attempted"
            showResult()
        }
        toppedButton.setOnClickListener {
            result = "Topped"
            showResult()
        }

        findViewById<View>(R.id.change_route).setOnClickListener {
            Toast.makeText(this, "Route picker is coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.video_gallery).setOnClickListener {
            Toast.makeText(this, "Gallery picker is coming soon", Toast.LENGTH_SHORT).show()
        }

        val openRecord = View.OnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }
        findViewById<View>(R.id.video_record).setOnClickListener(openRecord)
        findViewById<View>(R.id.video_replace).setOnClickListener(openRecord)
        findViewById<View>(R.id.video_remove).setOnClickListener {
            clipSeconds = 0
            showVideo()
        }

        findViewById<View>(R.id.edit_save).setOnClickListener {
            lifecycleScope.launch {
                val updated = attempt.copy(
                    result = result,
                    note = noteInput.text.toString().trim(),
                    clipSeconds = clipSeconds,
                )
                withContext(Dispatchers.IO) {
                    SessionData.updateAttempt(updated)
                }
                finish()
            }
        }
        findViewById<View>(R.id.edit_delete).setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    SessionData.deleteAttempt(attempt.attemptId)
                }
                finish()
            }
        }
    }

    // Highlights whichever result is selected
    private fun showResult() {
        val selected = if (result == "Topped") toppedButton else attemptedButton
        val other = if (result == "Topped") attemptedButton else toppedButton

        selected.setBackgroundResource(R.drawable.bg_dark)
        selected.setTextColor(ContextCompat.getColor(this, R.color.crux_white))
        selected.setTypeface(null, Typeface.BOLD)
        other.setBackgroundResource(0)
        other.setTextColor(ContextCompat.getColor(this, R.color.crux_muted))
        other.setTypeface(null, Typeface.NORMAL)
    }

    // Shows the attached video, or hides that row when there is none
    private fun showVideo() {
        if (clipSeconds > 0) {
            videoRow.visibility = View.VISIBLE
            findViewById<TextView>(R.id.video_duration).text = formatClip(clipSeconds)
        } else {
            videoRow.visibility = View.GONE
        }
    }
}
