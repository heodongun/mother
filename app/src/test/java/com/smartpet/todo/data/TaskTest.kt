package com.smartpet.todo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {

    @Test
    fun interventionLevel_noDueDate_isZero() {
        val now = 1_000_000L
        val task = Task(title = "t", dueDate = null)
        assertEquals(0, task.getInterventionLevel(now))
    }

    @Test
    fun interventionLevel_dueInFuture_isBasedOnThresholds() {
        val now = 1_000_000L
        val dueIn90Min = now + 90 * 60 * 1000L
        val dueIn59Min = now + 59 * 60 * 1000L
        val dueIn20Min = now + 20 * 60 * 1000L

        assertEquals(0, Task(title = "t1", dueDate = dueIn90Min).getInterventionLevel(now))
        assertEquals(1, Task(title = "t2", dueDate = dueIn59Min).getInterventionLevel(now))
        assertEquals(2, Task(title = "t3", dueDate = dueIn20Min).getInterventionLevel(now))
    }

    @Test
    fun interventionLevel_overdue_isThree() {
        val now = 1_000_000L
        val due = now - 1L
        assertEquals(3, Task(title = "t", dueDate = due).getInterventionLevel(now))
    }

    @Test
    fun interventionLevel_isCappedByMaxEnforcementLevel() {
        val now = 1_000_000L
        val due = now - 1L // overdue -> raw 3

        assertEquals(1, Task(title = "t1", dueDate = due, maxEnforcementLevel = 1).getInterventionLevel(now))
        assertEquals(2, Task(title = "t2", dueDate = due, maxEnforcementLevel = 2).getInterventionLevel(now))
        assertEquals(3, Task(title = "t3", dueDate = due, maxEnforcementLevel = 3).getInterventionLevel(now))
    }

    @Test
    fun isOverdue_works() {
        val now = 1_000_000L
        assertTrue(Task(title = "t1", dueDate = now - 1L).isOverdue(now))
        assertFalse(Task(title = "t2", dueDate = now + 1L).isOverdue(now))
        assertFalse(Task(title = "t3", dueDate = now - 1L, isCompleted = true).isOverdue(now))
    }
}

