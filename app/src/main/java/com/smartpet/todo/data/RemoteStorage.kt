package com.smartpet.todo.data

import com.google.gson.Gson
import com.smartpet.todo.BuildConfig
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RemoteStorage {
    private val gson = Gson()

    suspend fun loadTasks(): List<Task> = request(mapOf("action" to "list")).tasks.map(::taskFromMap)

    suspend fun addTask(task: Task): List<Task> {
        request(mapOf("action" to "create", "task" to task.toPayload()))
        return loadTasks()
    }

    suspend fun updateTask(task: Task): List<Task> {
        request(mapOf("action" to "update", "task" to task.toPayload()))
        return loadTasks()
    }

    suspend fun deleteTask(taskId: String): List<Task> {
        request(mapOf("action" to "delete", "id" to taskId))
        return loadTasks()
    }

    suspend fun toggleTaskCompletion(taskId: String): List<Task> {
        request(mapOf("action" to "toggle", "id" to taskId))
        return loadTasks()
    }

    suspend fun upsertTask(task: Task): List<Task> {
        request(mapOf("action" to "upsert", "task" to task.toPayload()))
        return loadTasks()
    }

    private fun taskFromMap(map: Map<String, Any?>): Task {
        fun longOrNull(v: Any?): Long? = when (v) {
            null -> null
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }

        fun intOrNull(v: Any?): Int? = when (v) {
            null -> null
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }

        return Task(
            id = (map["id"] ?: "").toString(),
            title = (map["title"] ?: "").toString(),
            description = (map["description"] ?: "").toString(),
            dueDate = longOrNull(map["dueDate"]),
            isCompleted = (map["isCompleted"] as? Boolean) == true,
            createdAt = longOrNull(map["createdAt"]) ?: System.currentTimeMillis(),
            completedAt = longOrNull(map["completedAt"]),
            priority = runCatching {
                TaskPriority.valueOf(((map["priority"] ?: "NORMAL").toString()).uppercase())
            }.getOrDefault(TaskPriority.NORMAL),
            maxEnforcementLevel = intOrNull(map["maxEnforcementLevel"])?.coerceIn(1, 3) ?: 3,
            estimatedMinutes = intOrNull(map["estimatedMinutes"])
        )
    }

    private fun Task.toPayload(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "dueDate" to dueDate,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt,
        "completedAt" to completedAt,
        "priority" to priority.name,
        "maxEnforcementLevel" to maxEnforcementLevel,
        "estimatedMinutes" to estimatedMinutes
    )

    private data class ApiResponse(
        val ok: Boolean = false,
        val tasks: List<Map<String, Any?>> = emptyList()
    )

    private fun request(payload: Map<String, Any?>): ApiResponse {
        val baseUrl = BuildConfig.MOTHER_BASE_URL.trimEnd('/')
        val conn = (URL(baseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(gson.toJson(payload))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()

        if (code !in 200..299) {
            throw IllegalStateException("API error ($code): $responseText")
        }

        val parsed = gson.fromJson(responseText, ApiResponse::class.java)
        if (!parsed.ok) {
            throw IllegalStateException("API failed: $responseText")
        }

        return parsed
    }
}
