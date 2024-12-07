package com.example.xiaoniuclock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaoniuclock.model.Alarm
import com.google.android.material.switchmaterial.SwitchMaterial

class GroupAlarmAdapter(
    private val alarms: List<Alarm>,
    private val onAlarmToggle: (Alarm, Boolean) -> Unit
) : RecyclerView.Adapter<GroupAlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alarmTime)
        val alarmSwitch: SwitchMaterial = view.findViewById(R.id.alarmSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_item_in_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        
        // 设置时间文本
        holder.timeText.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        
        // 移除之前的监听器
        holder.alarmSwitch.setOnCheckedChangeListener(null)
        
        // 设置开关状态
        holder.alarmSwitch.isChecked = alarm.isActive
        
        // 设置新的开关监听器
        holder.alarmSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {  // 只有用户操作才触发
                alarm.isActive = isChecked  // 立即更新本地状态
                onAlarmToggle(alarm, isChecked)
            }
        }
    }

    override fun getItemCount() = alarms.size
}
