package com.smartpet.todo.workflow

import com.smartpet.todo.data.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutonomousWorkflowEngineTest {
    @Test
    fun decomposeGoal_returnsSequencedDrafts() {
        val now = 1_700_000_000_000L

        val drafts = AutonomousWorkflowEngine.decomposeGoal(
            goal = "Linear UI를 개편하고 Paperclip 엔진을 붙여 자동 배정한다.",
            nowMillis = now
        )

        assertTrue(drafts.isNotEmpty())
        assertEquals(TaskPriority.HIGH, drafts.first().priority)
        assertTrue(drafts.first().title.startsWith("1. "))
        assertEquals("CEO", drafts.first().assignee)
        assertTrue((drafts.first().dueDate ?: 0L) > now)
    }

    @Test
    fun decomposeGoal_blankGoal_returnsEmpty() {
        val drafts = AutonomousWorkflowEngine.decomposeGoal("   ")
        assertTrue(drafts.isEmpty())
    }
}
