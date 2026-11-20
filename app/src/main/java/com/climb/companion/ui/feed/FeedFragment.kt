package com.climb.companion.ui.feed

import android.content.Intent
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.CatalogDatabase
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.Favorites
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
    private var feedAdapter: FeedClimbAdapter? = null
    private var feedList: RecyclerView? = null
    private var feedEmpty: TextView? = null
    private var gymChip: TextView? = null
    private var outdoorMode = false
    private var outdoorLoading = false
    private var loadGeneration = 0
    private var outdoorSession: OutdoorFeedSession? = null

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && !outdoorMode) {
            loadIndoor()
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

        gymChip = view.findViewById(R.id.gym_chip)
        gymChip?.setOnClickListener {
            val nextOutdoor = !FeedModeStore.isOutdoor(requireContext())
            FeedModeStore.setOutdoor(requireContext(), nextOutdoor)
            showMode(nextOutdoor)
            if (nextOutdoor) showOutdoor() else loadIndoor()
        }

        feedEmpty = view.findViewById(R.id.feed_empty)
        val list = view.findViewById<RecyclerView>(R.id.feed_list)
        feedList = list
        list.layoutManager = LinearLayoutManager(requireContext())
        PagerSnapHelper().attachToRecyclerView(list)
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                maybeLoadMoreOutdoor()
            }
        })

        showMode(FeedModeStore.isOutdoor(requireContext()))
        if (outdoorMode) showOutdoor() else loadIndoor()
    }

    private fun showMode(outdoor: Boolean) {
        outdoorMode = outdoor
        gymChip?.text = if (outdoor) "Outdoor" else "Indoor"
        gymChip?.contentDescription = if (outdoor) {
            "Outdoor climbs. Tap to switch to indoor."
        } else {
            "Indoor climbs. Tap to switch to outdoor."
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

    private fun loadIndoor() {
        val list = feedList ?: return
        val generation = ++loadGeneration
        outdoorLoading = false
        viewLifecycleOwner.lifecycleScope.launch {
            val appContext = requireContext().applicationContext
            try {
                CatalogDatabase.ensureLoaded(appContext)
                val climbs = withContext(Dispatchers.IO) {
                    CatalogDatabase.getInstance(appContext).catalogDao().getAll()
                }
                val favoriteIds = loadSavedIds(appContext)
                if (!isAdded || generation != loadGeneration || outdoorMode) return@launch
                savedIds.clear()
                savedIds.addAll(favoriteIds)
                val filter = FeedFilterStore.load(appContext)
                val visible = filter.matching(climbs, savedIds)
                val feedClimbs = visible.map { climb ->
                    FeedClimb(
                        uuid = climb.uuid,
                        name = climb.name,
                        grade = climb.grade,
                        angle = "${climb.angle}°",
                    )
                }
                val adapter = FeedClimbAdapter(
                    climbs = feedClimbs,
                    savedUuids = savedIds,
                    onSaveClick = { climb -> toggleSave(climb) },
                    onDetailsClick = { climb -> openRoute(climb.name) },
                )
                feedAdapter = adapter
                list.adapter = adapter
                feedEmpty?.text = "No climbs match these filters."
                feedEmpty?.visibility = if (feedClimbs.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded && !outdoorMode) {
                    Toast.makeText(requireContext(), "Could not load climbs", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                val angle = if (climb.outdoor) OUTDOOR_ANGLE else FEED_ANGLE
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
                        loadIndoor()
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
        private const val FEED_ANGLE = 40
        private const val OUTDOOR_ANGLE = -1
    }
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
