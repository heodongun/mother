package com.smartpet.todo.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * File-based storage for tasks using JSON
 * Uses internal app storage for data persistence
 */
class FileStorage(private val context: Context) {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val tasksFile: File
        get() = File(context.filesDir, TASKS_FILENAME)

    private val backupFile: File
        get() = File(context.filesDir, "$TASKS_FILENAME.bak")

    private val mutex = Mutex()

    /**
     * Load all tasks from file
     */
    suspend fun loadTasks(): List<Task> = mutex.withLock {
        loadTasksInternalWithInfo().tasks
    }
    
    /**
     * Save all tasks to file
     */
    private suspend fun saveTasks(tasks: List<Task>, preserveCorruptMain: Boolean) = withContext(Dispatchers.IO) {
        try {
            val payload = TaskListPayload(tasks = tasks)
            val json = gson.toJson(payload)

            // Atomic-ish write: write to tmp then replace.
            val tmp = File(context.filesDir, "$TASKS_FILENAME.tmp")
            tmp.writeText(json)

            if (preserveCorruptMain && tasksFile.exists()) {
                // Best-effort preservation for a corrupted main file.
                val corrupt = File(context.filesDir, "$TASKS_FILENAME.corrupt-${System.currentTimeMillis()}")
                if (!tasksFile.renameTo(corrupt)) {
                    runCatching { tasksFile.copyTo(corrupt, overwrite = true) }
                    runCatching { tasksFile.delete() }
                }
            }

            // Prefer java.nio move for replace/atomic behavior when supported.
            val moved = runCatching {
                try {
                    Files.move(
                        tmp.toPath(),
                        tasksFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: Exception) {
                    Files.move(
                        tmp.toPath(),
                        tasksFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }.isSuccess

            if (!moved) {
                tmp.copyTo(tasksFile, overwrite = true)
                tmp.delete()
            }

            // Keep a last-known-good backup to recover from unexpected corruption.
            runCatching { tasksFile.copyTo(backupFile, overwrite = true) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Add a new task
     */
    suspend fun addTask(task: Task): List<Task> = mutateTasks { tasks ->
        tasks.add(task)
    }
    
    /**
     * Update an existing task
     */
    suspend fun updateTask(task: Task): List<Task> = mutateTasks { tasks ->
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
        }
    }
    
    /**
     * Delete a task by ID
     */
    suspend fun deleteTask(taskId: String): List<Task> = mutateTasks { tasks ->
        tasks.removeAll { it.id == taskId }
    }
    
    /**
     * Toggle task completion status
     */
    suspend fun toggleTaskCompletion(taskId: String): List<Task> = mutateTasks { tasks ->
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = tasks[index]
            tasks[index] = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
            )
        }
    }

    /**
     * Upsert a task by ID (used for undo/restore)
     */
    suspend fun upsertTask(task: Task): List<Task> = mutateTasks { tasks ->
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) {
            tasks.add(task)
        } else {
            tasks[index] = task
        }
    }

    private suspend fun mutateTasks(mutator: (MutableList<Task>) -> Unit): List<Task> = mutex.withLock {
        val load = loadTasksInternalWithInfo()
        val tasks = load.tasks.toMutableList()
        mutator(tasks)
        saveTasks(tasks, preserveCorruptMain = load.mainHadParseError)
        tasks.toList()
    }

    private data class LoadInternalResult(
        val tasks: List<Task>,
        val mainHadParseError: Boolean
    )

    private suspend fun loadTasksInternalWithInfo(): LoadInternalResult {
        // Callers must already hold mutex.
        return withContext(Dispatchers.IO) {
            val mainAttempt = readTasksFile(tasksFile)
            when (mainAttempt) {
                is ReadAttempt.Success -> LoadInternalResult(mainAttempt.tasks, mainHadParseError = false)
                ReadAttempt.Missing, ReadAttempt.Blank -> {
                    // If the main file is missing/blank but a backup exists, prefer the backup.
                    val backupAttempt = readTasksFile(backupFile)
                    val tasks = (backupAttempt as? ReadAttempt.Success)?.tasks ?: emptyList()
                    LoadInternalResult(tasks, mainHadParseError = false)
                }
                is ReadAttempt.Failure -> {
                    // Try the backup if the main file is corrupted/unreadable.
                    val backupAttempt = readTasksFile(backupFile)
                    val tasks = (backupAttempt as? ReadAttempt.Success)?.tasks ?: emptyList()
                    LoadInternalResult(tasks, mainHadParseError = true)
                }
            }
        }
    }

    private sealed class ReadAttempt {
        data class Success(val tasks: List<Task>) : ReadAttempt()
        data class Failure(val error: Throwable) : ReadAttempt()
        object Missing : ReadAttempt()
        object Blank : ReadAttempt()
    }

    private fun readTasksFile(file: File): ReadAttempt {
        return try {
            if (!file.exists()) return ReadAttempt.Missing

            val json = file.readText()
            if (json.isBlank()) return ReadAttempt.Blank

            val tasks = decodeTasks(json)
            ReadAttempt.Success(tasks)
        } catch (t: Throwable) {
            ReadAttempt.Failure(t)
        }
    }

    private fun decodeTasks(json: String): List<Task> {
        val trimmed = json.trimStart()
        val dtos: List<TaskDto> = if (trimmed.startsWith("[")) {
            // Backward compatibility: file may contain a raw JSON array.
            (gson.fromJson(trimmed, Array<TaskDto>::class.java) ?: emptyArray()).toList()
        } else {
            val wrapper = gson.fromJson(trimmed, TaskListDto::class.java)
            wrapper?.tasks.orEmpty()
        }

        return dtos.mapNotNull { it.toTaskOrNull() }
    }
    
    companion object {
        private const val TASKS_FILENAME = "tasks.json"
    }
}

private data class TaskListDto(
    val tasks: List<TaskDto>? = null
)

private data class TaskListPayload(
    val tasks: List<Task> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

private data class TaskDto(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val dueDate: Long? = null,
    val isCompleted: Boolean? = null,
    val createdAt: Long? = null,
    val completedAt: Long? = null,
    val priority: TaskPriority? = null,
    val maxEnforcementLevel: Int? = null,
    val estimatedMinutes: Int? = null
) {
    fun toTaskOrNull(nowMillis: Long = System.currentTimeMillis()): Task? {
        val safeTitle = title?.trim().orEmpty()
        if (safeTitle.isBlank()) return null

        val safeMaxLevel = (maxEnforcementLevel ?: 3).coerceIn(1, 3)
        val safeEstimatedMinutes = estimatedMinutes?.takeIf { it > 0 }?.coerceAtMost(24 * 60)

        return Task(
            id = id ?: UUID.randomUUID().toString(),
            title = safeTitle,
            description = description ?: "",
            dueDate = dueDate,
            isCompleted = isCompleted ?: false,
            createdAt = createdAt ?: nowMillis,
            completedAt = completedAt,
            priority = priority ?: TaskPriority.NORMAL,
            maxEnforcementLevel = safeMaxLevel,
            estimatedMinutes = safeEstimatedMinutes
        )
    }
}
