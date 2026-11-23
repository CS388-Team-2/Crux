package com.climb.companion.ui.feed

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.KilterCatalog
import com.climb.companion.data.KilterClimb
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.Favorites
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RouteDetailActivity : AppCompatActivity() {

    private var climb: KilterClimb? = null
    private var saveInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.title_text).text = title
        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.save_button).setOnClickListener { toggleSave() }

        val uuid = intent.getStringExtra(EXTRA_UUID)
        val angle = intent.getIntExtra(EXTRA_ANGLE, Int.MIN_VALUE)
        if (uuid.isNullOrBlank() || angle == Int.MIN_VALUE) {
            findViewById<MaterialButton>(R.id.save_button).isEnabled = false
            return
        }

        lifecycleScope.launch {
            val appContext = applicationContext
            val loaded = try {
                withContext(Dispatchers.IO) {
                    val found = KilterCatalog.find(appContext, uuid, angle)
                    val board = found?.let { KilterCatalog.board(appContext) }
                    val userId = UserSession.userId(appContext)
                    val saved = if (found != null && userId > 0L) {
                        AppDatabase.getInstance(appContext).appDao()
                            .findFavorite(userId, found.uuid, found.angle) != null
                    } else {
                        false
                    }
                    Triple(found, board, saved)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@RouteDetailActivity, "Could not load climb", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val found = loaded.first
            val board = loaded.second
            if (isFinishing) return@launch
            if (found == null || board == null) {
                Toast.makeText(this@RouteDetailActivity, "Could not load climb", Toast.LENGTH_SHORT).show()
                findViewById<MaterialButton>(R.id.save_button).isEnabled = false
                return@launch
            }
            climb = found
            findViewById<TextView>(R.id.title_text).text = found.name
            findViewById<TextView>(R.id.grade_value).text = found.grade
            findViewById<TextView>(R.id.angle_value).text = "${found.angle}°"
            findViewById<TextView>(R.id.logged_value).text =
                NumberFormat.getIntegerInstance().format(found.loggedCount)
            findViewById<TextView>(R.id.setter_text).text =
                "${found.setter} · ${found.layoutName}"
            findViewById<BoardView>(R.id.board_view).show(board, found.frames)
            showSaved(loaded.third)
        }
    }

    private fun showSaved(saved: Boolean) {
        findViewById<MaterialButton>(R.id.save_button).text = if (saved) "Saved" else "Save"
    }

    private fun toggleSave() {
        val found = climb ?: return
        if (saveInFlight) return
        saveInFlight = true
        val appContext = applicationContext
        lifecycleScope.launch {
            try {
                val userId = UserSession.userId(appContext)
                if (userId <= 0L) {
                    Toast.makeText(this@RouteDetailActivity, "Sign in to save climbs", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val dao = AppDatabase.getInstance(appContext).appDao()
                val existing = withContext(Dispatchers.IO) {
                    dao.findFavorite(userId, found.uuid, found.angle)
                }
                if (existing == null) {
                    withContext(Dispatchers.IO) {
                        dao.insertFavorite(
                            Favorites(
                                boardID = found.uuid,
                                angle = found.angle,
                                uId = userId,
                            )
                        )
                    }
                    showSaved(true)
                } else {
                    withContext(Dispatchers.IO) {
                        dao.deleteFavorites(existing.favoriteId)
                    }
                    showSaved(false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@RouteDetailActivity, "Could not save climb", Toast.LENGTH_SHORT).show()
            } finally {
                saveInFlight = false
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "climb_name"
        const val EXTRA_UUID = "climb_uuid"
        const val EXTRA_ANGLE = "climb_angle"
    }
}
