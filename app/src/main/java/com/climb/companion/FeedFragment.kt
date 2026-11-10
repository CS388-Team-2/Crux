package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class FeedFragment : Fragment() {

    private val sampleClimbs = listOf(
        FeedClimb("Crimp Lord", "V5", "40°"),
        FeedClimb("Slab Party", "V3", "30°"),
        FeedClimb("Pinch Canyon", "V7", "45°"),
    )

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
        feedList.adapter = FeedClimbAdapter(
            climbs = sampleClimbs,
            onSaveClick = { },
            onDetailsClick = {
                startActivity(Intent(requireContext(), RouteDetailActivity::class.java))
            },
        )
        PagerSnapHelper().attachToRecyclerView(feedList)
    }
}
