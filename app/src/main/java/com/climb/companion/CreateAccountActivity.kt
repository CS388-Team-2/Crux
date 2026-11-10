package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val createButton = findViewById<Button>(R.id.create_button)
        val dao = AppDatabase.getInstance(this).appDao()

        createButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createButton.isEnabled = false
            lifecycleScope.launch {
                val newUserId = withContext(Dispatchers.IO) {
                    if (dao.getLogin(username).isNotEmpty()) {
                        return@withContext 0L
                    }
                    dao.insertLogin(Login(Username = username, Password = password))
                    dao.getLogin(username).firstOrNull()?.uId ?: -1L
                }
                createButton.isEnabled = true
                when {
                    newUserId == 0L -> {
                        Toast.makeText(
                            this@CreateAccountActivity,
                            "That username is already taken",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    newUserId < 0L -> {
                        Toast.makeText(
                            this@CreateAccountActivity,
                            "Could not create account",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    else -> openPermissions(newUserId, username)
                }
            }
        }
    }

    private fun openPermissions(userId: Long, username: String) {
        UserSession.save(this, userId, username)
        val intent = Intent(this, PermissionsActivity::class.java)
        intent.putExtra("username", username)
        intent.putExtra("userId", userId)
        startActivity(intent)
        finish()
    }
}
