package com.smartpet.todo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.smartpet.todo.data.Task

object OverdueScheduler {
    fun scheduleAll(context: Context, tasks: List<Task>) {
        tasks.filter { !it.isCompleted && it.dueDate != null }.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, task: Task) {
        val due = task.dueDate ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, task.id, task.title)
        val trigger = if (due < System.currentTimeMillis()) System.currentTimeMillis() + 1000 else due
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
    }

    fun cancel(context: Context, taskId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, taskId, ""))
    }

    private fun pendingIntent(context: Context, taskId: String, taskTitle: String): PendingIntent {
        val intent = Intent(context, OverdueAlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
