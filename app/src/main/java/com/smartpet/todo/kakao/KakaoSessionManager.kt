package com.smartpet.todo.kakao

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject

object KakaoSessionManager {
    private const val PREFS_NAME = "MotherKakaoSessions"
    private const val KEY_ROOM_TIMES = "room_times"

    private val sessionMap = mutableMapOf<String, CachedSession>()
    private val roomLastSeen = mutableMapOf<String, Long>()
    private var roomsLoaded = false

    data class CachedSession(
        val action: NotificationCompat.Action,
        val packageName: String,
        val updatedAt: Long = System.currentTimeMillis()
    )

    data class RoomEntry(
        val name: String,
        val lastSeen: Long,
        val hasActiveSession: Boolean
    )

    fun bindSession(context: Context, room: String, notification: Notification, packageName: String) {
        val action = findReplyAction(notification) ?: return
        loadRooms(context)
        sessionMap[room] = CachedSession(action = action, packageName = packageName)
        roomLastSeen[room] = System.currentTimeMillis()
        persistRooms(context)
    }

    fun getSession(room: String): CachedSession? = sessionMap[room]

    fun getRegisteredRooms(context: Context): List<RoomEntry> {
        loadRooms(context)
        val names = (roomLastSeen.keys + sessionMap.keys).distinct().sortedBy { it.lowercase() }
        return names.map { room ->
            RoomEntry(
                name = room,
                lastSeen = roomLastSeen[room] ?: 0L,
                hasActiveSession = sessionMap.containsKey(room)
            )
        }
    }

    fun findReplyAction(notification: Notification): NotificationCompat.Action? {
        val wearableExtender = NotificationCompat.WearableExtender(notification)
        wearableExtender.actions.firstOrNull { !it.remoteInputs.isNullOrEmpty() }?.let { return it }
        for (index in 0 until NotificationCompat.getActionCount(notification)) {
            val action = NotificationCompat.getAction(notification, index)
            if (action != null && !action.remoteInputs.isNullOrEmpty()) return action
        }
        return null
    }

    private fun loadRooms(context: Context) {
        if (roomsLoaded) return
        roomsLoaded = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ROOM_TIMES, null) ?: return
        runCatching {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                roomLastSeen[key] = json.optLong(key, 0L)
            }
        }.onFailure {
            Log.e("MotherKakao", "방 캐시 로드 실패", it)
        }
    }

    private fun persistRooms(context: Context) {
        runCatching {
            val json = JSONObject()
            roomLastSeen.forEach { (room, time) -> json.put(room, time) }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ROOM_TIMES, json.toString())
                .apply()
        }.onFailure {
            Log.e("MotherKakao", "방 캐시 저장 실패", it)
        }
    }
}
