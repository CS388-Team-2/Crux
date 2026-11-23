package com.climb.companion.ui.feed

import android.content.Intent
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.KilterCatalog
import com.climb.companion.data.KilterClimb
import com.climb.companion.data.KilterSort
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.Favorites
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButtonToggleGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private val savesInFlight = mutableSetOf<String>()
    private val savedIds = mutableSetOf<String>()
    private val outdoorCards = mutableListOf<FeedClimb>()
    private val indoorCards = mutableListOf<FeedClimb>()
    private var indoorOffset = 0
    private var indoorHasMore = true
    private var indoorLoading = false
    private var indoorFilterKey = ""
    private var indoorBoard: com.climb.companion.data.BoardLayout? = null
    private var feedAdapter: FeedClimbAdapter? = null
    private var feedList: RecyclerView? = null
    private var feedEmpty: TextView? = null
    private var modeToggle: MaterialButtonToggleGroup? = null
    private var outdoorMode = false
    private var outdoorLoading = false
    private var loadGeneration = 0
    private var outdoorSession: OutdoorFeedSession? = null

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && !outdoorMode) {
            showIndoor()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.filter_button).setOnClickListener {
            filterLauncher.launch(Intent(requireContext(), FilterActivity::class.java))
        }

        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.feed_mode_toggle)
        modeToggle = toggle
        val startOutdoor = FeedModeStore.isOutdoor(requireContext())
        toggle.check(if (startOutdoor) R.id.outdoor_button else R.id.indoor_button)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || checkedId == View.NO_ID) return@addOnButtonCheckedListener
            val nextOutdoor = checkedId == R.id.outdoor_button
            if (nextOutdoor == outdoorMode) return@addOnButtonCheckedListener
            FeedModeStore.setOutdoor(requireContext(), nextOutdoor)
            showMode(nextOutdoor)
            if (nextOutdoor) showOutdoor() else showIndoor()
        }

        feedEmpty = view.findViewById(R.id.feed_empty)
        val list = view.findViewById<RecyclerView>(R.id.feed_list)
        feedList = list
        list.layoutManager = LinearLayoutManager(requireContext())
        PagerSnapHelper().attachToRecyclerView(list)
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (outdoorMode) maybeLoadMoreOutdoor() else maybeLoadMoreIndoor()
            }
        })

        showMode(startOutdoor)
        if (outdoorMode) showOutdoor() else showIndoor()
    }

    private fun showMode(outdoor: Boolean) {
        outdoorMode = outdoor
        val toggle = modeToggle ?: return
        val checkedId = if (outdoor) R.id.outdoor_button else R.id.indoor_button
        if (toggle.checkedButtonId != checkedId) {
            toggle.check(checkedId)
        }
    }

    private fun showOutdoor() {
        feedEmpty?.visibility = View.GONE
        if (outdoorCards.isNotEmpty()) {
            installOutdoorAdapter()
            feedList?.post { maybeLoadMoreOutdoor() }
            return
        }
        loadOutdoor(reset = true)
    }

    private fun showIndoor() {
        val appContext = requireContext().applicationContext
        val key = filterKey(appContext)
        feedEmpty?.visibility = View.GONE
        if (indoorCards.isNotEmpty() && key == indoorFilterKey) {
            installIndoorAdapter()
            feedList?.post { maybeLoadMoreIndoor() }
            return
        }
        indoorFilterKey = key
        loadIndoor(reset = true)
    }

    private fun loadIndoor(reset: Boolean) {
        if (!reset && (indoorLoading || !indoorHasMore)) return
        val list = feedList ?: return
        if (reset) {
            indoorCards.clear()
            indoorOffset = 0
            indoorHasMore = true
            feedAdapter = null
            list.adapter = null
            feedEmpty?.visibility = View.GONE
        }
        indoorLoading = true
        val generation = ++loadGeneration
        outdoorLoading = false
        val offset = indoorOffset
        viewLifecycleOwner.lifecycleScope.launch {
            val appContext = requireContext().applicationContext
            try {
                val favoriteIds = loadSavedIds(appContext)
                val filter = FeedFilterStore.load(appContext)
                val loaded = withContext(Dispatchers.IO) {
                    val board = indoorBoard ?: KilterCatalog.board(appContext)
                    val page = KilterCatalog.page(
                        context = appContext,
                        minGrade = filter.minGrade,
                        maxGrade = filter.maxGrade,
                        angle = filter.angle,
                        sort = filter.kilterSort(),
                        hideSaved = filter.hideSaved,
                        savedIds = favoriteIds,
                        offset = offset,
                    )
                    board to page
                }
                if (!isAdded || generation != loadGeneration) return@launch
                indoorBoard = loaded.first
                val page = loaded.second
                savedIds.clear()
                savedIds.addAll(favoriteIds)
                val more = page.climbs.map { it.toFeedClimb() }
                val wasEmpty = indoorCards.isEmpty()
                indoorCards.addAll(more)
                indoorOffset += page.climbs.size
                indoorHasMore = page.hasMore
                if (outdoorMode) return@launch
                if (wasEmpty || list.adapter == null) {
                    installIndoorAdapter()
                } else {
                    feedAdapter?.append(more)
                }
                feedEmpty?.text = "No climbs match these filters."
                feedEmpty?.visibility = if (indoorCards.isEmpty()) View.VISIBLE else View.GONE
                list.post { maybeLoadMoreIndoor() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded && !outdoorMode && indoorCards.isEmpty()) {
                    Toast.makeText(requireContext(), "Could not load climbs", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (generation == loadGeneration) indoorLoading = false
            }
        }
    }

    private fun maybeLoadMoreIndoor() {
        if (outdoorMode || indoorLoading || !indoorHasMore) return
        val list = feedList ?: return
        val layoutManager = list.layoutManager as? LinearLayoutManager ?: return
        val count = list.adapter?.itemCount ?: 0
        if (count == 0) return
        val last = layoutManager.findLastVisibleItemPosition()
        if (last >= count - 2) loadIndoor(reset = false)
    }

    private fun installIndoorAdapter() {
        val list = feedList ?: return
        val adapter = FeedClimbAdapter(
            climbs = indoorCards.toList(),
            savedUuids = savedIds,
            onSaveClick = { climb -> toggleSave(climb) },
            onDetailsClick = { climb -> openRoute(climb.name) },
            board = indoorBoard,
        )
        feedAdapter = adapter
        list.adapter = adapter
    }

    private fun loadOutdoor(reset: Boolean) {
        if (!reset && outdoorLoading) return
        val session = if (reset) {
            OutdoorFeedSession(OpenBetaClient()).also {
                outdoorSession = it
                outdoorCards.clear()
            }
        } else {
            outdoorSession ?: return
        }
        if (!reset && !session.hasMore()) return
        outdoorLoading = true
        val generation = ++loadGeneration
        if (reset) {
            feedAdapter = null
            feedList?.adapter = null
            feedEmpty?.visibility = View.GONE
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val appContext = requireContext().applicationContext
            try {
                val cards = withContext(Dispatchers.IO) {
                    buildList {
                        var rounds = 0
                        while (isEmpty() && rounds < 3 && session.hasMore()) {
                            addAll(session.nextBatch())
                            rounds++
                        }
                    }
                }
                if (!isAdded || generation != loadGeneration) return@launch
                val more = cards.map { it.toFeedClimb() }
                val wasEmpty = outdoorCards.isEmpty()
                outdoorCards.addAll(more)
                if (!outdoorMode) return@launch
                if (wasEmpty || feedList?.adapter == null) {
                    savedIds.clear()
                    savedIds.addAll(loadSavedIds(appContext))
                    if (!isAdded || generation != loadGeneration || !outdoorMode) return@launch
                    installOutdoorAdapter()
                } else {
                    feedAdapter?.append(more)
                }
                feedEmpty?.text = "No outdoor climbs to show."
                feedEmpty?.visibility = if (outdoorCards.isEmpty()) View.VISIBLE else View.GONE
                feedList?.post { maybeLoadMoreOutdoor() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded && outdoorMode && outdoorCards.isEmpty()) {
                    Toast.makeText(requireContext(), "Could not load outdoor climbs", Toast.LENGTH_SHORT).show()
                    feedEmpty?.text = "No outdoor climbs to show."
                    feedEmpty?.visibility = View.VISIBLE
                }
            } finally {
                if (generation == loadGeneration) outdoorLoading = false
            }
        }
    }

    private fun maybeLoadMoreOutdoor() {
        if (!outdoorMode || outdoorLoading) return
        val session = outdoorSession ?: return
        if (!session.hasMore()) return
        val list = feedList ?: return
        val layoutManager = list.layoutManager as? LinearLayoutManager ?: return
        val count = list.adapter?.itemCount ?: 0
        if (count == 0) return
        val last = layoutManager.findLastVisibleItemPosition()
        if (last >= count - 2) loadOutdoor(reset = false)
    }

    private fun installOutdoorAdapter() {
        val list = feedList ?: return
        val adapter = FeedClimbAdapter(
            climbs = outdoorCards.toList(),
            savedUuids = savedIds,
            onSaveClick = { climb -> toggleSave(climb) },
            onDetailsClick = { climb -> openRoute(climb.name) },
        )
        feedAdapter = adapter
        list.adapter = adapter
    }

    private suspend fun loadSavedIds(appContext: android.content.Context): Set<String> {
        val userId = UserSession.userId(appContext)
        if (userId <= 0L) return emptySet()
        return withContext(Dispatchers.IO) {
            AppDatabase.getInstance(appContext).appDao()
                .getFavorites(userId)
                .first()
                .map { it.boardID }
                .toSet()
        }
    }

    private fun toggleSave(climb: FeedClimb) {
        if (!savesInFlight.add(climb.uuid)) return
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = UserSession.userId(appContext)
                if (userId <= 0L) {
                    Toast.makeText(requireContext(), "Sign in to save climbs", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val dao = AppDatabase.getInstance(appContext).appDao()
                val angle = if (climb.outdoor) OUTDOOR_ANGLE else climb.angleDegrees
                val existing = withContext(Dispatchers.IO) {
                    dao.findFavorite(userId, climb.uuid, angle)
                }
                if (existing == null) {
                    withContext(Dispatchers.IO) {
                        dao.insertFavorite(
                            Favorites(
                                boardID = climb.uuid,
                                angle = angle,
                                uId = userId,
                                climbName = climb.name.takeIf { climb.outdoor },
                                grade = climb.grade.takeIf { climb.outdoor },
                                photoUrl = climb.photoUrl.takeIf { climb.outdoor },
                            )
                        )
                    }
                    savedIds.add(climb.uuid)
                    feedAdapter?.setSaved(climb.uuid, true)
                    if (!climb.outdoor && FeedFilterStore.load(appContext).hideSaved) {
                        indoorFilterKey = filterKey(appContext)
                        loadIndoor(reset = true)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        dao.deleteFavorites(existing.favoriteId)
                    }
                    savedIds.remove(climb.uuid)
                    feedAdapter?.setSaved(climb.uuid, false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Could not save climb", Toast.LENGTH_SHORT).show()
                }
            } finally {
                savesInFlight.remove(climb.uuid)
            }
        }
    }

    private fun openRoute(name: String) {
        startActivity(
            Intent(requireContext(), RouteDetailActivity::class.java)
                .putExtra(RouteDetailActivity.EXTRA_TITLE, name)
        )
    }

    companion object {
        private const val OUTDOOR_ANGLE = -1

        private fun filterKey(context: android.content.Context): String {
            val filter = FeedFilterStore.load(context)
            return listOf(
                filter.minGrade,
                filter.maxGrade,
                filter.angle,
                filter.popularity,
                filter.hideSaved,
            ).joinToString("|")
        }
    }
}

private fun KilterClimb.toFeedClimb(): FeedClimb = FeedClimb(
    uuid = uuid,
    name = name,
    grade = grade,
    angle = "$angle°",
    angleDegrees = angle,
    frames = frames,
)

internal fun FeedFilter.kilterSort(): KilterSort = when (popularity) {
    FeedFilter.LEAST_CLIMBED -> KilterSort.LEAST
    FeedFilter.TOP_CLIMBED -> KilterSort.TOP
    else -> KilterSort.MOST
}

private fun OutdoorCard.toFeedClimb(): FeedClimb = FeedClimb(
    uuid = climbId,
    name = name,
    grade = grade,
    angle = "",
    areaName = areaName,
    photoUrl = photoUrl,
    outdoor = true,
)
