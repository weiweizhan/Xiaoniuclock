package com.example.xiaoniuclock.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.xiaoniuclock.MainActivity
import com.example.xiaoniuclock.R
import com.example.xiaoniuclock.manager.CustomAlarmManager
import com.example.xiaoniuclock.model.Alarm

class AlarmService : Service() {
    private lateinit var alarmManager: CustomAlarmManager
    
    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_DISMISS_ALARM = "com.example.xiaoniuclock.DISMISS_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        alarmManager = CustomAlarmManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS_ALARM -> {
                val alarmId = intent.getIntExtra("alarm_id", -1)
                val groupId = intent.getIntExtra("group_id", -1)
                if (alarmId != -1 && groupId != -1) {
                    alarmManager.onAlarmDismissed(groupId, alarmId)
                    stopForeground(true)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
                val groupId = intent?.getIntExtra("group_id", -1) ?: -1

                if (alarmId != -1 && groupId != -1) {
                    // 显示闹钟提醒通知
                    val notification = createAlarmNotification(alarmId, groupId)
                    startForeground(NOTIFICATION_ID, notification)
                    
                    // 显示闹钟对话框
                    showAlarmDialog(alarmId, groupId)
                }
            }
        }

        return START_STICKY
    }

    private fun showAlarmDialog(alarmId: Int, groupId: Int) {
        val dialogIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("show_alarm_dialog", true)
            putExtra("alarm_id", alarmId)
            putExtra("group_id", groupId)
        }
        startActivity(dialogIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "闹钟通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "用于显示闹钟提醒"
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createAlarmNotification(alarmId: Int, groupId: Int): Notification {
        // 创建用于关闭闹钟的 PendingIntent
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra("alarm_id", alarmId)
            putExtra("group_id", groupId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            alarmId + 1000, // 避免与其他 PendingIntent 冲突
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建打开应用的 PendingIntent
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_alarm_dialog", true)
            putExtra("alarm_id", alarmId)
            putExtra("group_id", groupId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("闹钟响铃")
            .setContentText("点击查看详情")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "关闭", dismissPendingIntent)
            .setAutoCancel(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
