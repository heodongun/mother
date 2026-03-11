package com.smartpet.todo.workflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutonomousOrganizationOrchestratorTest {

    private val orchestrator = AutonomousOrganizationOrchestrator()

    @Test
    fun buildExecutionPlan_createsOrderedPipelineWithDependencies() {
        val goal = StrategicGoal(
            id = "heo-64",
            title = "AI CEO Workflow",
            description = "Build autonomous planning and delegation.",
            successMetrics = listOf("PR automation", "hands-off delegation")
        )

        val tasks = orchestrator.buildExecutionPlan(goal, agents = emptyList())

        assertEquals(5, tasks.size)
        assertEquals(WorkflowPhase.PLANNING, tasks[0].phase)
        assertEquals(listOf(tasks[0].id), tasks[1].dependencies)
        assertEquals(listOf(tasks[1].id), tasks[2].dependencies)
        assertEquals(listOf(tasks[2].id), tasks[3].dependencies)
        assertEquals(listOf(tasks[3].id), tasks[4].dependencies)
    }

    @Test
    fun buildExecutionPlan_assignsSpecialistsWhenAvailable() {
        val agents = listOf(
            AgentProfile("Alice-CEO", setOf("strategy", "planning")),
            AgentProfile("Bob-Linear", setOf("linear")),
            AgentProfile("Charlie-Build", setOf("implementation", "android")),
            AgentProfile("Dana-QA", setOf("testing")),
            AgentProfile("Evan-PR", setOf("paperclip", "github"))
        )

        val tasks = orchestrator.buildExecutionPlan(
            StrategicGoal("heo-64", "AI CEO Workflow", "desc"),
            agents
        )

        assertEquals("Alice-CEO", tasks[0].recommendedAgent)
        assertEquals("Bob-Linear", tasks[1].recommendedAgent)
        assertEquals("Charlie-Build", tasks[2].recommendedAgent)
        assertEquals("Dana-QA", tasks[3].recommendedAgent)
        assertEquals("Evan-PR", tasks[4].recommendedAgent)
    }

    @Test
    fun buildPrDraft_includesCompletedTasksAndMetrics() {
        val goal = StrategicGoal(
            id = "heo-64",
            title = "AI CEO Workflow",
            description = "Autonomous org execution",
            successMetrics = listOf("Quality gate passed")
        )

        val tasks = orchestrator.buildExecutionPlan(goal, agents = emptyList())
        val draft = orchestrator.buildPrDraft(goal, tasks.take(2))

        assertTrue(draft.title.contains("AI CEO Workflow"))
        assertTrue(draft.body.contains("[x] CEO Goal Decomposition"))
        assertTrue(draft.body.contains("Quality gate passed"))
        assertEquals(listOf("ceo-agent", "platform-lead"), draft.reviewers)
    }
}
