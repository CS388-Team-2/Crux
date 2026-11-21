package com.climb.companion.ui.diary

import com.climb.companion.ui.common.PlaceholderActivity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.ClimbLogs
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import com.climb.companion.R
import com.climb.companion.data.entity.DiaryDayLog

class DiaryEntryActivity : PlaceholderActivity() {
    override val screenTitle = "Diary entry"
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

        dTitle = findViewById(R.id.DiaryTitle)
        dDate = findViewById(R.id.DiaryDate)
        dBody = findViewById(R.id.DiaryBody)
        dTime = findViewById(R.id.TotalTimeValues)
        dAttempts = findViewById(R.id.AttemptValue)
        dTops = findViewById(R.id.TopsValue)
        dDistance = findViewById(R.id.DistanceValue)

        val dBack = findViewById<Button>(R.id.BackButton)
        val dao = AppDatabase.getInstance(this).appDao()
        val logId = intent.getLongExtra("logId", -1L)
        if (logId != -1L) {
            lifecycleScope.launch {
                //fix to get DiaryItem dataclass
                val log = dao.getSingleLog(logId)
                if (log != null) {
                    val formattedString = getDate(log.date)
                    dTitle.setText(log.title, TextView.BufferType.EDITABLE)
                    dDate.text = formattedString
                    dBody.setText(log.notes, TextView.BufferType.EDITABLE)
                    val dayData: DiaryDayLog? = dao.getDailyLogData(logId)
                    var hours = 0L
                    var minutes = 0L
                    var attempts = 0
                    var tops = 0
                    var distance = "0 m"
                    if (dayData != null){
                        hours = dayData.totalTime/3600000
                        minutes = (dayData.totalTime%3600000)/60000
                        attempts = dayData.attemptCount
                        tops = dayData.topCount
                        distance = "${dayData.totalDistance} m"
                    }

                    val minutesString = minutes.toString().padStart(2, '0');
                    dTime.text = "${hours}:${minutesString}"
                    dAttempts.text = attempts.toString()
                    dTops.text = tops.toString()
                    dDistance.text = distance
                }

            }
            dBack.setOnClickListener{
                var title = dTitle.text.toString()
                val notes = dBody.text.toString()
                if (title.isEmpty()) {title="Untitled"}
                    lifecycleScope.launch {
                        dao.updateLog(title, notes, logId)
                        finish()
                    }

            }
        }
        else{
            val todayDate: Date? = Calendar.getInstance().getTime()
            val formatter = SimpleDateFormat("yyyy-MM-dd")
            val todayString = formatter.format(todayDate)
            val displayDate = getDate(todayString)

            dDate.text = displayDate
            dAttempts.text = "0"
            dTime.text = "0:00"
            dDistance.text = "0 m"
            dTops.text = "0"
            dBack.setOnClickListener {
                var title = dTitle.text.toString()
                val date = todayString
                val notes = dBody.text.toString()
                val uId = userId
                if (!title.isEmpty()) {
                    lifecycleScope.launch {
                        dao.insertLog(
                            ClimbLogs(
                                title = title,
                                date = date,
                                notes = notes,
                                uId = uId
                            )
                        )
                        finish()
                    }
                }else{finish()}
            }
        }
    }

    fun getDate(input: String): String{
        val inputFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val outputFormat: DateFormat = SimpleDateFormat("EEE, MMM dd")
        val startDateStr = input
        val date = inputFormat.parse(startDateStr)
        val startDateStrNewFormat: String = outputFormat.format(date)
        return startDateStrNewFormat
    }

}
