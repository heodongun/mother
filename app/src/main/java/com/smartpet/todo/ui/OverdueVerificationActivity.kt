package com.smartpet.todo.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
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
import java.io.ByteArrayOutputStream

class OverdueVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val taskId = intent.getStringExtra("taskId").orEmpty()
        val taskTitle = intent.getStringExtra("taskTitle").orEmpty()
        val storage = RemoteStorage()

        setContent {
            var loading by remember { mutableStateOf(false) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
                if (bitmap == null) return@rememberLauncherForActivityResult
                loading = true
                lifecycleScope.launch {
                    val b64 = withContext(Dispatchers.Default) { bitmap.toBase64Jpeg() }
                    runCatching { storage.verifyPhoto(taskId, b64) }
                        .onSuccess {
                            loading = false
                            Toast.makeText(this@OverdueVerificationActivity, it.message, Toast.LENGTH_SHORT).show()
                            if (it.verified) finish()
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
                    Button(onClick = { launcher.launch(null) }) { Text("사진으로 인증하기") }
                }
            }
        }
    }
}

private fun Bitmap.toBase64Jpeg(quality: Int = 80): String {
    val out = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
