package com.climb.companion.ui.feed

import android.view.LayoutInflater
import com.climb.companion.R
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.climb.companion.data.BoardLayout
import com.google.android.material.button.MaterialButton

class FeedClimbAdapter(
    climbs: List<FeedClimb>,
    savedUuids: Set<String>,
    private val onSaveClick: (FeedClimb) -> Unit,
    private val onDetailsClick: (FeedClimb) -> Unit,
    private val board: BoardLayout? = null,
) : RecyclerView.Adapter<FeedClimbAdapter.ClimbViewHolder>() {

    private val climbs = climbs.toMutableList()
    private val savedUuids = savedUuids.toMutableSet()

    fun setSaved(uuid: String, saved: Boolean) {
        val changed = if (saved) savedUuids.add(uuid) else savedUuids.remove(uuid)
        if (!changed) return
        val index = climbs.indexOfFirst { it.uuid == uuid }
        if (index >= 0) notifyItemChanged(index)
    }

    fun append(more: List<FeedClimb>) {
        if (more.isEmpty()) return
        val start = climbs.size
        climbs.addAll(more)
        notifyItemRangeInserted(start, more.size)
    }

    class ClimbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photo: ImageView = itemView.findViewById(R.id.climb_photo)
        val boardView: BoardView = itemView.findViewById(R.id.board_view)
        val nameText: TextView = itemView.findViewById(R.id.climb_name_text)
        val gradeText: TextView = itemView.findViewById(R.id.grade_text)
        val angleText: TextView = itemView.findViewById(R.id.angle_text)
        val areaText: TextView = itemView.findViewById(R.id.area_text)
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
        holder.saveButton.text = if (climb.uuid in savedUuids) "Saved" else "Save"
        holder.saveButton.setOnClickListener { onSaveClick(climb) }
        if (climb.outdoor) {
            holder.boardView.visibility = View.GONE
            holder.boardView.clear()
            holder.photo.visibility = View.VISIBLE
            holder.angleText.visibility = View.GONE
            holder.detailsButton.visibility = View.GONE
            holder.areaText.visibility = if (climb.areaName.isBlank()) View.GONE else View.VISIBLE
            holder.areaText.text = climb.areaName
            holder.photo.contentDescription = climb.name
            holder.photo.scaleType = ImageView.ScaleType.CENTER_CROP
            // Coil applies EXIF orientation, so sideways phone photos show upright.
            holder.photo.load(climb.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_feed_hold)
                error(R.drawable.bg_feed_hold)
            }
        } else {
            holder.photo.dispose()
            holder.photo.setImageDrawable(null)
            holder.photo.visibility = View.GONE
            holder.boardView.visibility = View.VISIBLE
            val layout = board
            if (layout == null) holder.boardView.clear() else holder.boardView.show(layout, climb.frames)
            holder.angleText.visibility = View.VISIBLE
            holder.angleText.text = climb.angle
            holder.detailsButton.visibility = View.VISIBLE
            holder.detailsButton.setOnClickListener { onDetailsClick(climb) }
            holder.areaText.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = climbs.size
}
