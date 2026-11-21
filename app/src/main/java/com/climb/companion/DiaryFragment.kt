package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch


class DiaryFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logs = mutableListOf<ClimbLogs>()
        val logAdapter = DiaryListAdapter(requireContext(), logs)
        val logRecycler = view.findViewById<RecyclerView>(R.id.diary_recycler_view)
        logRecycler.adapter = logAdapter
        logRecycler.layoutManager = LinearLayoutManager(requireContext())

        val userId = UserSession.userId(requireContext())
        val dao = AppDatabase.getInstance(requireContext()).appDao()
        lifecycleScope.launch {
            dao.getLogs(userId).collect { databaseList ->
                logs.clear()
                logs.addAll(databaseList)
                logAdapter.notifyDataSetChanged()
            }
        }

        view.findViewById<Button>(R.id.diary_entry_button).setOnClickListener {
            startActivity(Intent(requireContext(), DiaryEntryActivity::class.java))
        }

        val simpleItemTouchCallback = object :
            ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT
            ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                Toast.makeText(requireContext(), "Item Deleted ", Toast.LENGTH_SHORT).show()
                //Remove swiped item from list and notify the RecyclerView
                val position = viewHolder.getAdapterPosition()
                val logId = logs[position].logId
                logs.removeAt(position)
                logAdapter.notifyDataSetChanged()
                lifecycleScope.launch {
                    dao.deleteLog(logId)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(logRecycler)
    }


}
