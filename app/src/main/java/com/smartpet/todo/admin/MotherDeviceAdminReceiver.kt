package com.smartpet.todo.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MotherDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "마더 기기 관리자 권한이 활성화됐어요.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "마더 기기 관리자 권한이 해제됐어요.", Toast.LENGTH_SHORT).show()
    }
}
