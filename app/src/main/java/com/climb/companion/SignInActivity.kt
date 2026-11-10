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

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val signInButton = findViewById<Button>(R.id.sign_in_button)
        val dao = AppDatabase.getInstance(this).appDao()

        signInButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signInButton.isEnabled = false
            lifecycleScope.launch {
                val matches = withContext(Dispatchers.IO) {
                    dao.getLogin(username).firstOrNull { it.Password == password }
                }
                signInButton.isEnabled = true
                if (matches == null) {
                    Toast.makeText(this@SignInActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                } else {
                    openPermissions(matches.id, matches.Username)
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
