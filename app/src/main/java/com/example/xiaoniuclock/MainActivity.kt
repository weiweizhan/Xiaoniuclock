package com.example.xiaoniuclock

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaoniuclock.manager.CustomAlarmManager
import com.example.xiaoniuclock.model.Alarm
import com.example.xiaoniuclock.repository.AlarmRepository
import com.example.xiaoniuclock.database.AlarmDatabase
import com.example.xiaoniuclock.database.AlarmDao
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var alarmManager: CustomAlarmManager
    private lateinit var groupsAdapter: AlarmGroupAdapter
    private val alarmGroups = mutableListOf<List<Alarm>>()
    private var currentGroupId = 0
    private var alarmDialog: Dialog? = null
    private var groupDialog: Dialog? = null
    private lateinit var repository: AlarmRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmManager = CustomAlarmManager.getInstance(this)
        repository = AlarmRepository(AlarmDatabase.getDatabase(this).alarmDao())
        
        setupGroupsRecyclerView()
        loadAlarms()

        // 设置添加闹钟组按钮
        findViewById<FloatingActionButton>(R.id.addGroupFab).setOnClickListener {
            showGroupDialog()
        }

        // 检查前台服务权限
        checkForegroundServicePermission()
    }

    private fun setupGroupsRecyclerView() {
        groupsAdapter = AlarmGroupAdapter(
            groups = alarmGroups,
            onGroupClick = { position -> showGroupDialog(position) },
            onGroupToggle = { position, isChecked -> 
                if (isChecked) {
                    alarmManager.enableAlarmGroup(alarmGroups[position].first().groupId)
                } else {
                    alarmManager.disableAlarmGroup(alarmGroups[position].first().groupId)
                }
            },
            onAlarmToggle = { alarm, isChecked ->
                if (isChecked) {
                    alarmManager.enableAlarm(alarm)
                } else {
                    alarmManager.disableAlarm(alarm)
                }
            }
        )

        findViewById<RecyclerView>(R.id.groupsRecyclerView)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = groupsAdapter
        }
    }

    private fun loadAlarms() {
        scope.launch {
            repository.allAlarms.collect { allAlarms ->
                val groups = allAlarms.groupBy { it.groupId }.values.toList()
                alarmGroups.clear()
                alarmGroups.addAll(groups)
                groupsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun checkForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE
                    )
                ) {
                    showPermissionExplanationDialog()
                } else {
                    requestForegroundServicePermission()
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要前台服务权限")
            .setMessage("为了确保闹钟能在后台正常运行，我们需要前台服务权限。\n\n" +
                    "请在接下来的对话框中授予权限。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                requestForegroundServicePermission()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "没有前台服务权限，闹钟可能无法正常工作", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestForegroundServicePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.FOREGROUND_SERVICE),
            1001
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "已获得前台服务权限", Toast.LENGTH_SHORT).show()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE
                    )
                ) {
                    // 用户选择了"不再询问"，引导用户去设置页面开启权限
                    showSettingsDialog()
                } else {
                    Toast.makeText(this, "没有前台服务权限，闹钟可能无法正常工作", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("前台服务权限已被禁用，这可能导致闹钟无法正常工作。\n\n" +
                    "请前往设置页面手动开启权限。")
            .setPositiveButton("去设置") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "没有前台服务权限，闹钟可能无法正常工作", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGroupDialog(editPosition: Int = -1) {
        val tempAlarms = mutableListOf<Alarm>()
        if (editPosition >= 0) {
            tempAlarms.addAll(alarmGroups[editPosition])
        }
        
        var alarmAdapter: AlarmAdapter? = null

        groupDialog?.dismiss()
        groupDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.alarm_group_dialog)
            
            val groupNameInput = findViewById<TextInputEditText>(R.id.groupNameInput)
            if (editPosition >= 0 && tempAlarms.isNotEmpty()) {
                groupNameInput?.setText(tempAlarms[0].groupName)
            }
            
            val timePicker = findViewById<TimePicker>(R.id.timePicker)?.apply {
                setIs24HourView(true)
            }
            
            val recyclerView = findViewById<RecyclerView>(R.id.alarmsRecyclerView)
            alarmAdapter = AlarmAdapter(tempAlarms)
            recyclerView?.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = alarmAdapter
            }

            findViewById<Button>(R.id.addAlarmButton)?.setOnClickListener {
                timePicker?.let {
                    val groupName = groupNameInput?.text?.toString() ?: ""
                    if (groupName.isBlank()) {
                        Toast.makeText(context, "请输入闹钟组名称", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    val alarm = Alarm(
                        id = tempAlarms.size + (currentGroupId * 100),
                        hour = it.hour,
                        minute = it.minute,
                        groupId = if (editPosition >= 0) tempAlarms[0].groupId else currentGroupId,
                        orderInGroup = tempAlarms.size,
                        groupName = groupName
                    )
                    tempAlarms.add(alarm)
                    alarmAdapter?.notifyItemInserted(tempAlarms.size - 1)
                }
            }

            findViewById<Button>(R.id.saveGroupButton)?.setOnClickListener {
                val groupName = groupNameInput?.text?.toString() ?: ""
                if (groupName.isBlank()) {
                    Toast.makeText(context, "请输入闹钟组名称", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (tempAlarms.isNotEmpty()) {
                    val sortedAlarms = tempAlarms.sortedBy { it.getTimeInMinutes() }
                    alarmManager.addAlarmGroup(sortedAlarms)
                    
                    if (editPosition >= 0) {
                        alarmGroups[editPosition] = sortedAlarms
                        groupsAdapter.notifyItemChanged(editPosition)
                    } else {
                        alarmGroups.add(sortedAlarms)
                        groupsAdapter.notifyItemInserted(alarmGroups.size - 1)
                        currentGroupId++
                    }
                    
                    dismiss()
                    Toast.makeText(this@MainActivity, "闹钟组已保存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "请先添加闹钟", Toast.LENGTH_SHORT).show()
                }
            }

            show()
        }
    }

    private fun showAlarmDialog(alarmId: Int, groupId: Int) {
        alarmDialog?.dismiss()
        alarmDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.alarm_dialog)
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                           WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                           WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                           WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

            findViewById<Button>(R.id.dismissButton)?.setOnClickListener {
                alarmManager.onAlarmDismissed(groupId, alarmId)
                dismiss()
                finish()
            }

            findViewById<Button>(R.id.snoozeButton)?.setOnClickListener {
                alarmManager.onAlarmSnoozed(groupId, alarmId)
                dismiss()
                finish()
            }

            setCancelable(false)
            show()
        }
    }

    private fun toggleAlarmGroup(position: Int, isChecked: Boolean) {
        val group = alarmGroups[position]
        if (group.isNotEmpty()) {
            val groupId = group[0].groupId
            if (isChecked) {
                alarmManager.enableAlarmGroup(groupId)
            } else {
                alarmManager.disableAlarmGroup(groupId)
            }
            findViewById<RecyclerView>(R.id.groupsRecyclerView).post {
                groupsAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun toggleSingleAlarm(alarm: Alarm, isChecked: Boolean) {
        if (isChecked) {
            alarmManager.enableAlarm(alarm)
        } else {
            alarmManager.disableAlarm(alarm)
        }
        val groupPosition = alarmGroups.indexOfFirst { it.any { a -> a.id == alarm.id } }
        if (groupPosition != -1) {
            findViewById<RecyclerView>(R.id.groupsRecyclerView).post {
                groupsAdapter.notifyItemChanged(groupPosition)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmDialog?.dismiss()
        groupDialog?.dismiss()
    }
}
