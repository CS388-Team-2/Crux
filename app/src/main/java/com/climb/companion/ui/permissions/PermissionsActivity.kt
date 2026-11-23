package com.climb.companion.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.climb.companion.MainActivity
import com.climb.companion.R
import com.climb.companion.data.PermissionChoices
import com.climb.companion.data.UserSession

class PermissionsActivity : AppCompatActivity() {

    private val fromProfile by lazy { intent.getBooleanExtra(EXTRA_FROM_PROFILE, false) }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        PermissionChoices.saveAllowed(this)
        refreshStatus()
        if (!fromProfile) openMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_permissions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<Button>(R.id.continue_button).setOnClickListener {
            val missing = permissions().filter { !granted(it) }
            if (missing.isEmpty()) {
                PermissionChoices.saveAllowed(this)
                refreshStatus()
                if (!fromProfile) openMain()
            } else {
                requestPermissions.launch(missing.toTypedArray())
            }
        }
        val notNow = findViewById<Button>(R.id.not_now_button)
        if (fromProfile) {
            notNow.text = "Done"
            notNow.setOnClickListener { finish() }
            findViewById<Button>(R.id.settings_button).apply {
                visibility = android.view.View.VISIBLE
                setOnClickListener { openSettings() }
            }
        } else {
            notNow.setOnClickListener {
                PermissionChoices.saveSkipped(this)
                openMain()
            }
        }
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        findViewById<TextView>(R.id.location_status).text = status(
            granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        )
        findViewById<TextView>(R.id.camera_status).text = status(granted(Manifest.permission.CAMERA))
        findViewById<TextView>(R.id.motion_status).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            status(granted(Manifest.permission.ACTIVITY_RECOGNITION))
        } else {
            "Not needed on this phone"
        }
        findViewById<TextView>(R.id.notification_status).text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                status(granted(Manifest.permission.POST_NOTIFICATIONS))
            } else {
                "Not needed on this phone"
            }
    }

    private fun status(allowed: Boolean): String = if (allowed) "Allowed" else "Not allowed"

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun permissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        )
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

    companion object {
        const val EXTRA_FROM_PROFILE = "from_profile"
    }
}
