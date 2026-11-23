package com.climb.companion.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentInput = findViewById<EditText>(R.id.current_password_input)
        val newInput = findViewById<EditText>(R.id.new_password_input)
        val saveButton = findViewById<Button>(R.id.save_password_button)
        val dao = AppDatabase.getInstance(this).appDao()

        saveButton.setOnClickListener {
            val currentPassword = currentInput.text.toString()
            val newPassword = newInput.text.toString()
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter your current password and a new password",
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }

            val userId = UserSession.userId(this)
            if (userId <= 0L) {
                Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            saveButton.isEnabled = false
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val login = dao.getLoginById(userId)
                    when {
                        login == null -> "missing"
                        login.Password != currentPassword -> "wrong"
                        else -> {
                            dao.updatePassword(userId, newPassword)
                            "ok"
                        }
                    }
                }
                saveButton.isEnabled = true
                when (result) {
                    "wrong" -> Toast.makeText(
                        this@ChangePasswordActivity,
                        "Current password is incorrect",
                        Toast.LENGTH_SHORT,
                    ).show()
                    "missing" -> Toast.makeText(
                        this@ChangePasswordActivity,
                        "Could not change password",
                        Toast.LENGTH_SHORT,
                    ).show()
                    else -> finish()
                }
            }
        }
    }
}
