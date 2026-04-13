package com.smartpet.todo.penalty

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.smartpet.todo.admin.LockTaskController
import com.smartpet.todo.data.Task
import com.smartpet.todo.kakao.KakaoMessageSender
import com.smartpet.todo.kakao.KakaoNotificationAccess
import com.smartpet.todo.kakao.KakaoSessionManager

class PenaltyManager(private val context: Context) {
    private val store = PenaltyLocalStore(context.filesDir)
    private val kakaoMessageSender = KakaoMessageSender(context)

    fun loadSettings(): PenaltySettings = store.loadSettings()

    fun saveSettings(settings: PenaltySettings) {
        store.saveSettings(settings)
    }

    fun loadProfiles(): Map<String, PenaltyProfile> = store.loadProfiles()

    fun saveProfile(profile: PenaltyProfile) {
        store.saveProfile(profile)
    }

    fun clearTrigger(taskId: String) {
        store.clearTriggered(taskId)
    }

    fun runtimeStatus(): PenaltyRuntimeStatus {
        val lockTaskStatus = LockTaskController.status(context)
        return PenaltyRuntimeStatus(
            notificationListenerEnabled = KakaoNotificationAccess.isEnabled(context),
            cachedKakaoRooms = KakaoSessionManager.getRegisteredRooms(context).map { it.name },
            activeKakaoSessions = KakaoSessionManager.getRegisteredRooms(context)
                .filter { it.hasActiveSession }
                .map { it.name },
            isDeviceOwnerApp = lockTaskStatus.isDeviceOwnerApp,
            isLockTaskPermitted = lockTaskStatus.isLockTaskPermitted,
            canPlaceCalls = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun resolve(task: Task, profile: PenaltyProfile?): PenaltyResolution {
        return PenaltySelector.resolve(
            task = task,
            profile = profile,
            settings = loadSettings(),
            runtimeStatus = runtimeStatus()
        )
    }

    fun triggerIfNeeded(activity: Activity, task: Task, resolution: PenaltyResolution): PenaltyExecutionResult {
        val alreadyTriggered = store.loadTriggers()[task.id]
        if (alreadyTriggered?.penaltyType == resolution.selectedType) {
            return PenaltyExecutionResult(
                success = true,
                userMessage = "같은 벌칙은 이미 한 번 실행했어요.",
                triggeredType = resolution.selectedType
            )
        }

        val result = when (resolution.selectedType) {
            PenaltyType.PROOF_REQUIRED -> PenaltyExecutionResult(
                success = true,
                userMessage = "인증을 완료할 때까지 이 화면을 유지해요.",
                triggeredType = PenaltyType.PROOF_REQUIRED
            )

            PenaltyType.DEVICE_LOCK -> {
                if (LockTaskController.startLockTaskIfPermitted(activity)) {
                    PenaltyExecutionResult(true, "이 작업은 인증 전까지 앱 잠금이 유지됩니다.", PenaltyType.DEVICE_LOCK)
                } else {
                    PenaltyExecutionResult(
                        success = false,
                        userMessage = "앱 잠금을 시작하지 못했어요.",
                        triggeredType = PenaltyType.DEVICE_LOCK,
                        blockedReason = PenaltySelector.blockedReason(PenaltyType.DEVICE_LOCK, loadSettings(), runtimeStatus())
                    )
                }
            }

            PenaltyType.ACCOUNTABILITY_CALL -> triggerAccountabilityCall(activity)
            PenaltyType.ACCOUNTABILITY_KAKAO -> triggerAccountabilityKakao(task)
        }

        if (result.success) {
            store.markTriggered(PenaltyTriggerRecord(task.id, resolution.selectedType))
        }
        return result
    }

    fun stopLockTaskIfNeeded(activity: Activity, resolution: PenaltyResolution?) {
        if (resolution?.selectedType == PenaltyType.DEVICE_LOCK) {
            LockTaskController.stopLockTaskSafely(activity)
        }
    }

    private fun triggerAccountabilityCall(activity: Activity): PenaltyExecutionResult {
        val settings = loadSettings()
        val phoneNumber = settings.target.phoneNumber.filter { it.isDigit() || it == '+' }
        if (phoneNumber.isBlank()) {
            return PenaltyExecutionResult(
                success = false,
                userMessage = "전화 벌칙을 실행할 수 없어요.",
                triggeredType = PenaltyType.ACCOUNTABILITY_CALL,
                blockedReason = "책임 파트너 전화번호를 먼저 입력하세요."
            )
        }

        val callUri = Uri.parse("tel:$phoneNumber")
        val canCallDirectly = ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val callIntent = Intent(if (canCallDirectly) Intent.ACTION_CALL else Intent.ACTION_DIAL).apply {
            data = callUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            activity.startActivity(callIntent)
            val userMessage = if (canCallDirectly) {
                "책임 파트너에게 바로 전화 연결을 시작했어요."
            } else {
                "통화 권한이 없어 다이얼 화면으로 보냈어요. 직접 통화를 시작하세요."
            }
            PenaltyExecutionResult(true, userMessage, PenaltyType.ACCOUNTABILITY_CALL)
        } catch (_: ActivityNotFoundException) {
            PenaltyExecutionResult(
                success = false,
                userMessage = "전화 앱을 찾지 못했어요.",
                triggeredType = PenaltyType.ACCOUNTABILITY_CALL,
                blockedReason = "통화 앱이 없어 전화 벌칙을 실행할 수 없어요."
            )
        }
    }

    private fun triggerAccountabilityKakao(task: Task): PenaltyExecutionResult {
        val settings = loadSettings()
        val roomName = settings.target.kakaoRoomName.trim()
        if (roomName.isBlank()) {
            return PenaltyExecutionResult(
                success = false,
                userMessage = "카카오 벌칙을 실행할 수 없어요.",
                triggeredType = PenaltyType.ACCOUNTABILITY_KAKAO,
                blockedReason = "책임 파트너 카카오톡 방 이름을 먼저 입력하세요."
            )
        }

        val message = PenaltyMessageFormatter.formatKakaoMessage(settings, task)
        val sendResult = kakaoMessageSender.send(roomName, message)
        return PenaltyExecutionResult(
            success = sendResult.success,
            userMessage = sendResult.message,
            triggeredType = PenaltyType.ACCOUNTABILITY_KAKAO,
            blockedReason = sendResult.message.takeIf { !sendResult.success }
        )
    }
}
