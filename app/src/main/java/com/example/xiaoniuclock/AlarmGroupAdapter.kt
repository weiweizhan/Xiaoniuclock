package com.example.xiaoniuclock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaoniuclock.model.Alarm
import com.google.android.material.switchmaterial.SwitchMaterial

class AlarmGroupAdapter(
    private val groups: List<List<Alarm>>,
    private val onGroupClick: (Int) -> Unit,
    private val onGroupToggle: (Int, Boolean) -> Unit,
    private val onAlarmToggle: (Alarm, Boolean) -> Unit
) : RecyclerView.Adapter<AlarmGroupAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupTitle: TextView = view.findViewById(R.id.groupTitle)
        val groupSwitch: SwitchMaterial = view.findViewById(R.id.groupSwitch)
        val alarmsList: RecyclerView = view.findViewById(R.id.groupAlarmsList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_group_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        val groupName = group.firstOrNull()?.groupName ?: "闹钟组 ${position + 1}"
        holder.groupTitle.text = groupName

        // 设置组开关状态（如果组内所有闹钟都是激活的，则组开关为开）
        val isGroupActive = group.all { it.isActive }
        holder.groupSwitch.isChecked = isGroupActive

        // 设置组开关监听器
        holder.groupSwitch.setOnCheckedChangeListener { _, isChecked ->
            onGroupToggle(position, isChecked)
        }

        // 设置闹钟列表
        holder.alarmsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GroupAlarmAdapter(group) { alarm, isChecked ->
                onAlarmToggle(alarm, isChecked)
            }
        }

        // 设置组点击事件（编辑）
        holder.itemView.setOnClickListener {
            onGroupClick(position)
        }
    }

    override fun getItemCount() = groups.size
}
