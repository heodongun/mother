package com.smartpet.todo.penalty

import com.smartpet.todo.data.Task

enum class PenaltyType {
    PROOF_REQUIRED,
    DEVICE_LOCK,
    ACCOUNTABILITY_CALL,
    ACCOUNTABILITY_KAKAO
}

enum class PenaltySelectionMode {
    AUTO,
    MANUAL
}

enum class TaskCategory {
    DEVICE_REQUIRED,
    PHOTO_VERIFIABLE,
    OFFLINE_PHYSICAL,
    GENERIC
}

data class PenaltyTarget(
    val partnerName: String = "",
    val phoneNumber: String = "",
    val kakaoRoomName: String = ""
)

data class PenaltySettings(
    val target: PenaltyTarget = PenaltyTarget(),
    val kakaoMessageTemplate: String = DEFAULT_KAKAO_TEMPLATE,
    val callDialFallbackEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_KAKAO_TEMPLATE = "[마더] {partnerName}님, \"{taskTitle}\" 작업이 아직 완료되지 않았어요. 지금 확인 부탁드려요."
    }
}

data class PenaltyProfile(
    val taskId: String,
    val selectionMode: PenaltySelectionMode = PenaltySelectionMode.AUTO,
    val manualPenaltyType: PenaltyType? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class PenaltyRecommendation(
    val category: TaskCategory,
    val type: PenaltyType,
    val reason: String,
    val blockedReason: String? = null
) {
    val isReady: Boolean
        get() = blockedReason == null
}

data class PenaltyResolution(
    val profile: PenaltyProfile,
    val recommendation: PenaltyRecommendation,
    val selectedType: PenaltyType,
    val usingManualOverride: Boolean,
    val blockedReason: String? = recommendation.blockedReason
)

data class PenaltyDraft(
    val selectionMode: PenaltySelectionMode,
    val manualPenaltyType: PenaltyType?
)

data class PenaltyRuntimeStatus(
    val notificationListenerEnabled: Boolean = false,
    val cachedKakaoRooms: List<String> = emptyList(),
    val activeKakaoSessions: List<String> = emptyList(),
    val isDeviceOwnerApp: Boolean = false,
    val isLockTaskPermitted: Boolean = false,
    val canPlaceCalls: Boolean = false
)

data class PenaltyTriggerRecord(
    val taskId: String,
    val penaltyType: PenaltyType,
    val triggeredAt: Long = System.currentTimeMillis()
)

data class PenaltyExecutionResult(
    val success: Boolean,
    val userMessage: String,
    val triggeredType: PenaltyType,
    val blockedReason: String? = null
)

data class TaskPenaltyPresentation(
    val label: String,
    val detail: String,
    val warning: String? = null
)

fun PenaltyResolution.toPresentation(): TaskPenaltyPresentation {
    val label = when (selectedType) {
        PenaltyType.PROOF_REQUIRED -> "벌칙: 인증 고정"
        PenaltyType.DEVICE_LOCK -> "벌칙: 앱 고정 잠금"
        PenaltyType.ACCOUNTABILITY_CALL -> "벌칙: 책임 파트너 전화"
        PenaltyType.ACCOUNTABILITY_KAKAO -> "벌칙: 책임 파트너 카카오톡"
    }
    val detail = if (usingManualOverride) {
        "수동 지정 · ${recommendation.reason}"
    } else {
        "자동 선택 · ${recommendation.reason}"
    }
    return TaskPenaltyPresentation(label = label, detail = detail, warning = blockedReason)
}

fun Task.asPenaltyProfile(existing: PenaltyProfile?): PenaltyProfile {
    return existing ?: PenaltyProfile(taskId = id)
}
