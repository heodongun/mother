package com.smartpet.todo.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class RemoteStorageVerifyPhotoTest {

    @Test
    fun verifyPhoto_sendsMultipartWithTaskImageTaskId_andParsesReasonResponse() = runBlocking {
        val requestRef = AtomicReference<CapturedRequest?>()

        val serverSocket = ServerSocket(0)
        val serverThread = thread(start = true, name = "verify-photo-test-server") {
            serverSocket.use { server ->
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val input = socket.getInputStream()
                    val headers = readHeaders(input)
                    val contentLength = Regex("(?i)Content-Length:\\s*(\\d+)")
                        .find(headers)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt()
                        ?: 0
                    val body = ByteArray(contentLength)
                    DataInputStream(input).readFully(body)
                    requestRef.set(CapturedRequest(headers, body))

                    val responseBody = """
                        [
                          {
                            "verified": true,
                            "reason": "라면을 실제로 먹는 장면이 확인됩니다."
                          }
                        ]
                    """.trimIndent().toByteArray(StandardCharsets.UTF_8)

                    val output = socket.getOutputStream()
                    output.write("HTTP/1.1 200 OK\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                    output.write("Content-Type: application/json\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                    output.write("Content-Length: ${responseBody.size}\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                    output.write("Connection: close\r\n\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                    output.write(responseBody)
                    output.flush()
                }
            }
        }

        try {
            val baseUrl = "http://127.0.0.1:${serverSocket.localPort}"
            val storage = RemoteStorage(
                tasksBaseUrl = "$baseUrl/tasks",
                verifyUrl = "$baseUrl/verify-photo"
            )

            val result = storage.verifyPhoto(
                taskId = "task-123",
                task = "라면 먹기 점심으로 라면",
                image = RemoteStorage.UploadImage(
                    filename = "picked.jpg",
                    mimeType = "image/jpeg",
                    bytes = byteArrayOf(1, 2, 3, 4, 5)
                )
            )

            assertTrue(result.verified)
            assertEquals("라면을 실제로 먹는 장면이 확인됩니다.", result.message)

            serverThread.join(5000)
            val captured = requestRef.get()
            assertNotNull(captured)

            val headers = captured!!.headers
            assertTrue(headers.contains("POST /verify-photo HTTP/1.1"))
            assertTrue(headers.contains("Content-Type: multipart/form-data; boundary="))

            val body = String(captured.body, StandardCharsets.UTF_8)
            assertTrue(body.contains("name=\"task\""))
            assertTrue(body.contains("라면 먹기 점심으로 라면"))
            assertTrue(body.contains("name=\"taskId\""))
            assertTrue(body.contains("task-123"))
            assertTrue(body.contains("name=\"image\"; filename=\"picked.jpg\""))
            assertTrue(body.contains("Content-Type: image/jpeg"))
        } finally {
            runCatching { serverSocket.close() }
        }
    }

    private fun readHeaders(input: java.io.InputStream): String {
        val out = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) break
            out.write(b)
            val arr = out.toByteArray()
            val n = arr.size
            if (n >= 4 &&
                arr[n - 4] == '\r'.code.toByte() &&
                arr[n - 3] == '\n'.code.toByte() &&
                arr[n - 2] == '\r'.code.toByte() &&
                arr[n - 1] == '\n'.code.toByte()
            ) {
                break
            }
        }
        return out.toString(StandardCharsets.ISO_8859_1.name())
    }

    private data class CapturedRequest(
        val headers: String,
        val body: ByteArray
    )
}
