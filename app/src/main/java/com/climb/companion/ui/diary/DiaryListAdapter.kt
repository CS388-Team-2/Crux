package com.climb.companion.ui.diary

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.climb.companion.R
import android.content.Intent
import com.climb.companion.data.entity.DiaryListItem

class DiaryListAdapter(private val context: Context, private val logs: List<DiaryListItem>) :  RecyclerView.Adapter<DiaryListAdapter.LogsViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary, parent, false)
        return LogsViewHolder(view)
    }

    inner class LogsViewHolder(val dView: View)  : RecyclerView.ViewHolder(dView),
        View.OnClickListener{
        val dDate: TextView = dView.findViewById<TextView>(R.id.diaryEntryDate)
        val dTitle: TextView = dView.findViewById<TextView>(R.id.diaryEntryTitle)
        val dTime: TextView = dView.findViewById<TextView>(R.id.diaryEntryTime)
        val dAttempts: TextView = dView.findViewById<TextView>(R.id.diaryEntryAttempts)
        val dTops: TextView = dView.findViewById<TextView>(R.id.diaryEntryTops)
        val dDistance: TextView = dView.findViewById<TextView>(R.id.diaryEntryDistance)
        val dBody: TextView = dView.findViewById<TextView>(R.id.diaryEntryBody)

        init {
            dView.setOnClickListener(this)
        }

        fun bind(log: DiaryListItem){
            dDate.text = log.date
            dTitle.text = log.title
            dTime.text = formatShortDuration(log.totalTime)
            dAttempts.text = "${log.attemptCount} Attempts"
            dTops.text = "${log.topCount} Tops"
            dDistance.text = "${log.totalDistance}m Climbed"
            dBody.text = log.notes
        }

        override fun onClick(v: View?) {
            val log = logs[absoluteAdapterPosition]
            val intent = Intent(context, DiaryEntryActivity::class.java)
            intent.putExtra("logId", log.logId)
            context.startActivity(intent)
        }
    }

    override fun onBindViewHolder(holder: LogsViewHolder, position: Int) {
        val log = logs[position]
        holder.bind(log)
    }

    override fun getItemCount(): Int {
        return logs.size
    }
}