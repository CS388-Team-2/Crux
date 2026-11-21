package com.climb.companion.ui.feed

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.CatalogDatabase
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.CatalogClimb
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.RangeSlider
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterActivity : AppCompatActivity() {

    private lateinit var gradeLabel: TextView
    private lateinit var gradeSlider: RangeSlider
    private lateinit var angleToggle: MaterialButtonToggleGroup
    private lateinit var popularityGroup: RadioGroup
    private lateinit var hideSavedSwitch: SwitchMaterial
    private lateinit var showButton: Button

    private var climbs: List<CatalogClimb> = emptyList()
    private var savedIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_filter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gradeLabel = findViewById(R.id.grade_range_label)
        gradeSlider = findViewById(R.id.grade_slider)
        angleToggle = findViewById(R.id.angle_toggle)
        popularityGroup = findViewById(R.id.popularity_group)
        hideSavedSwitch = findViewById(R.id.hide_saved_switch)
        showButton = findViewById(R.id.show_climbs_button)

        val current = FeedFilterStore.load(this)
        showCurrent(current)

        gradeSlider.setLabelFormatter { value -> "V${value.toInt()}" }
        gradeSlider.addOnChangeListener { _, _, _ -> refreshCount() }
        angleToggle.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) refreshCount()
        }
        popularityGroup.setOnCheckedChangeListener { _, _ -> refreshCount() }
        hideSavedSwitch.setOnCheckedChangeListener { _, _ -> refreshCount() }

        findViewById<Button>(R.id.close_button).setOnClickListener { finish() }
        showButton.setOnClickListener {
            FeedFilterStore.save(this, readFilter())
            setResult(RESULT_OK)
            finish()
        }

        lifecycleScope.launch {
            val appContext = applicationContext
            CatalogDatabase.ensureLoaded(appContext)
            val loaded = withContext(Dispatchers.IO) {
                CatalogDatabase.getInstance(appContext).catalogDao().getAll()
            }
            val userId = UserSession.userId(appContext)
            val saved = if (userId > 0L) {
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(appContext).appDao()
                        .getFavorites(userId)
                        .first()
                        .map { it.boardID }
                        .toSet()
                }
            } else {
                emptySet()
            }
            climbs = loaded
            savedIds = saved
            refreshCount()
        }
    }

    private fun showCurrent(filter: FeedFilter) {
        val low = minOf(filter.minGrade, filter.maxGrade).coerceIn(0, 9)
        val high = maxOf(filter.minGrade, filter.maxGrade).coerceIn(0, 9)
        gradeSlider.values = listOf(low.toFloat(), high.toFloat())
        gradeLabel.text = gradeLabel(low, high)
        angleToggle.check(
            when (filter.angle) {
                20 -> R.id.angle_20
                30 -> R.id.angle_30
                50 -> R.id.angle_50
                null -> R.id.angle_any
                else -> R.id.angle_40
            }
        )
        popularityGroup.check(
            when (filter.popularity) {
                FeedFilter.LEAST_CLIMBED -> R.id.popularity_least
                FeedFilter.TOP_CLIMBED -> R.id.popularity_top
                else -> R.id.popularity_most
            }
        )
        hideSavedSwitch.isChecked = filter.hideSaved
    }

    private fun readFilter(): FeedFilter {
        val values = gradeSlider.values
        val low = values.getOrNull(0)?.toInt() ?: 0
        val high = values.getOrNull(1)?.toInt() ?: 9
        return FeedFilter(
            minGrade = minOf(low, high),
            maxGrade = maxOf(low, high),
            angle = when (angleToggle.checkedButtonId) {
                R.id.angle_20 -> 20
                R.id.angle_30 -> 30
                R.id.angle_50 -> 50
                R.id.angle_any -> null
                else -> 40
            },
            popularity = when (popularityGroup.checkedRadioButtonId) {
                R.id.popularity_least -> FeedFilter.LEAST_CLIMBED
                R.id.popularity_top -> FeedFilter.TOP_CLIMBED
                else -> FeedFilter.MOST_CLIMBED
            },
            hideSaved = hideSavedSwitch.isChecked,
        )
    }

    private fun refreshCount() {
        val filter = readFilter()
        val values = gradeSlider.values
        gradeLabel.text = gradeLabel(
            values.getOrNull(0)?.toInt() ?: filter.minGrade,
            values.getOrNull(1)?.toInt() ?: filter.maxGrade,
        )
        if (climbs.isEmpty()) {
            showButton.text = "Show climbs"
            return
        }
        val count = filter.matching(climbs, savedIds).size
        showButton.text = when (count) {
            0 -> "No climbs match"
            1 -> "Show 1 climb"
            else -> "Show $count climbs"
        }
    }

    private fun gradeLabel(low: Int, high: Int): String {
        val start = minOf(low, high)
        val end = maxOf(low, high)
        return "V$start – V$end"
    }
}
