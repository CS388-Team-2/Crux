package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val openFilter = View.OnClickListener {
            startActivity(Intent(requireContext(), FilterActivity::class.java))
        }
        view.findViewById<View>(R.id.filter_button).setOnClickListener(openFilter)
        view.findViewById<View>(R.id.gym_chip).setOnClickListener(openFilter)

        val feedList = view.findViewById<RecyclerView>(R.id.feed_list)
        feedList.layoutManager = LinearLayoutManager(requireContext())
        PagerSnapHelper().attachToRecyclerView(feedList)

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
                            .filter { it.angle == FEED_ANGLE }
                            .map { it.boardID }
                            .toSet()
                    }
                } else {
                    emptySet()
                }
                val feedClimbs = climbs.map { climb ->
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
                feedList.adapter = adapter
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
