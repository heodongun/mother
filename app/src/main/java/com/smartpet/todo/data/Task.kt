package com.smartpet.todo.data

import java.util.UUID

enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH
}

/**
 * Task data model representing a to-do item
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val maxEnforcementLevel: Int = 3,
    val estimatedMinutes: Int? = null
) {
    /**
     * Calculate intervention level based on due date
     * 0: No urgency (or no due date)
     * 1: Due within 1 hour - gentle reminder
     * 2: Due within 30 minutes - warning
     * 3: Overdue - robot chasing!
     */
    fun getInterventionLevel(nowMillis: Long = System.currentTimeMillis()): Int {
        if (isCompleted || dueDate == null) return 0
        
        val timeUntilDue = dueDate - nowMillis
        
        val rawLevel = when {
            timeUntilDue < 0 -> 3                    // Overdue
            timeUntilDue < 30 * 60 * 1000 -> 2       // < 30 minutes
            timeUntilDue < 60 * 60 * 1000 -> 1       // < 1 hour
            else -> 0                                // Plenty of time
        }

        return rawLevel.coerceAtMost(maxEnforcementLevel.coerceIn(1, 3))
    }
    
    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean =
        dueDate != null && dueDate < nowMillis && !isCompleted
}
