package com.climb.companion.ui.feed

import android.view.LayoutInflater
import com.climb.companion.R
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class FeedClimbAdapter(
    private val climbs: List<FeedClimb>,
    savedUuids: Set<String>,
    private val onSaveClick: (FeedClimb) -> Unit,
    private val onDetailsClick: (FeedClimb) -> Unit,
) : RecyclerView.Adapter<FeedClimbAdapter.ClimbViewHolder>() {

    private val savedUuids = savedUuids.toMutableSet()

    fun setSaved(uuid: String, saved: Boolean) {
        val changed = if (saved) savedUuids.add(uuid) else savedUuids.remove(uuid)
        if (!changed) return
        val index = climbs.indexOfFirst { it.uuid == uuid }
        if (index >= 0) notifyItemChanged(index)
    }

    class ClimbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.climb_name_text)
        val gradeText: TextView = itemView.findViewById(R.id.grade_text)
        val angleText: TextView = itemView.findViewById(R.id.angle_text)
        val saveButton: MaterialButton = itemView.findViewById(R.id.save_button)
        val detailsButton: MaterialButton = itemView.findViewById(R.id.details_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClimbViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_climb, parent, false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        return ClimbViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClimbViewHolder, position: Int) {
        val climb = climbs[position]
        holder.nameText.text = climb.name
        holder.gradeText.text = climb.grade
        holder.angleText.text = climb.angle
        holder.saveButton.text = if (climb.uuid in savedUuids) "Saved" else "Save"
        holder.saveButton.setOnClickListener { onSaveClick(climb) }
        holder.detailsButton.setOnClickListener { onDetailsClick(climb) }
    }

    override fun getItemCount(): Int = climbs.size
}
