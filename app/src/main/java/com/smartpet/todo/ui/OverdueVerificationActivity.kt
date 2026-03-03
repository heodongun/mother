package com.smartpet.todo.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.smartpet.todo.data.RemoteStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLConnection

class OverdueVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val taskId = intent.getStringExtra("taskId").orEmpty()
        val taskTitle = intent.getStringExtra("taskTitle").orEmpty()
        val taskDescription = intent.getStringExtra("taskDescription").orEmpty()
        val storage = RemoteStorage()

        setContent {
            var loading by remember { mutableStateOf(false) }
            val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri == null) return@rememberLauncherForActivityResult
                loading = true
                lifecycleScope.launch {
                    val uploadImage = withContext(Dispatchers.IO) { readUploadImage(uri) }
                    if (uploadImage == null) {
                        loading = false
                        Toast.makeText(this@OverdueVerificationActivity, "이미지 파일을 읽을 수 없어요.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val taskPayload = buildTaskPayload(taskTitle, taskDescription)
                    runCatching { storage.verifyPhoto(taskId, taskPayload, uploadImage) }
                        .onSuccess {
                            loading = false
                            Toast.makeText(this@OverdueVerificationActivity, it.message, Toast.LENGTH_SHORT).show()
                            if (it.verified) {
                                runCatching { storage.toggleTaskCompletion(taskId) }
                                finish()
                            }
                        }
                        .onFailure {
                            loading = false
                            Toast.makeText(this@OverdueVerificationActivity, "인증 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏰ 시간이 지났어요", style = MaterialTheme.typography.headlineMedium)
                Text(taskTitle, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            runCatching { pickerLauncher.launch(arrayOf("image/*")) }
                                .onFailure {
                                    Toast.makeText(
                                        this@OverdueVerificationActivity,
                                        "파일 선택 실패: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    ) { Text("사진 선택해서 인증하기") }
                }
            }
        }
    }
}

private fun OverdueVerificationActivity.readUploadImage(uri: Uri): RemoteStorage.UploadImage? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty()) return null

    val name = queryDisplayName(uri) ?: "upload.jpg"
    val mimeType = contentResolver.getType(uri)
        ?: URLConnection.guessContentTypeFromName(name)
        ?: "application/octet-stream"

    return RemoteStorage.UploadImage(
        filename = name,
        mimeType = mimeType,
        bytes = bytes
    )
}

private fun OverdueVerificationActivity.queryDisplayName(uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}

private fun buildTaskPayload(title: String, description: String): String {
    return listOf(title.trim(), description.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
