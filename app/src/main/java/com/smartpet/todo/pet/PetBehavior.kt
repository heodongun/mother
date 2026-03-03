package com.smartpet.todo.pet

import com.smartpet.todo.data.PetState
import com.smartpet.todo.data.Task

/**
 * Pet behavior logic based on task status
 * Determines pet's mood and messages based on task urgency
 */
object PetBehavior {
    
    /**
     * Calculate the overall pet state based on all tasks
     * Uses the highest intervention level among all incomplete tasks
     */
    fun calculatePetState(tasks: List<Task>, nowMillis: Long = System.currentTimeMillis()): PetState {
        val incompleteTasks = tasks.filter { !it.isCompleted }
        
        if (incompleteTasks.isEmpty()) {
            return PetState.fromInterventionLevel(0)
        }
        
        // Find the highest intervention level
        val maxLevel = incompleteTasks.maxOfOrNull { it.getInterventionLevel(nowMillis) } ?: 0
        
        return PetState.fromInterventionLevel(maxLevel)
    }
    
    /**
     * Get a specific message for a task based on its urgency
     */
    fun getTaskMessage(task: Task, nowMillis: Long = System.currentTimeMillis()): String {
        return when (task.getInterventionLevel(nowMillis)) {
            0 -> "여유 있어요 ✨"
            1 -> "곧 마감이에요!"
            2 -> "급해요!! 🏃"
            3 -> "마감 지났어요!! 😱"
            else -> ""
        }
    }
    
    /**
     * Check if any task requires immediate attention
     */
    fun hasUrgentTasks(tasks: List<Task>, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return tasks.any { !it.isCompleted && it.getInterventionLevel(nowMillis) >= 2 }
    }
    
    /**
     * Get count of overdue tasks
     */
    fun getOverdueCount(tasks: List<Task>, nowMillis: Long = System.currentTimeMillis()): Int {
        return tasks.count { it.isOverdue(nowMillis) }
    }
}
