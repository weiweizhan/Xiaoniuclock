package com.example.xiaoniuclock.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.xiaoniuclock.model.Alarm

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey
    val id: Int,
    var hour: Int,
    var minute: Int,
    var isActive: Boolean,
    val groupId: Int,
    val orderInGroup: Int,
    var isTriggered: Boolean,
    val groupName: String
) {
    fun toAlarm(): Alarm = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        isActive = isActive,
        groupId = groupId,
        orderInGroup = orderInGroup,
        isTriggered = isTriggered,
        groupName = groupName
    )

    companion object {
        fun fromAlarm(alarm: Alarm): AlarmEntity = AlarmEntity(
            id = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            isActive = alarm.isActive,
            groupId = alarm.groupId,
            orderInGroup = alarm.orderInGroup,
            isTriggered = alarm.isTriggered,
            groupName = alarm.groupName
        )
    }
}
