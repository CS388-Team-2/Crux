package com.climb.companion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AttemptAdapter(
    private val attempts: List<AttemptItem>,
    private val onAttemptClick: (AttemptItem) -> Unit,
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
        val context = holder.itemView.context

        holder.titleText.text = "Attempt ${attempt.attemptId}"

        // A clip length replaces the other detail so the row stays on one line
        if (attempt.isAuto) {
            holder.iconImage.setImageResource(R.drawable.ic_baseline_show_chart_24)
            holder.subtitleText.text = if (attempt.clipSeconds > 0) {
                "Auto-detected · ${formatClip(attempt.clipSeconds)} clip"
            } else {
                "Auto-detected · max ${attempt.maxHeight} m"
            }
        } else {
            holder.iconImage.setImageResource(R.drawable.ic_baseline_back_hand_24)
            holder.subtitleText.text = if (attempt.clipSeconds > 0) {
                "Logged manually · ${formatClip(attempt.clipSeconds)} clip"
            } else {
                "Logged manually · ${attempt.time}"
            }
        }

        holder.resultTag.text = attempt.result
        if (attempt.result == "Topped") {
            holder.resultTag.setBackgroundResource(R.drawable.bg_dark)
            holder.resultTag.setTextColor(ContextCompat.getColor(context, R.color.crux_white))
        } else {
            holder.resultTag.setBackgroundResource(R.drawable.bg_feed_chip)
            holder.resultTag.setTextColor(ContextCompat.getColor(context, R.color.crux_ink))
        }

        holder.itemView.setOnClickListener { onAttemptClick(attempt) }
    }

    override fun getItemCount(): Int = attempts.size
}
