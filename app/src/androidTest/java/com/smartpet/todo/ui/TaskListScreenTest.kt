package com.smartpet.todo.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.smartpet.todo.data.Task
import com.smartpet.todo.data.TaskPriority
import com.smartpet.todo.viewmodel.TaskUiState
import org.junit.Rule
import org.junit.Test

class TaskListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_isShown() {
        composeRule.setContent {
            SmartPetTodoTheme {
                TaskListScreen(
                    uiState = TaskUiState(tasks = emptyList(), isLoading = false),
                    onAddTask = { _, _, _, _, _, _ -> },
                    onUpdateTask = { },
                    onToggleComplete = { },
                    onDeleteTask = { },
                    onRestoreTask = { },
                    onRefresh = { }
                )
            }
        }

        composeRule.onNodeWithText("할 일이 없어요!", substring = true).assertExists()
    }

    @Test
    fun addTask_fromFab_addsToList() {
        composeRule.setContent {
            val tasks = remember { mutableStateListOf<Task>() }
            val uiState by remember {
                derivedStateOf { TaskUiState(tasks = tasks.toList(), isLoading = false) }
            }

            SmartPetTodoTheme {
                TaskListScreen(
                    uiState = uiState,
                    onAddTask = { title, description, dueDate, priority, maxLevel, est ->
                        tasks.add(
                            Task(
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                priority = priority,
                                maxEnforcementLevel = maxLevel,
                                estimatedMinutes = est
                            )
                        )
                    },
                    onUpdateTask = { updated ->
                        val idx = tasks.indexOfFirst { it.id == updated.id }
                        if (idx != -1) tasks[idx] = updated
                    },
                    onToggleComplete = { id ->
                        val idx = tasks.indexOfFirst { it.id == id }
                        if (idx != -1) {
                            val t = tasks[idx]
                            tasks[idx] = t.copy(isCompleted = !t.isCompleted)
                        }
                    },
                    onDeleteTask = { id ->
                        tasks.removeAll { it.id == id }
                    },
                    onRestoreTask = { task ->
                        val idx = tasks.indexOfFirst { it.id == task.id }
                        if (idx == -1) tasks.add(task) else tasks[idx] = task
                    },
                    onRefresh = { }
                )
            }
        }

        composeRule.onNodeWithTag("add_fab").performClick()
        composeRule.onNodeWithTag("task_editor_dialog").assertExists()
        composeRule.onNodeWithTag("task_editor_title").performTextInput("운동")
        composeRule.onNodeWithTag("task_editor_save").performClick()

        composeRule.onNodeWithText("운동").assertExists()
    }

    @Test
    fun editTask_updatesTitle() {
        composeRule.setContent {
            val tasks = remember {
                mutableStateListOf(
                    Task(id = "idA", title = "A", priority = TaskPriority.NORMAL, maxEnforcementLevel = 3)
                )
            }
            val uiState by remember {
                derivedStateOf { TaskUiState(tasks = tasks.toList(), isLoading = false) }
            }

            SmartPetTodoTheme {
                TaskListScreen(
                    uiState = uiState,
                    onAddTask = { _, _, _, _, _, _ -> },
                    onUpdateTask = { updated ->
                        val idx = tasks.indexOfFirst { it.id == updated.id }
                        if (idx != -1) tasks[idx] = updated
                    },
                    onToggleComplete = { },
                    onDeleteTask = { },
                    onRestoreTask = { },
                    onRefresh = { }
                )
            }
        }

        // Open editor
        composeRule.onNodeWithTag("task_card_idA").performClick()
        composeRule.onNodeWithTag("task_editor_title").performTextClearance()
        composeRule.onNodeWithTag("task_editor_title").performTextInput("B")
        composeRule.onNodeWithTag("task_editor_save").performClick()

        composeRule.onNodeWithText("B").assertExists()
        composeRule.onNodeWithText("A").assertDoesNotExist()
    }
}
