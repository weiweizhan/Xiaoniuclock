package com.example.xiaoniuclock.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY groupId, orderInGroup")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE groupId = :groupId ORDER BY orderInGroup")
    fun getAlarmsByGroup(groupId: Int): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<AlarmEntity>)

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE groupId = :groupId")
    suspend fun deleteAlarmGroup(groupId: Int)

    @Query("UPDATE alarms SET isActive = :isActive WHERE groupId = :groupId")
    suspend fun updateGroupActiveStatus(groupId: Int, isActive: Boolean)
}
