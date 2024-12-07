package com.example.xiaoniuclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.xiaoniuclock.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val groupId = intent.getIntExtra("group_id", -1)
        
        if (alarmId != -1 && groupId != -1) {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("group_id", groupId)
            }
            context.startService(serviceIntent)
        }
    }
}
