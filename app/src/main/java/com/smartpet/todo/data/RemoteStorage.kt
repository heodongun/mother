package com.smartpet.todo.data

import com.google.gson.Gson
import com.smartpet.todo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RemoteStorage(
    private val tasksBaseUrl: String = BuildConfig.MOTHER_TASKS_BASE_URL,
    private val verifyUrl: String = BuildConfig.MOTHER_VERIFY_URL
) {
    private val gson = Gson()

    suspend fun loadTasks(): List<Task> = request("$tasksBaseUrl/list", emptyMap()).tasks.map(::taskFromMap)

    suspend fun addTask(task: Task): List<Task> {
        request("$tasksBaseUrl/create", mapOf("task" to task.toPayload()))
        return loadTasks()
    }

    suspend fun updateTask(task: Task): List<Task> {
        request("$tasksBaseUrl/update", mapOf("task" to task.toPayload()))
        return loadTasks()
    }

    suspend fun deleteTask(taskId: String): List<Task> {
        request("$tasksBaseUrl/delete", mapOf("id" to taskId))
        return loadTasks()
    }

    suspend fun toggleTaskCompletion(taskId: String): List<Task> {
        request("$tasksBaseUrl/toggle", mapOf("id" to taskId))
        return loadTasks()
    }

    suspend fun upsertTask(task: Task): List<Task> {
        request("$tasksBaseUrl/upsert", mapOf("task" to task.toPayload()))
        return loadTasks()
    }

    suspend fun verifyPhoto(taskId: String, task: String, image: UploadImage): VerificationResult {
        val parsed = requestPhotoVerify(verifyUrl, taskId, task, image)
        return VerificationResult(
            verified = parsed.verified == true,
            message = parsed.message ?: parsed.reason ?: if (parsed.verified == true) "인증 성공" else "인증 실패"
        )
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
        fun boolFrom(v: Any?): Boolean = when (v) {
            is Boolean -> v
            is Number -> v.toInt() == 1
            is String -> {
                val normalized = v.trim()
                normalized == "1" || normalized.equals("true", ignoreCase = true)
            }
            else -> false
        }

        return Task(
            id = (map["id"] ?: "").toString(),
            title = (map["title"] ?: "").toString(),
            description = (map["description"] ?: "").toString(),
            dueDate = longOrNull(map["dueDate"]),
            isCompleted = boolFrom(map["isCompleted"]),
            createdAt = longOrNull(map["createdAt"]) ?: System.currentTimeMillis(),
            completedAt = longOrNull(map["completedAt"]),
            priority = runCatching { TaskPriority.valueOf(((map["priority"] ?: "NORMAL").toString()).uppercase()) }.getOrDefault(TaskPriority.NORMAL),
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

    data class UploadImage(
        val filename: String,
        val mimeType: String,
        val bytes: ByteArray
    )

    data class VerificationResult(val verified: Boolean, val message: String)

    private data class ApiResponse(
        val ok: Boolean = false,
        val tasks: List<Map<String, Any?>> = emptyList(),
        val verified: Boolean? = null,
        val message: String? = null,
        val reason: String? = null
    )

    private suspend fun request(url: String, payload: Map<String, Any?>): ApiResponse = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(gson.toJson(payload)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()

            if (code !in 200..299) throw IllegalStateException("API error ($code): $responseText")

            parseApiResponse(responseText, requireOk = true)
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun requestPhotoVerify(
        url: String,
        taskId: String,
        task: String,
        image: UploadImage
    ): ApiResponse =
        withContext(Dispatchers.IO) {
            val boundary = "----SmartPetTodoBoundary${System.currentTimeMillis()}"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            try {
                DataOutputStream(conn.outputStream).use { out ->
                    writeTextPart(out, boundary, "task", task)
                    writeTextPart(out, boundary, "taskId", taskId)
                    writeFilePart(
                        out = out,
                        boundary = boundary,
                        name = "image",
                        filename = image.filename,
                        contentType = image.mimeType,
                        bytes = image.bytes
                    )
                    out.writeBytes("--$boundary--\r\n")
                    out.flush()
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()

                if (code !in 200..299) throw IllegalStateException("API error ($code): $responseText")
                parseApiResponse(responseText, requireOk = false)
            } finally {
                conn.disconnect()
            }
        }

    private fun parseApiResponse(responseText: String, requireOk: Boolean): ApiResponse {
        if (responseText.isBlank()) throw IllegalStateException("API returned empty response")

        val root = runCatching { com.google.gson.JsonParser.parseString(responseText) }
            .getOrElse { throw IllegalStateException("Invalid API JSON: $responseText", it) }

        val parsed = when {
            root.isJsonArray -> {
                val array = root.asJsonArray
                if (array.size() == 0) throw IllegalStateException("API returned empty array response")
                gson.fromJson(array[0], ApiResponse::class.java)
            }

            root.isJsonObject -> gson.fromJson(root, ApiResponse::class.java)
            else -> throw IllegalStateException("Unexpected API response shape: $responseText")
        }

        if (requireOk && !parsed.ok) throw IllegalStateException("API failed: $responseText")
        if (!requireOk && !parsed.ok && parsed.verified == null && parsed.message == null && parsed.reason == null) {
            throw IllegalStateException("API failed: $responseText")
        }
        return parsed
    }

    private fun writeTextPart(out: DataOutputStream, boundary: String, name: String, value: String) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n")
        out.writeBytes("\r\n")
        out.write(value.toByteArray(Charsets.UTF_8))
        out.writeBytes("\r\n")
    }

    private fun writeFilePart(
        out: DataOutputStream,
        boundary: String,
        name: String,
        filename: String,
        contentType: String,
        bytes: ByteArray
    ) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n")
        out.writeBytes("Content-Type: $contentType\r\n")
        out.writeBytes("\r\n")
        out.write(bytes)
        out.writeBytes("\r\n")
    }
}
