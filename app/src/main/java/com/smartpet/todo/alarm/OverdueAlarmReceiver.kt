package com.smartpet.todo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartpet.todo.ui.OverdueVerificationActivity

class OverdueAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val i = Intent(context, OverdueVerificationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("taskId", intent?.getStringExtra("taskId") ?: "")
            putExtra("taskTitle", intent?.getStringExtra("taskTitle") ?: "할 일")
        }
        context.startActivity(i)
    }
}
