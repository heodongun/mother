package com.smartpet.todo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.smartpet.todo.data.Task

object OverdueScheduler {
    fun scheduleAll(context: Context, tasks: List<Task>) {
        tasks
            .filter { !it.isCompleted && it.dueDate != null }
            .forEach { task ->
                runCatching { schedule(context, task) }
                    .onFailure { Log.e(TAG, "Failed to schedule task alarm: ${task.id}", it) }
            }
    }

    fun schedule(context: Context, task: Task) {
        val due = task.dueDate ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, task.id, task.title, task.description)
        val trigger = if (due < System.currentTimeMillis()) System.currentTimeMillis() + 1000 else due

        try {
            if (canUseExactAlarm(am)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (se: SecurityException) {
            // Android 12+ can reject exact alarms when permission/app-op is not granted.
            Log.w(TAG, "Exact alarm not permitted, falling back to inexact alarm for task=${task.id}", se)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, taskId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, taskId, "", ""))
    }

    private fun pendingIntent(
        context: Context,
        taskId: String,
        taskTitle: String,
        taskDescription: String
    ): PendingIntent {
        val intent = Intent(context, OverdueAlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
            putExtra("taskDescription", taskDescription)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canUseExactAlarm(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private const val TAG = "OverdueScheduler"
}
