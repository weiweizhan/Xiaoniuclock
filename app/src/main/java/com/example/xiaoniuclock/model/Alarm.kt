package com.example.xiaoniuclock.model

import java.io.Serializable

data class Alarm(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var isActive: Boolean = true,
    val groupId: Int, // 用于标识属于哪个级联组
    val orderInGroup: Int, // 在级联组中的顺序
    var isTriggered: Boolean = false, // 标记是否已经触发过
    val groupName: String = ""  // Add group name field
) : Serializable {
    fun getTimeInMinutes(): Int = hour * 60 + minute
}
