package com.smartpet.todo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartpet.todo.alarm.OverdueScheduler
import com.smartpet.todo.data.RemoteStorage
import com.smartpet.todo.data.Task
import com.smartpet.todo.data.TaskPriority
import com.smartpet.todo.penalty.PenaltyDraft
import com.smartpet.todo.penalty.PenaltyManager
import com.smartpet.todo.penalty.PenaltyProfile
import com.smartpet.todo.penalty.PenaltyRuntimeStatus
import com.smartpet.todo.penalty.PenaltySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val penaltySettings: PenaltySettings = PenaltySettings(),
    val penaltyProfiles: Map<String, PenaltyProfile> = emptyMap(),
    val penaltyRuntimeStatus: PenaltyRuntimeStatus = PenaltyRuntimeStatus()
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = RemoteStorage()
    private val appContext = application.applicationContext
    private val penaltyManager = PenaltyManager(appContext)

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val settings = penaltyManager.loadSettings()
            val profiles = penaltyManager.loadProfiles()
            val runtimeStatus = penaltyManager.runtimeStatus()
            runCatching { storage.loadTasks() }
                .onSuccess { tasks ->
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        isLoading = false,
                        errorMessage = null,
                        penaltySettings = settings,
                        penaltyProfiles = profiles,
                        penaltyRuntimeStatus = runtimeStatus
                    )
                    OverdueScheduler.scheduleAll(appContext, tasks)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        penaltySettings = settings,
                        penaltyProfiles = profiles,
                        penaltyRuntimeStatus = runtimeStatus
                    )
                    handleFailure(error)
                }
        }
    }

    fun savePenaltySettings(settings: PenaltySettings) {
        penaltyManager.saveSettings(settings)
        _uiState.value = _uiState.value.copy(
            penaltySettings = settings,
            penaltyRuntimeStatus = penaltyManager.runtimeStatus()
        )
    }

    fun addTask(
        title: String,
        description: String,
        dueDate: Long?,
        priority: TaskPriority,
        maxEnforcementLevel: Int,
        estimatedMinutes: Int?,
        penaltyDraft: PenaltyDraft
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
            runCatching { storage.addTask(task) }
                .onSuccess { tasks ->
                    penaltyManager.saveProfile(
                        PenaltyProfile(
                            taskId = task.id,
                            selectionMode = penaltyDraft.selectionMode,
                            manualPenaltyType = penaltyDraft.manualPenaltyType
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        errorMessage = null,
                        penaltyProfiles = penaltyManager.loadProfiles(),
                        penaltyRuntimeStatus = penaltyManager.runtimeStatus()
                    )
                    OverdueScheduler.scheduleAll(appContext, tasks)
                }
                .onFailure(::handleFailure)
        }
    }

    fun updateTask(task: Task, penaltyDraft: PenaltyDraft) {
        viewModelScope.launch {
            val normalized = normalizeTaskOrNull(task) ?: run {
                _uiState.value = _uiState.value.copy(errorMessage = "제목을 입력해주세요.")
                return@launch
            }
            runCatching { storage.updateTask(normalized) }
                .onSuccess { tasks ->
                    penaltyManager.saveProfile(
                        PenaltyProfile(
                            taskId = normalized.id,
                            selectionMode = penaltyDraft.selectionMode,
                            manualPenaltyType = penaltyDraft.manualPenaltyType
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        errorMessage = null,
                        penaltyProfiles = penaltyManager.loadProfiles(),
                        penaltyRuntimeStatus = penaltyManager.runtimeStatus()
                    )
                    OverdueScheduler.scheduleAll(appContext, tasks)
                }
                .onFailure(::handleFailure)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            OverdueScheduler.cancel(appContext, taskId)
            runCatching { storage.deleteTask(taskId) }
                .onSuccess { tasks ->
                    penaltyManager.clearTrigger(taskId)
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        errorMessage = null,
                        penaltyRuntimeStatus = penaltyManager.runtimeStatus()
                    )
                    OverdueScheduler.scheduleAll(appContext, tasks)
                }
                .onFailure(::handleFailure)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            runCatching { storage.toggleTaskCompletion(taskId) }
                .onSuccess { tasks ->
                    penaltyManager.clearTrigger(taskId)
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        errorMessage = null,
                        penaltyRuntimeStatus = penaltyManager.runtimeStatus()
                    )
                    OverdueScheduler.scheduleAll(appContext, tasks)
                }
                .onFailure(::handleFailure)
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch {
            val normalized = normalizeTaskOrNull(task) ?: run {
                _uiState.value = _uiState.value.copy(errorMessage = "제목을 입력해주세요.")
                return@launch
            }
            runCatching { storage.upsertTask(normalized) }
                .onSuccess { tasks ->
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        errorMessage = null,
                        penaltyRuntimeStatus = penaltyManager.runtimeStatus()
                    )
                    OverdueScheduler.scheduleAll(appContext, tasks)
                }
                .onFailure(::handleFailure)
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

    private fun handleFailure(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = throwable.message?.takeIf { it.isNotBlank() } ?: "네트워크 오류가 발생했어요."
        )
    }
}
