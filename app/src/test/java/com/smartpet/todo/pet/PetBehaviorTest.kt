package com.smartpet.todo.pet

import com.smartpet.todo.data.PetMood
import com.smartpet.todo.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class PetBehaviorTest {

    @Test
    fun calculatePetState_noIncompleteTasks_isHappy() {
        val now = 1_000_000L
        val tasks = listOf(Task(title = "done", isCompleted = true))
        val state = PetBehavior.calculatePetState(tasks, now)
        assertEquals(PetMood.HAPPY, state.mood)
    }

    @Test
    fun calculatePetState_picksHighestInterventionLevel() {
        val now = 1_000_000L
        val dueSoon = Task(title = "soon", dueDate = now + 20 * 60 * 1000L) // level 2
        val overdue = Task(title = "overdue", dueDate = now - 1L) // level 3

        val state = PetBehavior.calculatePetState(listOf(dueSoon, overdue), now)
        assertEquals(PetMood.ANGRY, state.mood)
    }

    @Test
    fun getOverdueCount_countsOnlyOverdueIncompleteTasks() {
        val now = 1_000_000L
        val tasks = listOf(
            Task(title = "overdue", dueDate = now - 1L),
            Task(title = "future", dueDate = now + 1L),
            Task(title = "done", dueDate = now - 1L, isCompleted = true)
        )

        assertEquals(1, PetBehavior.getOverdueCount(tasks, now))
    }
}

