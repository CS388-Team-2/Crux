package com.climb.companion

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class DiaryEntryActivity : PlaceholderActivity() {
    override val screenTitle = "Diary entry"
    private lateinit var dTitle: EditText
    private lateinit var dDate: TextView
    private lateinit var dBody: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.food_input) //old name need to update

        dTitle = findViewById(R.id.DiaryTitle)
        dDate = findViewById(R.id.DiaryDate)
        dBody = findViewById(R.id.DiaryBody)

        val dBack = findViewById<Button>(R.id.BackButton)

        dBack.setOnClickListener{
            //write entry into db
            finish()
        }



    }
}
