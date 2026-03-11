package com.smartpet.todo.workflow

import com.smartpet.todo.data.TaskPriority

private val ROLE_ROTATION = listOf("CEO", "CTO", "COO", "CPO", "Engineer")

data class WorkflowTaskDraft(
    val title: String,
    val description: String,
    val assignee: String,
    val priority: TaskPriority,
    val dueDate: Long?,
    val estimatedMinutes: Int,
    val maxEnforcementLevel: Int
)

object AutonomousWorkflowEngine {
    fun decomposeGoal(goal: String, nowMillis: Long = System.currentTimeMillis()): List<WorkflowTaskDraft> {
        val normalized = goal.trim()
        if (normalized.isBlank()) return emptyList()

        val chunks = normalized
            .split("\n", ".", "!", "?")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(normalized) }

        val scopedChunks = chunks.take(5)
        return scopedChunks.mapIndexed { index, chunk ->
            val assignee = ROLE_ROTATION[index % ROLE_ROTATION.size]
            val priority = when (index) {
                0 -> TaskPriority.HIGH
                1 -> TaskPriority.NORMAL
                else -> TaskPriority.LOW
            }
            val dueHours = (index + 1) * 8L
            val escalationHours = dueHours + 4L
            WorkflowTaskDraft(
                title = buildTitle(chunk, index),
                description = "[Goal] $normalized\n[Owner] $assignee\n[Escalation] ${escalationHours}h 이후 CEO 에스컬레이션",
                assignee = assignee,
                priority = priority,
                dueDate = nowMillis + dueHours * 60L * 60L * 1000L,
                estimatedMinutes = estimateMinutes(chunk),
                maxEnforcementLevel = if (priority == TaskPriority.HIGH) 3 else 2
            )
        }
    }

    private fun buildTitle(chunk: String, index: Int): String {
        val clean = chunk.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 36) return "${index + 1}. $clean"
        return "${index + 1}. ${clean.take(33)}..."
    }

    private fun estimateMinutes(text: String): Int {
        val tokens = text.split(Regex("\\s+")).size.coerceAtLeast(1)
        return (tokens * 10).coerceIn(20, 180)
    }
}
