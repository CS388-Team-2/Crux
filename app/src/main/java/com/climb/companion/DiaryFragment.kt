package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
        val userId = UserSession.userId(requireContext())
        val dao = AppDatabase.getInstance(requireContext()).appDao()
        lifecycleScope.launch {
            dao.getLogs(userId).collect { databaseList ->
                logs.clear()
                logs.addAll(databaseList)
                // adapter.notifyDataSetChanged()  // uncomment once DiaryAdapter exists
            }
        }

        view.findViewById<Button>(R.id.diary_entry_button).setOnClickListener {
            startActivity(Intent(requireContext(), DiaryEntryActivity::class.java))
        }
    }
}
