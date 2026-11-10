package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PermissionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        findViewById<Button>(R.id.continue_button).setOnClickListener {
            openMain()
        }
        findViewById<Button>(R.id.not_now_button).setOnClickListener {
            openMain()
        }
    }

    private fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", getIntent().getStringExtra("username"))
        val userId = getIntent().getLongExtra("userId", UserSession.userId(this))
        if (userId >= 0) {
            intent.putExtra("userId", userId)
        }
        startActivity(intent)
        finish()
    }
}
