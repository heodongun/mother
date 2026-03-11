package com.smartpet.todo.workflow

data class StrategicGoal(
    val id: String,
    val title: String,
    val description: String,
    val successMetrics: List<String> = emptyList()
)

data class AgentProfile(
    val name: String,
    val specialties: Set<String>
)

enum class WorkflowPhase {
    PLANNING,
    EXECUTION,
    VALIDATION,
    INTEGRATION
}

data class WorkflowTaskDraft(
    val id: String,
    val title: String,
    val description: String,
    val phase: WorkflowPhase,
    val recommendedAgent: String,
    val dependencies: List<String> = emptyList(),
    val labels: Set<String> = emptySet()
)

data class PullRequestDraft(
    val title: String,
    val body: String,
    val reviewers: List<String>
)

class AutonomousOrganizationOrchestrator {

    fun buildExecutionPlan(goal: StrategicGoal, agents: List<AgentProfile>): List<WorkflowTaskDraft> {
        val planner = pickAgent(agents, setOf("strategy", "planning"), "CEO-Agent")
        val linearOperator = pickAgent(agents, setOf("linear", "project-management"), "Linear-Operator-Agent")
        val implementation = pickAgent(agents, setOf("android", "backend", "implementation"), "Builder-Agent")
        val qa = pickAgent(agents, setOf("qa", "testing", "verification"), "QA-Agent")
        val integration = pickAgent(agents, setOf("github", "paperclip", "integration"), "Paperclip-Agent")

        val goalLabel = goal.title.lowercase().replace(" ", "-")

        val planningTask = WorkflowTaskDraft(
            id = "${goal.id}-plan",
            title = "CEO Goal Decomposition: ${goal.title}",
            description = "Break the business goal into executable issues and define acceptance criteria.",
            phase = WorkflowPhase.PLANNING,
            recommendedAgent = planner,
            labels = setOf("ceo-agent", "goal-decomposition", goalLabel)
        )

        val delegationTask = WorkflowTaskDraft(
            id = "${goal.id}-delegate",
            title = "Linear Delegation Pipeline Setup",
            description = "Create and assign Linear issues to specialized agents following organization workflow.",
            phase = WorkflowPhase.PLANNING,
            recommendedAgent = linearOperator,
            dependencies = listOf(planningTask.id),
            labels = setOf("linear", "delegation", goalLabel)
        )

        val buildTask = WorkflowTaskDraft(
            id = "${goal.id}-execute",
            title = "Autonomous Workflow Implementation",
            description = "Implement execution logic for workers to progress tasks without manual intervention.",
            phase = WorkflowPhase.EXECUTION,
            recommendedAgent = implementation,
            dependencies = listOf(delegationTask.id),
            labels = setOf("implementation", "automation", goalLabel)
        )

        val validationTask = WorkflowTaskDraft(
            id = "${goal.id}-validate",
            title = "Runtime & Quality Validation",
            description = "Verify system behavior, run tests, and collect evidence that workflow is operating correctly.",
            phase = WorkflowPhase.VALIDATION,
            recommendedAgent = qa,
            dependencies = listOf(buildTask.id),
            labels = setOf("qa", "validation", goalLabel)
        )

        val integrationTask = WorkflowTaskDraft(
            id = "${goal.id}-integrate",
            title = "Paperclip PR & Review Automation",
            description = "Generate PR details from execution logs and trigger automated review request flow.",
            phase = WorkflowPhase.INTEGRATION,
            recommendedAgent = integration,
            dependencies = listOf(validationTask.id),
            labels = setOf("paperclip", "pr-automation", goalLabel)
        )

        return listOf(planningTask, delegationTask, buildTask, validationTask, integrationTask)
    }

    fun buildPrDraft(goal: StrategicGoal, completedTasks: List<WorkflowTaskDraft>): PullRequestDraft {
        val summary = completedTasks.joinToString(separator = "\n") {
            "- [x] ${it.title} (${it.phase})"
        }

        val metrics = if (goal.successMetrics.isEmpty()) {
            "- (no explicit success metrics were provided)"
        } else {
            goal.successMetrics.joinToString(separator = "\n") { "- $it" }
        }

        return PullRequestDraft(
            title = "feat: autonomous organization workflow for ${goal.title}",
            body = """
                ## Goal
                ${goal.description}

                ## Completed workflow
                $summary

                ## Success metrics
                $metrics

                ## Operational notes
                - CEO agent decomposes top-level goals into sub-issues.
                - Linear assignment keeps tasks flowing across specialist agents.
                - Paperclip automation prepares PR context and review handoff.
            """.trimIndent(),
            reviewers = listOf("ceo-agent", "platform-lead")
        )
    }

    private fun pickAgent(agents: List<AgentProfile>, requiredTags: Set<String>, fallback: String): String {
        return agents.firstOrNull { profile -> profile.specialties.any { it in requiredTags } }?.name ?: fallback
    }
}
