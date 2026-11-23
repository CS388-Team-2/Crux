package com.climb.companion.ui.diary

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.climb.companion.R
import com.climb.companion.data.AppDao
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.ClimbLogs
import com.climb.companion.data.entity.DiaryDayLog
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryEntryActivity : AppCompatActivity() {

    private lateinit var dTitle: EditText
    private lateinit var dDate: TextView
    private lateinit var dBody: EditText
    private lateinit var dTime: TextView
    private lateinit var dAttempts: TextView
    private lateinit var dTops: TextView
    private lateinit var dDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_edit)

        val userId = UserSession.userId(this)
        if (userId < 0) {
            Toast.makeText(this, "Sign in to use the diary", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dTitle = findViewById(R.id.DiaryTitle)
        dDate = findViewById(R.id.DiaryDate)
        dBody = findViewById(R.id.DiaryBody)
        dTime = findViewById(R.id.TotalTimeValues)
        dAttempts = findViewById(R.id.AttemptValue)
        dTops = findViewById(R.id.TopsValue)
        dDistance = findViewById(R.id.DistanceValue)

        val saveButton = findViewById<Button>(R.id.BackButton)
        saveButton.text = "Save"

        val dao = AppDatabase.getInstance(this).appDao()
        val logId = intent.getLongExtra("logId", -1L)

        if (logId != -1L) {
            lifecycleScope.launch {
                val log = dao.getSingleLog(logId) ?: return@launch
                dTitle.setText(log.title.orEmpty(), TextView.BufferType.EDITABLE)
                dDate.text = getDate(log.date)
                dBody.setText(log.notes.orEmpty(), TextView.BufferType.EDITABLE)
                bindDayStats(dao.getDailyLogData(logId) ?: dao.getDailyStatsForDate(userId, log.date))
            }
            saveButton.setOnClickListener { saveExisting(dao, logId) }
        } else {
            val todayString = todayYmd()
            dDate.text = getDate(todayString)
            lifecycleScope.launch {
                bindDayStats(dao.getDailyStatsForDate(userId, todayString))
            }
            saveButton.setOnClickListener { saveNew(dao, userId, todayString) }
        }
    }

    private fun bindDayStats(dayData: DiaryDayLog?) {
        val timeSeconds = dayData?.totalTime ?: 0L
        dTime.text = formatClockDuration(timeSeconds)
        dAttempts.text = (dayData?.attemptCount ?: 0).toString()
        dTops.text = (dayData?.topCount ?: 0).toString()
        val distance = dayData?.totalDistance ?: 0.0
        dDistance.text = "$distance m"
    }

    private fun saveExisting(dao: AppDao, logId: Long) {
        var title = dTitle.text.toString().trim()
        if (title.isEmpty()) title = "Untitled"
        val notes = dBody.text.toString()
        lifecycleScope.launch {
            dao.updateLog(title, notes, logId)
            finish()
        }
    }

    private fun saveNew(dao: AppDao, userId: Long, date: String) {
        var title = dTitle.text.toString().trim()
        if (title.isEmpty()) title = "Untitled"
        val notes = dBody.text.toString()
        lifecycleScope.launch {
            val existing = dao.checkLogExistence(userId, date)
            if (existing != null) {
                dao.updateLog(title, notes, existing)
            } else {
                dao.insertLog(
                    ClimbLogs(
                        title = title,
                        date = date,
                        notes = notes,
                        uId = userId,
                    )
                )
            }
            finish()
        }
    }

    private fun todayYmd(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

    private fun getDate(input: String): String {
        val inputFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat: DateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val date = inputFormat.parse(input) ?: return input
        return outputFormat.format(date)
    }
}