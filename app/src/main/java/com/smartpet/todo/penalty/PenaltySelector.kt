package com.smartpet.todo.penalty

import com.smartpet.todo.data.Task
import java.util.Locale

object PenaltySelector {
    private val deviceRequiredKeywords = listOf(
        "코딩", "개발", "프로그래밍", "디버깅", "리팩토링", "문서", "조사", "공부", "강의", "writing",
        "coding", "development", "debug", "study", "research", "docs", "documentation"
    )
    private val photoVerifiableKeywords = listOf(
        "운동", "러닝", "산책", "설거지", "청소", "빨래", "정리", "요리", "식사", "물", "약", "스트레칭",
        "workout", "run", "walk", "clean", "laundry", "cook", "meal", "dish", "medicine"
    )
    private val offlinePhysicalKeywords = listOf(
        "숙제", "발표", "연습", "미팅", "회의", "준비", "외출", "장보기", "업무", "exercise",
        "meeting", "practice", "prep", "shopping", "errand"
    )

    fun classify(task: Task): TaskCategory {
        val haystack = listOf(task.title, task.description)
            .joinToString(" ")
            .lowercase(Locale.KOREA)

        return when {
            haystack.containsAny(deviceRequiredKeywords) -> TaskCategory.DEVICE_REQUIRED
            haystack.containsAny(photoVerifiableKeywords) -> TaskCategory.PHOTO_VERIFIABLE
            haystack.containsAny(offlinePhysicalKeywords) -> TaskCategory.OFFLINE_PHYSICAL
            else -> TaskCategory.GENERIC
        }
    }

    fun recommend(
        task: Task,
        settings: PenaltySettings,
        runtimeStatus: PenaltyRuntimeStatus
    ): PenaltyRecommendation {
        val category = classify(task)
        return when (category) {
            TaskCategory.DEVICE_REQUIRED -> PenaltyRecommendation(
                category = category,
                type = PenaltyType.PROOF_REQUIRED,
                reason = "디바이스가 필요한 목표라 잠금형 벌칙을 제외하고 인증형으로 고정했어요."
            )

            TaskCategory.PHOTO_VERIFIABLE -> PenaltyRecommendation(
                category = category,
                type = PenaltyType.PROOF_REQUIRED,
                reason = "사진으로 끝났는지 검증하기 쉬운 작업이라 인증형 벌칙이 가장 직접적이에요."
            )

            TaskCategory.OFFLINE_PHYSICAL -> pickExternalOrLock(
                category = category,
                settings = settings,
                runtimeStatus = runtimeStatus,
                fallbackReason = "오프라인 행동 작업이라 외부 개입 또는 잠금형 벌칙이 효과적이에요."
            )

            TaskCategory.GENERIC -> pickExternalOrLock(
                category = category,
                settings = settings,
                runtimeStatus = runtimeStatus,
                fallbackReason = "일반 작업은 책임 파트너 개입이 가능하면 먼저 쓰고, 아니면 인증형으로 내려가요."
            )
        }
    }

    fun resolve(
        task: Task,
        profile: PenaltyProfile?,
        settings: PenaltySettings,
        runtimeStatus: PenaltyRuntimeStatus
    ): PenaltyResolution {
        val safeProfile = profile ?: PenaltyProfile(taskId = task.id)
        val recommendation = recommend(task, settings, runtimeStatus)
        val selectedType = if (safeProfile.selectionMode == PenaltySelectionMode.MANUAL) {
            safeProfile.manualPenaltyType ?: recommendation.type
        } else {
            recommendation.type
        }
        val blockedReason = blockedReason(selectedType, settings, runtimeStatus)
        val effectiveRecommendation = if (blockedReason == null || selectedType == recommendation.type) {
            recommendation.copy(blockedReason = blockedReason)
        } else {
            recommendation.copy(
                blockedReason = blockedReason,
                reason = recommendation.reason
            )
        }
        return PenaltyResolution(
            profile = safeProfile,
            recommendation = effectiveRecommendation,
            selectedType = selectedType,
            usingManualOverride = safeProfile.selectionMode == PenaltySelectionMode.MANUAL && safeProfile.manualPenaltyType != null,
            blockedReason = blockedReason
        )
    }

    fun blockedReason(
        type: PenaltyType,
        settings: PenaltySettings,
        runtimeStatus: PenaltyRuntimeStatus
    ): String? {
        return when (type) {
            PenaltyType.PROOF_REQUIRED -> null
            PenaltyType.DEVICE_LOCK -> if (runtimeStatus.isLockTaskPermitted) null else "기기 소유자 잠금 설정이 아직 완료되지 않았어요."
            PenaltyType.ACCOUNTABILITY_CALL -> if (settings.target.phoneNumber.isBlank()) "책임 파트너 전화번호를 먼저 입력하세요." else null
            PenaltyType.ACCOUNTABILITY_KAKAO -> when {
                settings.target.kakaoRoomName.isBlank() -> "책임 파트너 카카오톡 방 이름을 먼저 입력하세요."
                !runtimeStatus.notificationListenerEnabled -> "카카오 세션 캐시를 위해 알림 접근 권한이 필요해요."
                else -> null
            }
        }
    }

    private fun pickExternalOrLock(
        category: TaskCategory,
        settings: PenaltySettings,
        runtimeStatus: PenaltyRuntimeStatus,
        fallbackReason: String
    ): PenaltyRecommendation {
        return when {
            runtimeStatus.isLockTaskPermitted -> PenaltyRecommendation(
                category = category,
                type = PenaltyType.DEVICE_LOCK,
                reason = "$fallbackReason 기기 고정 잠금이 준비돼 있어 우선 적용했어요."
            )

            settings.target.kakaoRoomName.isNotBlank() -> PenaltyRecommendation(
                category = category,
                type = PenaltyType.ACCOUNTABILITY_KAKAO,
                reason = "$fallbackReason 책임 파트너 카카오톡으로 바로 개입을 요청해요.",
                blockedReason = blockedReason(PenaltyType.ACCOUNTABILITY_KAKAO, settings, runtimeStatus)
            )

            settings.target.phoneNumber.isNotBlank() -> PenaltyRecommendation(
                category = category,
                type = PenaltyType.ACCOUNTABILITY_CALL,
                reason = "$fallbackReason 책임 파트너에게 즉시 전화를 연결해요.",
                blockedReason = blockedReason(PenaltyType.ACCOUNTABILITY_CALL, settings, runtimeStatus)
            )

            else -> PenaltyRecommendation(
                category = category,
                type = PenaltyType.PROOF_REQUIRED,
                reason = "$fallbackReason 외부 개입 대상이 아직 설정되지 않아 인증형으로 내려갔어요."
            )
        }
    }

    private fun String.containsAny(keywords: List<String>): Boolean = keywords.any { contains(it) }
}
