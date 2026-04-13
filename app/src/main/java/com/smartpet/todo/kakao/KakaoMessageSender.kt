package com.smartpet.todo.kakao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput

class KakaoMessageSender(private val context: Context) {
    data class SendResult(
        val success: Boolean,
        val message: String
    )

    fun send(roomName: String, message: String): SendResult {
        val session = KakaoSessionManager.getSession(roomName)
            ?: return SendResult(false, "최근에 받은 카카오 알림 세션이 없어 바로 전송할 수 없어요.")
        val remoteInputs = session.action.remoteInputs
        if (remoteInputs.isNullOrEmpty()) {
            return SendResult(false, "이 채팅방 알림에는 답장 입력 필드가 없어요.")
        }

        return runCatching {
            val remoteInput = remoteInputs.first()
            val intent = Intent()
            val bundle = Bundle().apply {
                putCharSequence(remoteInput.resultKey, message)
            }
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
            session.action.actionIntent?.send(context, 0, intent) ?: error("pendingIntent missing")
            SendResult(true, "카카오톡 책임 메시지를 보냈어요.")
        }.getOrElse {
            SendResult(false, "카카오톡 전송 실패: ${it.message ?: "알 수 없는 오류"}")
        }
    }
}
