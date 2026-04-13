package com.smartpet.todo.penalty

import com.smartpet.todo.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PenaltySelectorTest {

    @Test
    fun classify_deviceRequiredTask_returnsDeviceRequired() {
        val category = PenaltySelector.classify(Task(title = "코딩 완료하기", description = "리팩토링 마무리"))
        assertEquals(TaskCategory.DEVICE_REQUIRED, category)
    }

    @Test
    fun recommend_photoVerifiableTask_prefersProofRequired() {
        val recommendation = PenaltySelector.recommend(
            task = Task(title = "운동 인증", description = "러닝 30분"),
            settings = PenaltySettings(),
            runtimeStatus = PenaltyRuntimeStatus()
        )

        assertEquals(TaskCategory.PHOTO_VERIFIABLE, recommendation.category)
        assertEquals(PenaltyType.PROOF_REQUIRED, recommendation.type)
    }

    @Test
    fun recommend_genericTask_usesLockWhenReady() {
        val recommendation = PenaltySelector.recommend(
            task = Task(title = "회고 작성"),
            settings = PenaltySettings(target = PenaltyTarget(partnerName = "민수")),
            runtimeStatus = PenaltyRuntimeStatus(isLockTaskPermitted = true)
        )

        assertEquals(PenaltyType.DEVICE_LOCK, recommendation.type)
    }

    @Test
    fun resolve_manualOverride_keepsManualTypeAndWarnsWhenUnavailable() {
        val resolution = PenaltySelector.resolve(
            task = Task(title = "산책"),
            profile = PenaltyProfile(
                taskId = "task-1",
                selectionMode = PenaltySelectionMode.MANUAL,
                manualPenaltyType = PenaltyType.ACCOUNTABILITY_KAKAO
            ),
            settings = PenaltySettings(),
            runtimeStatus = PenaltyRuntimeStatus(notificationListenerEnabled = false)
        )

        assertEquals(PenaltyType.ACCOUNTABILITY_KAKAO, resolution.selectedType)
        assertTrue(resolution.usingManualOverride)
        assertEquals("책임 파트너 카카오톡 방 이름을 먼저 입력하세요.", resolution.blockedReason)
    }
}
