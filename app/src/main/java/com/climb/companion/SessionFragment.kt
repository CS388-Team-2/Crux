package com.climb.companion

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class SessionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_session, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.record_button).setOnClickListener {
            startActivity(Intent(requireContext(), RecordActivity::class.java))
        }
        view.findViewById<Button>(R.id.edit_attempt_button).setOnClickListener {
            startActivity(Intent(requireContext(), EditAttemptActivity::class.java))
        }
    }
}
