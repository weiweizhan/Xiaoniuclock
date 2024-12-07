package com.example.xiaoniuclock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaoniuclock.model.Alarm

class AlarmAdapter(private val alarms: List<Alarm>) : 
    RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.timeText)
        val orderText: TextView = view.findViewById(R.id.orderText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        val timeString = String.format("%02d:%02d", alarm.hour, alarm.minute)
        holder.timeText.text = timeString
        holder.orderText.text = "序号: ${alarm.orderInGroup + 1}"
    }

    override fun getItemCount() = alarms.size
}
