package com.climb.companion.ui.diary

import android.content.Intent
import com.climb.companion.R
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
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.DiaryListItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


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

        val logs = mutableListOf<DiaryListItem>()
        val logAdapter = DiaryListAdapter(requireContext(), logs)
        val logRecycler = view.findViewById<RecyclerView>(R.id.diary_recycler_view)
        logRecycler.adapter = logAdapter
        logRecycler.layoutManager = LinearLayoutManager(requireContext())

        val userId = UserSession.userId(requireContext())
        val dao = AppDatabase.getInstance(requireContext()).appDao()

        lifecycleScope.launch{
            dao.getLogSessionData(userId).collect {databaseList ->
                logs.clear()
                logs.addAll(databaseList)
                logAdapter.notifyDataSetChanged()}
        }

        view.findViewById<Button>(R.id.diary_entry_button).setOnClickListener {
            val todayDate: Date? = Calendar.getInstance().getTime()
            val formatter = SimpleDateFormat("yyyy-MM-dd")
            val date = formatter.format(todayDate)
            lifecycleScope.launch{
                val exists = dao.checkLogExistence(userId, date)
                if (exists == null) {
                    startActivity(Intent(requireContext(), DiaryEntryActivity::class.java))
                }
                else{
                    val intent = Intent(context, DiaryEntryActivity::class.java)
                    intent.putExtra("logId", exists)
                    startActivity(intent)
                }
            }
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
