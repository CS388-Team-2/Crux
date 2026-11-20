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
    private var feedAdapter: FeedClimbAdapter? = null
    private var feedList: RecyclerView? = null
    private var feedEmpty: TextView? = null

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            loadFeed()
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

        feedEmpty = view.findViewById(R.id.feed_empty)
        val list = view.findViewById<RecyclerView>(R.id.feed_list)
        feedList = list
        list.layoutManager = LinearLayoutManager(requireContext())
        PagerSnapHelper().attachToRecyclerView(list)
        loadFeed()
    }

    private fun loadFeed() {
        val list = feedList ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val appContext = requireContext().applicationContext
            try {
                CatalogDatabase.ensureLoaded(appContext)
                val climbs = withContext(Dispatchers.IO) {
                    CatalogDatabase.getInstance(appContext).catalogDao().getAll()
                }
                val userId = UserSession.userId(appContext)
                val savedIds = if (userId > 0L) {
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
                feedEmpty?.visibility = if (feedClimbs.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Could not load climbs", Toast.LENGTH_SHORT).show()
                }
            }
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
                val existing = withContext(Dispatchers.IO) {
                    dao.findFavorite(userId, climb.uuid, FEED_ANGLE)
                }
                if (existing == null) {
                    withContext(Dispatchers.IO) {
                        dao.insertFavorite(
                            Favorites(
                                boardID = climb.uuid,
                                angle = FEED_ANGLE,
                                uId = userId,
                            )
                        )
                    }
                    feedAdapter?.setSaved(climb.uuid, true)
                    if (FeedFilterStore.load(appContext).hideSaved) {
                        loadFeed()
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        dao.deleteFavorites(existing.favoriteId)
                    }
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
    }
}
