package com.climb.companion.ui.saved

import android.view.LayoutInflater
import com.climb.companion.R
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SavedClimbRow(
    val name: String,
    val grade: String,
)

class SavedClimbAdapter(
    private val climbs: List<SavedClimbRow>,
    private val onClick: (SavedClimbRow) -> Unit,
) : RecyclerView.Adapter<SavedClimbAdapter.RowViewHolder>() {

    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.saved_name)
        val gradeText: TextView = itemView.findViewById(R.id.saved_grade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_climb, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val climb = climbs[position]
        holder.nameText.text = climb.name
        holder.gradeText.text = climb.grade
        holder.itemView.setOnClickListener { onClick(climb) }
    }

    override fun getItemCount(): Int = climbs.size
}
