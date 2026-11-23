package com.climb.companion.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.climb.companion.R
import com.climb.companion.data.PermissionChoices
import com.climb.companion.data.UserSession
import com.climb.companion.ui.permissions.PermissionsActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.profile_permissions_button).setOnClickListener {
            startActivity(
                Intent(requireContext(), PermissionsActivity::class.java)
                    .putExtra(PermissionsActivity.EXTRA_FROM_PROFILE, true)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val view = view ?: return
        val username = UserSession.username(requireContext())
        view.findViewById<TextView>(R.id.profile_username).text = if (username.isNullOrBlank()) {
            "Not signed in"
        } else {
            username
        }
        val choice = when {
            PermissionChoices.allowed(requireContext()) -> "You allowed tracking permissions."
            PermissionChoices.hasChoice(requireContext()) -> "You chose not to allow tracking permissions."
            else -> "You have not chosen tracking permissions yet."
        }
        view.findViewById<TextView>(R.id.profile_permission_status).text = choice
    }
}
