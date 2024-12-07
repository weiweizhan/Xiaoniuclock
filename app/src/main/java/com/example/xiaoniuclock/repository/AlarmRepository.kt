package com.example.xiaoniuclock.repository

import com.example.xiaoniuclock.database.AlarmDao
import com.example.xiaoniuclock.database.AlarmEntity
import com.example.xiaoniuclock.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val alarmDao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
        .map { entities -> entities.map { it.toAlarm() } }

    fun getAlarmsByGroup(groupId: Int): Flow<List<Alarm>> =
        alarmDao.getAlarmsByGroup(groupId)
            .map { entities -> entities.map { it.toAlarm() } }

    suspend fun insertAlarm(alarm: Alarm) {
        alarmDao.insertAlarm(AlarmEntity.fromAlarm(alarm))
    }

    suspend fun insertAlarms(alarms: List<Alarm>) {
        alarmDao.insertAlarms(alarms.map { AlarmEntity.fromAlarm(it) })
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(AlarmEntity.fromAlarm(alarm))
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(AlarmEntity.fromAlarm(alarm))
    }

    suspend fun deleteAlarmGroup(groupId: Int) {
        alarmDao.deleteAlarmGroup(groupId)
    }

    suspend fun updateGroupActiveStatus(groupId: Int, isActive: Boolean) {
        alarmDao.updateGroupActiveStatus(groupId, isActive)
    }
}
