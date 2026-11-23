package com.climb.companion.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.climb.companion.R
import com.climb.companion.data.AppDatabase
import com.climb.companion.data.UserSession
import com.climb.companion.data.entity.Login
import com.climb.companion.ui.auth.ChangePasswordActivity
import com.climb.companion.ui.auth.WelcomeActivity
import com.climb.companion.ui.permissions.PermissionsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var login: Login? = null
    private var editingFirst = false
    private var editingLast = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val firstInput = view.findViewById<EditText>(R.id.profile_first_name_input)
        val lastInput = view.findViewById<EditText>(R.id.profile_last_name_input)
        view.findViewById<ImageButton>(R.id.profile_first_name_edit).setOnClickListener {
            if (editingFirst) saveName(first = true) else beginEdit(first = true)
        }
        view.findViewById<ImageButton>(R.id.profile_last_name_edit).setOnClickListener {
            if (editingLast) saveName(first = false) else beginEdit(first = false)
        }
        firstInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && editingFirst) {
                saveName(first = true)
                true
            } else {
                false
            }
        }
        lastInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && editingLast) {
                saveName(first = false)
                true
            } else {
                false
            }
        }
        view.findViewById<TextView>(R.id.profile_change_password).setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }
        view.findViewById<TextView>(R.id.profile_permissions).setOnClickListener {
            startActivity(
                Intent(requireContext(), PermissionsActivity::class.java)
                    .putExtra(PermissionsActivity.EXTRA_FROM_PROFILE, true)
            )
        }
        view.findViewById<Button>(R.id.profile_sign_out).setOnClickListener {
            leaveToWelcome()
        }
        view.findViewById<Button>(R.id.profile_delete_account).setOnClickListener {
            confirmDelete()
        }
    }

    override fun onResume() {
        super.onResume()
        editingFirst = false
        editingLast = false
        val userId = UserSession.userId(requireContext())
        if (userId <= 0L) {
            showSignedOut()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).appDao().getLoginById(userId)
            }
            if (loaded == null) {
                showSignedOut()
            } else {
                login = loaded
                showAccount(loaded)
            }
        }
    }

    private fun showSignedOut() {
        val view = view ?: return
        login = null
        view.findViewById<TextView>(R.id.profile_greeting).text = "Not signed in"
        view.findViewById<View>(R.id.profile_account).visibility = View.GONE
    }

    private fun showAccount(account: Login) {
        val view = view ?: return
        view.findViewById<View>(R.id.profile_account).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.profile_greeting).text = greeting(account)
        view.findViewById<TextView>(R.id.profile_username).text = account.Username
        showNameAsText(first = true, value = account.FirstName)
        showNameAsText(first = false, value = account.LastName)
    }

    private fun greeting(account: Login): String {
        val name = account.FirstName.ifBlank { account.Username }
        return "Hi, $name"
    }

    private fun beginEdit(first: Boolean) {
        val account = login ?: return
        val view = view ?: return
        val value = if (first) account.FirstName else account.LastName
        val text = view.findViewById<TextView>(if (first) R.id.profile_first_name else R.id.profile_last_name)
        val input = view.findViewById<EditText>(if (first) R.id.profile_first_name_input else R.id.profile_last_name_input)
        val edit = view.findViewById<ImageButton>(if (first) R.id.profile_first_name_edit else R.id.profile_last_name_edit)
        if (first) editingFirst = true else editingLast = true
        input.setText(value)
        text.visibility = View.GONE
        input.visibility = View.VISIBLE
        edit.setImageResource(R.drawable.ic_check)
        edit.contentDescription = if (first) "Save first name" else "Save last name"
        input.requestFocus()
        input.setSelection(input.text.length)
    }

    private fun showNameAsText(first: Boolean, value: String) {
        val view = view ?: return
        val text = view.findViewById<TextView>(if (first) R.id.profile_first_name else R.id.profile_last_name)
        val input = view.findViewById<EditText>(if (first) R.id.profile_first_name_input else R.id.profile_last_name_input)
        val edit = view.findViewById<ImageButton>(if (first) R.id.profile_first_name_edit else R.id.profile_last_name_edit)
        text.text = value
        text.visibility = View.VISIBLE
        input.visibility = View.GONE
        edit.setImageResource(R.drawable.ic_edit)
        edit.contentDescription = if (first) "Edit first name" else "Edit last name"
        edit.isEnabled = true
    }

    private fun saveName(first: Boolean) {
        val account = login ?: return
        val view = view ?: return
        val input = view.findViewById<EditText>(if (first) R.id.profile_first_name_input else R.id.profile_last_name_input)
        val edit = view.findViewById<ImageButton>(if (first) R.id.profile_first_name_edit else R.id.profile_last_name_edit)
        val updatedFirst = if (first) input.text.toString().trim() else account.FirstName
        val updatedLast = if (first) account.LastName else input.text.toString().trim()
        edit.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).appDao()
                        .updateNames(account.uId, updatedFirst, updatedLast)
                }
                val updated = account.copy(FirstName = updatedFirst, LastName = updatedLast)
                login = updated
                if (first) editingFirst = false else editingLast = false
                hideKeyboard(input)
                showNameAsText(first, if (first) updatedFirst else updatedLast)
                view.findViewById<TextView>(R.id.profile_greeting).text = greeting(updated)
            } finally {
                view.findViewById<ImageButton>(
                    if (first) R.id.profile_first_name_edit else R.id.profile_last_name_edit,
                ).isEnabled = true
            }
        }
    }

    private fun hideKeyboard(focused: View) {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This removes your account, diary, sessions, and saved climbs from this phone.")
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val userId = UserSession.userId(requireContext())
        if (userId <= 0L) {
            leaveToWelcome()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).appDao().deleteLogin(userId)
            }
            leaveToWelcome()
        }
    }

    private fun leaveToWelcome() {
        UserSession.clear(requireContext())
        startActivity(
            Intent(requireContext(), WelcomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
