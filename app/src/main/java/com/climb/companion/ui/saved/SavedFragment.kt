package com.climb.companion.ui.saved

import android.content.Intent
import android.os.Bundle
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.KilterCatalog
import com.climb.companion.data.UserSession
import com.climb.companion.ui.feed.RouteDetailActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_saved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = view.findViewById<RecyclerView>(R.id.saved_list)
        val empty = view.findViewById<TextView>(R.id.saved_empty)
        list.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val appContext = requireContext().applicationContext
            val rows = try {
                val userId = UserSession.userId(appContext)
                if (userId <= 0L) {
                    emptyList()
                } else {
                    withContext(Dispatchers.IO) {
                        val favorites = AppDatabase.getInstance(appContext).appDao()
                            .getFavorites(userId)
                            .first()
                            .sortedByDescending { it.favoriteId }
                        favorites.mapNotNull { favorite ->
                            val angle = favorite.angle
                            val climb = if (angle != null && angle >= 0) {
                                KilterCatalog.find(appContext, favorite.boardID, angle)
                            } else {
                                null
                            }
                            if (climb != null) {
                                SavedClimbRow(
                                    name = climb.name,
                                    grade = climb.grade,
                                    uuid = climb.uuid,
                                    angle = climb.angle,
                                )
                            } else {
                                val name = favorite.climbName?.takeIf { it.isNotBlank() }
                                    ?: return@mapNotNull null
                                SavedClimbRow(
                                    name = name,
                                    grade = favorite.grade?.takeIf { it.isNotBlank() } ?: "Ungraded",
                                    outdoor = true,
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Could not load saved climbs", Toast.LENGTH_SHORT).show()
                }
                emptyList()
            }
            if (!isAdded) return@launch
            empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            list.adapter = SavedClimbAdapter(rows) { row ->
                if (row.outdoor) return@SavedClimbAdapter
                startActivity(
                    Intent(requireContext(), RouteDetailActivity::class.java)
                        .putExtra(RouteDetailActivity.EXTRA_UUID, row.uuid)
                        .putExtra(RouteDetailActivity.EXTRA_ANGLE, row.angle)
                        .putExtra(RouteDetailActivity.EXTRA_TITLE, row.name)
                )
            }
        }
    }
}
