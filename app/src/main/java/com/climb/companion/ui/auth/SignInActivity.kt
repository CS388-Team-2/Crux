package com.climb.companion.ui.auth

import android.content.Intent
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.PermissionChoices
import com.climb.companion.data.UserSession
import com.climb.companion.MainActivity
import com.climb.companion.ui.permissions.PermissionsActivity
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
                    UserSession.save(this@SignInActivity, matches.uId, matches.Username)
                    openNext(matches.uId, matches.Username)
                }
            }
        }
    }

    private fun openNext(userId: Long, username: String) {
        val intent = if (PermissionChoices.hasChoice(this)) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PermissionsActivity::class.java)
        }
        intent.putExtra("username", username)
        intent.putExtra("userId", userId)
        startActivity(intent)
        finish()
    }
}
