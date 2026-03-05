package com.smartpet.todo.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smartpet.todo.R
import com.smartpet.todo.data.RemoteStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLConnection

class OverdueVerificationActivity : ComponentActivity() {
    private var sirenJob: Job? = null
    private var warningNotificationJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var taskId: String = ""
    private var taskTitle: String = ""
    private var taskDescription: String = ""
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startWarningNotifications()
            } else {
                Toast.makeText(this, "알림 권한이 없어 추적 알림이 표시되지 않아요.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        taskId = intent.getStringExtra("taskId").orEmpty()
        taskTitle = intent.getStringExtra("taskTitle").orEmpty()
        taskDescription = intent.getStringExtra("taskDescription").orEmpty()
        val storage = RemoteStorage()

        ensureNotificationChannel()

        setContent {
            var loading by remember { mutableStateOf(false) }
            val infiniteTransition = rememberInfiniteTransition(label = "overdue-alert")
            val warningOverlayAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "warning-overlay"
            )
            val warningOffsetX by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 170, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "warning-shake"
            )

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
                                stopSiren()
                                stopWarningNotifications(cancelNotice = true)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF140607),
                                Color(0xFF050505)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFF1E1E).copy(alpha = warningOverlayAlpha))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "긴급 경고",
                        color = Color(0xFFFF6666),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.offset(x = warningOffsetX.dp)
                    )
                    Text(
                        text = "로봇이 쫓아오고 있어요",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 18.dp)
                            .offset(x = (-warningOffsetX).dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1F0D0E),
                        border = BorderStroke(2.dp, Color(0xFFFF5252))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "지금 인증해야 할 미션",
                                color = Color(0xFFFF9B9B),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = taskTitle.ifBlank { "제목 없음" },
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (taskDescription.isNotBlank()) {
                                Text(
                                    text = taskDescription,
                                    color = Color(0xFFFFDBDB),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (loading) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        Text(
                            text = "사진 분석 중... 잠시만 기다려주세요",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp),
                            textAlign = TextAlign.Center
                        )
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
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF3D3D),
                                contentColor = Color.White
                            )
                        ) {
                            Text("사진 선택해서 즉시 인증")
                        }
                    }

                    OutlinedButton(
                        onClick = { finish() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF8A8A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD5D5))
                    ) {
                        Text("잠시 후 다시 시도")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ensureNotificationPermission()
        startSiren()
        startWarningNotifications()
    }

    override fun onStop() {
        stopWarningNotifications(cancelNotice = true)
        stopSiren()
        super.onStop()
    }

    private fun startSiren() {
        if (sirenJob != null) return

        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        sirenJob = lifecycleScope.launch {
            while (isActive) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 280)
                delay(300)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_LOW_L, 220)
                delay(240)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 260)
                delay(300)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_LOW_L, 220)
                delay(200)
            }
        }
    }

    private fun stopSiren() {
        sirenJob?.cancel()
        sirenJob = null
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHASE_CHANNEL_ID,
            "추적 경고",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "마감 미인증 시 강한 추적 경고를 알립니다."
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            enableVibration(true)
            setSound(alarmSound, attrs)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startWarningNotifications() {
        if (warningNotificationJob != null) return
        if (!canPostNotifications()) return

        val manager = NotificationManagerCompat.from(this)
        val warnings = listOf(
            "왜 아직 안 해요? 인증하세요.",
            "지금 안 하면 더 가까이 옵니다.",
            "도망칠 수 없어요. 지금 인증해요.",
            "시간 초과. 당장 사진으로 증명하세요.",
            "로봇 추적 중: 인증 전까지 계속 경고합니다.",
            "왜 멈췄어요? 지금 바로 인증하세요.",
            "할 일 미완료 상태입니다. 즉시 행동하세요.",
            "마감 초과 경고: 지금 인증하지 않으면 계속 울립니다."
        )

        warningNotificationJob = lifecycleScope.launch {
            var index = 0
            while (isActive) {
                if (canPostNotifications()) {
                    val message = warnings[index % warnings.size]
                    manager.notify(
                        CHASE_NOTIFICATION_ID,
                        buildWarningNotification(message)
                    )
                    index++
                }
                delay(1_000L)
            }
        }
    }

    private fun stopWarningNotifications(cancelNotice: Boolean) {
        warningNotificationJob?.cancel()
        warningNotificationJob = null
        if (cancelNotice) {
            NotificationManagerCompat.from(this).cancel(CHASE_NOTIFICATION_ID)
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    private fun buildWarningNotification(message: String): android.app.Notification {
        val openIntent = Intent(this, OverdueVerificationActivity::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
            putExtra("taskDescription", taskDescription)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode(),
            openIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHASE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("로봇 추적 경고")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$message\n$taskTitle 인증 전까지 경고가 계속됩니다."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHASE_CHANNEL_ID = "smartpet_chase_alert_channel"
        private const val CHASE_NOTIFICATION_ID = 90021
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
