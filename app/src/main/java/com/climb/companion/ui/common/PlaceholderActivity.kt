package com.climb.companion.ui.common

import android.os.Bundle
import com.climb.companion.R
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class PlaceholderActivity : AppCompatActivity() {

    protected open val screenTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        findViewById<TextView>(R.id.title_text).text = screenTitle
    }
}
