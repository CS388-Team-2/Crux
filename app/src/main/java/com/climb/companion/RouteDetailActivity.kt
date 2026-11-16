package com.climb.companion

import android.os.Bundle
import android.widget.TextView

class RouteDetailActivity : PlaceholderActivity() {
    override val screenTitle = "Route detail"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE)
        if (!title.isNullOrBlank()) {
            findViewById<TextView>(R.id.title_text).text = title
        }
    }

    companion object {
        const val EXTRA_TITLE = "climb_name"
    }
}
