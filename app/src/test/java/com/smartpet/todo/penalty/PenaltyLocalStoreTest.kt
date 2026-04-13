package com.smartpet.todo.penalty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PenaltyLocalStoreTest {

    @Test
    fun saveAndLoadSettings_roundTrips() {
        val root = createTempDir(prefix = "penalty-store-settings-")
        val store = PenaltyLocalStore(root)

        val settings = PenaltySettings(
            target = PenaltyTarget(partnerName = "책임", phoneNumber = "010-1234-5678", kakaoRoomName = "책임방"),
            kakaoMessageTemplate = "[마더] {taskTitle}"
        )
        store.saveSettings(settings)

        assertEquals(settings, store.loadSettings())
        root.deleteRecursively()
    }

    @Test
    fun saveProfileAndTrigger_roundTripsAndClears() {
        val root = createTempDir(prefix = "penalty-store-profile-")
        val store = PenaltyLocalStore(root)

        val profile = PenaltyProfile(taskId = "task-1", selectionMode = PenaltySelectionMode.MANUAL, manualPenaltyType = PenaltyType.DEVICE_LOCK)
        store.saveProfile(profile)
        store.markTriggered(PenaltyTriggerRecord(taskId = "task-1", penaltyType = PenaltyType.DEVICE_LOCK, triggeredAt = 42L))

        assertEquals(PenaltyType.DEVICE_LOCK, store.loadProfiles()["task-1"]?.manualPenaltyType)
        assertEquals(42L, store.loadTriggers()["task-1"]?.triggeredAt)

        store.clearTriggered("task-1")
        assertNull(store.loadTriggers()["task-1"])
        root.deleteRecursively()
    }
}
