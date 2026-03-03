package com.smartpet.todo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartpet.todo.alarm.OverdueScheduler
import com.smartpet.todo.data.RemoteStorage
import com.smartpet.todo.data.Task
import com.smartpet.todo.data.TaskPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = RemoteStorage()
    private val appContext = application.applicationContext

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val tasks = storage.loadTasks()
            _uiState.value = _uiState.value.copy(tasks = tasks, isLoading = false)
            OverdueScheduler.scheduleAll(appContext, tasks)
        }
    }

    fun addTask(
        title: String,
        description: String,
        dueDate: Long?,
        priority: TaskPriority,
        maxEnforcementLevel: Int,
        estimatedMinutes: Int?
    ) {
        viewModelScope.launch {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "제목을 입력해주세요.")
                return@launch
            }

            val task = Task(
                title = normalizedTitle,
                description = description.trim(),
                dueDate = dueDate,
                priority = priority,
                maxEnforcementLevel = maxEnforcementLevel.coerceIn(1, 3),
                estimatedMinutes = estimatedMinutes?.takeIf { it > 0 }?.coerceAtMost(24 * 60)
            )
            val tasks = storage.addTask(task)
            _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            OverdueScheduler.scheduleAll(appContext, tasks)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val normalized = normalizeTaskOrNull(task) ?: run {
                _uiState.value = _uiState.value.copy(errorMessage = "제목을 입력해주세요.")
                return@launch
            }
            val tasks = storage.updateTask(normalized)
            _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            OverdueScheduler.scheduleAll(appContext, tasks)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            OverdueScheduler.cancel(appContext, taskId)
            val tasks = storage.deleteTask(taskId)
            _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            OverdueScheduler.scheduleAll(appContext, tasks)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val tasks = storage.toggleTaskCompletion(taskId)
            _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            OverdueScheduler.scheduleAll(appContext, tasks)
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch {
            val normalized = normalizeTaskOrNull(task) ?: run {
                _uiState.value = _uiState.value.copy(errorMessage = "제목을 입력해주세요.")
                return@launch
            }
            val tasks = storage.upsertTask(normalized)
            _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            OverdueScheduler.scheduleAll(appContext, tasks)
        }
    }

    private fun normalizeTaskOrNull(task: Task): Task? {
        val normalizedTitle = task.title.trim()
        if (normalizedTitle.isBlank()) return null
        return task.copy(
            title = normalizedTitle,
            description = task.description.trim(),
            maxEnforcementLevel = task.maxEnforcementLevel.coerceIn(1, 3),
            estimatedMinutes = task.estimatedMinutes?.takeIf { it > 0 }?.coerceAtMost(24 * 60)
        )
    }
}
