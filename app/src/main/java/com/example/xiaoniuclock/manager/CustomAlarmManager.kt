package com.example.xiaoniuclock.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.xiaoniuclock.model.Alarm
import com.example.xiaoniuclock.service.AlarmService
import com.example.xiaoniuclock.repository.AlarmRepository
import com.example.xiaoniuclock.database.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class CustomAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val repository: AlarmRepository
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        val database = AlarmDatabase.getDatabase(context)
        repository = AlarmRepository(database.alarmDao())
    }

    companion object {
        private var instance: CustomAlarmManager? = null
        
        fun getInstance(context: Context): CustomAlarmManager {
            if (instance == null) {
                instance = CustomAlarmManager(context)
            }
            return instance!!
        }
    }

    // 存储所有闹钟的映射
    private val alarmMap = mutableMapOf<Int, Alarm>()
    private val groupAlarms = mutableMapOf<Int, List<Alarm>>()

    fun scheduleAlarm(alarm: Alarm) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("group_id", alarm.groupId)
        }

        val pendingIntent = PendingIntent.getService(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )

        // 保存到数据库
        scope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("group_id", alarm.groupId)
        }

        val pendingIntent = PendingIntent.getService(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        // 更新数据库
        scope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun enableAlarm(alarm: Alarm) {
        if (!alarm.isActive) {
            alarm.isActive = true
            scheduleAlarm(alarm)
            // 确保在数据库中更新状态
            scope.launch {
                repository.updateAlarm(alarm)
            }
        }
    }

    fun disableAlarm(alarm: Alarm) {
        if (alarm.isActive) {
            alarm.isActive = false
            cancelAlarm(alarm)
            // 确保在数据库中更新状态
            scope.launch {
                repository.updateAlarm(alarm)
            }
        }
    }

    fun addAlarmGroup(alarms: List<Alarm>) {
        if (alarms.isEmpty()) return
        
        // 保存到数据库
        scope.launch {
            repository.insertAlarms(alarms)
        }

        // 设置活跃的闹钟
        alarms.forEach { alarm ->
            alarmMap[alarm.id] = alarm
            if (alarm.isActive) {
                scheduleAlarm(alarm)
            }
        }
        groupAlarms[alarms.first().groupId] = alarms
    }

    fun enableAlarmGroup(groupId: Int) {
        scope.launch {
            repository.updateGroupActiveStatus(groupId, true)
            repository.getAlarmsByGroup(groupId).collect { alarms ->
                alarms.forEach { alarm ->
                    alarm.isActive = true
                    scheduleAlarm(alarm)
                }
            }
        }
    }

    fun disableAlarmGroup(groupId: Int) {
        scope.launch {
            repository.updateGroupActiveStatus(groupId, false)
            repository.getAlarmsByGroup(groupId).collect { alarms ->
                alarms.forEach { alarm ->
                    alarm.isActive = false
                    cancelAlarm(alarm)
                }
            }
        }
    }

    fun onAlarmDismissed(groupId: Int, alarmId: Int) {
        scope.launch {
            val alarms = repository.getAlarmsByGroup(groupId).collect { alarms ->
                val currentAlarm = alarms.find { it.id == alarmId }
                if (currentAlarm != null) {
                    currentAlarm.isTriggered = true
                    repository.updateAlarm(currentAlarm)

                    // 取消后续闹钟
                    alarms.filter { it.orderInGroup > currentAlarm.orderInGroup }
                        .forEach { alarm ->
                            alarm.isActive = false
                            cancelAlarm(alarm)
                            repository.updateAlarm(alarm)
                        }
                }
            }
        }
    }

    fun onAlarmSnoozed(groupId: Int, alarmId: Int) {
        scope.launch {
            repository.getAlarmsByGroup(groupId).collect { alarms ->
                val alarm = alarms.find { it.id == alarmId } ?: return@collect
                
                // 计算5分钟后的时间
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, 5)
                }
                
                // 更新闹钟时间
                alarm.hour = calendar.get(Calendar.HOUR_OF_DAY)
                alarm.minute = calendar.get(Calendar.MINUTE)
                
                // 保存到数据库并重新调度闹钟
                repository.updateAlarm(alarm)
                scheduleAlarm(alarm)
            }
        }
    }
}
