package com.smartpet.todo.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

data class LockTaskStatus(
    val isDeviceOwnerApp: Boolean,
    val isLockTaskPermitted: Boolean,
    val adbProvisioningCommand: String
)

object LockTaskController {
    fun status(context: Context): LockTaskStatus {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        if (isDeviceOwner) {
            runCatching {
                dpm?.setLockTaskPackages(adminComponent(context), arrayOf(context.packageName))
            }
        }
        return LockTaskStatus(
            isDeviceOwnerApp = isDeviceOwner,
            isLockTaskPermitted = dpm?.isLockTaskPermitted(context.packageName) == true,
            adbProvisioningCommand = "adb shell dpm set-device-owner ${context.packageName}/${MotherDeviceAdminReceiver::class.java.name}"
        )
    }

    fun startLockTaskIfPermitted(activity: Activity): Boolean {
        return if (status(activity).isLockTaskPermitted) {
            runCatching { activity.startLockTask() }.isSuccess
        } else {
            false
        }
    }

    fun stopLockTaskSafely(activity: Activity) {
        runCatching { activity.stopLockTask() }
    }

    private fun adminComponent(context: Context): ComponentName =
        ComponentName(context, MotherDeviceAdminReceiver::class.java)
}
