package com.smartpet.todo.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartpet.todo.admin.LockTaskController
import com.smartpet.todo.kakao.KakaoNotificationAccess
import com.smartpet.todo.penalty.PenaltyRuntimeStatus
import com.smartpet.todo.penalty.PenaltySettings
import com.smartpet.todo.penalty.PenaltyTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenaltySettingsDialog(
    initialSettings: PenaltySettings,
    runtimeStatus: PenaltyRuntimeStatus,
    onDismiss: () -> Unit,
    onSave: (PenaltySettings) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lockTaskStatus = remember(runtimeStatus.isDeviceOwnerApp, runtimeStatus.isLockTaskPermitted) {
        LockTaskController.status(context)
    }

    var partnerName by remember { mutableStateOf(initialSettings.target.partnerName) }
    var phoneNumber by remember { mutableStateOf(initialSettings.target.phoneNumber) }
    var kakaoRoomName by remember { mutableStateOf(initialSettings.target.kakaoRoomName) }
    var kakaoTemplate by remember { mutableStateOf(initialSettings.kakaoMessageTemplate) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("벌칙 설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "연락형 벌칙은 동의한 책임 파트너에게만 보냅니다. 기기 잠금은 공식 lock task 설정이 끝난 경우에만 활성화됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = partnerName,
                    onValueChange = { partnerName = it },
                    label = { Text("책임 파트너 이름") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' || ch == ' ' } },
                    label = { Text("책임 파트너 전화번호") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kakaoRoomName,
                    onValueChange = { kakaoRoomName = it },
                    label = { Text("책임 파트너 카카오톡 방 이름") },
                    supportingText = { Text("최근에 알림을 한 번 받은 방이어야 바로 전송할 수 있어요.") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kakaoTemplate,
                    onValueChange = { kakaoTemplate = it },
                    label = { Text("카카오 벌칙 메시지 템플릿") },
                    supportingText = { Text("{partnerName}, {taskTitle}, {taskDescription} 플레이스홀더를 쓸 수 있어요.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Kakao 상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = runtimeStatus.notificationListenerEnabled,
                        onClick = { },
                        label = { Text(if (runtimeStatus.notificationListenerEnabled) "알림 접근 허용" else "알림 접근 필요") },
                        enabled = false
                    )
                    FilterChip(
                        selected = runtimeStatus.activeKakaoSessions.isNotEmpty(),
                        onClick = { },
                        label = { Text(if (runtimeStatus.activeKakaoSessions.isNotEmpty()) "실시간 세션 있음" else "실시간 세션 없음") },
                        enabled = false
                    )
                }
                if (runtimeStatus.cachedKakaoRooms.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            "캐시된 방: ${runtimeStatus.cachedKakaoRooms.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "아직 캐시된 카카오톡 방이 없어요. 책임 파트너 방 알림을 한 번 받아야 세션이 잡혀요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(KakaoNotificationAccess.settingsIntent())
                    }) {
                        Text("알림 접근 열기")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("기기 잠금 상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = lockTaskStatus.isDeviceOwnerApp,
                        onClick = { },
                        enabled = false,
                        label = { Text(if (lockTaskStatus.isDeviceOwnerApp) "기기 소유자 연결됨" else "기기 소유자 미설정") }
                    )
                    FilterChip(
                        selected = lockTaskStatus.isLockTaskPermitted,
                        onClick = { },
                        enabled = false,
                        label = { Text(if (lockTaskStatus.isLockTaskPermitted) "lock task 가능" else "lock task 불가") }
                    )
                }
                Text(
                    "lock task는 Android 공식 kiosk 모드입니다. 아래 adb 명령은 초기화된 테스트 기기에서만 성공합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectionContainer {
                    Text(lockTaskStatus.adbProvisioningCommand, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(lockTaskStatus.adbProvisioningCommand))
                    Toast.makeText(context, "adb 명령을 복사했어요.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("adb 명령 복사")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    TextButton(onClick = {
                        onSave(
                            PenaltySettings(
                                target = PenaltyTarget(
                                    partnerName = partnerName.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    kakaoRoomName = kakaoRoomName.trim()
                                ),
                                kakaoMessageTemplate = kakaoTemplate.trim().ifBlank { PenaltySettings.DEFAULT_KAKAO_TEMPLATE },
                                callDialFallbackEnabled = true
                            )
                        )
                    }) {
                        Text("저장")
                    }
                }
            }
        }
    }
}
