package com.climb.companion.ui.session

import android.content.Intent
import com.climb.companion.R
import com.climb.companion.data.AppDao
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.entity.Attempts
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

    private lateinit var dao: AppDao
    private lateinit var attempt: Attempts
    private lateinit var attemptedButton: TextView
    private lateinit var toppedButton: TextView
    private lateinit var noteInput: EditText

    // Edits are kept here and only saved when the user taps Save.
    private var result = "Attempted"
    private var route: String? = null

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
        dao = AppDatabase.getInstance(this).appDao()
        noteInput = findViewById(R.id.note_input)

        val attemptId = intent.getLongExtra("attemptId", -1L)
        lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {
                dao.getAttemptById(attemptId).firstOrNull()
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
        route = attempt.route

        findViewById<TextView>(R.id.edit_title).text = "Attempt ${intent.getIntExtra("attemptNumber", 0)}"
        findViewById<TextView>(R.id.edit_subtitle).text = if (attempt.isAuto) {
            "${attempt.time} · max ${"%.1f".format(attempt.maxHeight)} m"
        } else {
            attempt.time
        }
        findViewById<TextView>(R.id.route_name).text = attempt.route ?: ""
        noteInput.setText(attempt.note)

        // The x y graph only shows when the phone's sensors caught the attempt
        val sensorGroup = findViewById<View>(R.id.sensor_group)
        sensorGroup.visibility = if (attempt.isAuto) View.VISIBLE else View.GONE
        findViewById<PathGraphView>(R.id.path_graph).setPath(attempt.path)

        showResult()

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
            SavedClimbPicker.show(this, lifecycleScope) { label ->
                route = label
                findViewById<TextView>(R.id.route_name).text = label
            }
        }
        findViewById<View>(R.id.video_gallery).setOnClickListener {
            Toast.makeText(this, "Gallery picker is coming soon", Toast.LENGTH_SHORT).show()
        }

        val openRecord = View.OnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }
        findViewById<View>(R.id.video_record).setOnClickListener(openRecord)

        findViewById<View>(R.id.edit_save).setOnClickListener {
            lifecycleScope.launch {
                val updated = attempt.copy(
                    result = result,
                    route = route,
                    note = noteInput.text.toString().trim(),
                )
                withContext(Dispatchers.IO) {
                    dao.updateAttempt(updated)
                }
                finish()
            }
        }
        findViewById<View>(R.id.edit_delete).setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    dao.deleteAttempt(attempt.attemptId)
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
}
