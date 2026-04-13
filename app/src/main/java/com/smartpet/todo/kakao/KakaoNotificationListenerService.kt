package com.smartpet.todo.kakao

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class KakaoNotificationListenerService : NotificationListenerService() {
    private val processedNotifications = mutableMapOf<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName != KAKAO_PACKAGE_NAME) return

        val lastSeen = processedNotifications[sbn.key] ?: 0L
        if (sbn.postTime <= lastSeen) return
        processedNotifications[sbn.key] = sbn.postTime
        if (processedNotifications.size > 200) {
            processedNotifications.entries.minByOrNull { it.value }?.key?.let { processedNotifications.remove(it) }
        }

        runCatching {
            val roomName = extractRoomName(sbn.notification) ?: return
            KakaoSessionManager.bindSession(this, roomName, sbn.notification, sbn.packageName)
        }.onFailure {
            Log.e("MotherKakao", "카카오 알림 처리 실패", it)
        }
    }

    private fun extractRoomName(notification: Notification): String? {
        val extras = notification.extras ?: return null
        val roomFromConversation = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        if (!roomFromConversation.isNullOrBlank()) return roomFromConversation

        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            val last = messages.last()
            if (last is Bundle) {
                val sender = last.getCharSequence("sender")?.toString()?.trim()
                if (!sender.isNullOrBlank()) return sender
            }
        }

        return extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
    }

    companion object {
        const val KAKAO_PACKAGE_NAME = "com.kakao.talk"
    }
}
