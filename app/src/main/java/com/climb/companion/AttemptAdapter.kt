package com.climb.companion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// list is newest first
class AttemptAdapter(
    private val attempts: List<Attempts>,
    private val onAttemptClick: (Attempts, Int) -> Unit,
) : RecyclerView.Adapter<AttemptAdapter.AttemptViewHolder>() {

    class AttemptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImage: ImageView = itemView.findViewById(R.id.attempt_icon)
        val titleText: TextView = itemView.findViewById(R.id.attempt_title)
        val subtitleText: TextView = itemView.findViewById(R.id.attempt_subtitle)
        val resultTag: TextView = itemView.findViewById(R.id.attempt_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttemptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attempt, parent, false)
        return AttemptViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttemptViewHolder, position: Int) {
        val attempt = attempts[position]
        val number = attempts.size - position
        val context = holder.itemView.context

        // example: "Attempt 3 - Crimp Lord · V5" (just "Attempt 3" if there is no route)
        val route = attempt.route
        holder.titleText.text = if (route.isNullOrBlank()) "Attempt $number" else "Attempt $number - $route"

        if (attempt.isAuto) {
            holder.iconImage.setImageResource(R.drawable.ic_baseline_show_chart_24)
            holder.subtitleText.text = "Auto-detected · max ${"%.1f".format(attempt.maxHeight)} m"
        } else {
            holder.iconImage.setImageResource(R.drawable.ic_baseline_back_hand_24)
            holder.subtitleText.text = "Logged manually · ${attempt.time}"
        }

        holder.resultTag.text = attempt.result
        if (attempt.result == "Topped") {
            holder.resultTag.setBackgroundResource(R.drawable.bg_dark)
            holder.resultTag.setTextColor(ContextCompat.getColor(context, R.color.crux_white))
        } else {
            holder.resultTag.setBackgroundResource(R.drawable.bg_feed_chip)
            holder.resultTag.setTextColor(ContextCompat.getColor(context, R.color.crux_ink))
        }

        holder.itemView.setOnClickListener { onAttemptClick(attempt, number) }
    }

    override fun getItemCount(): Int = attempts.size
}
