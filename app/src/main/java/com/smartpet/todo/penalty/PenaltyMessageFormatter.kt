package com.smartpet.todo.penalty

import com.smartpet.todo.data.Task

object PenaltyMessageFormatter {
    fun formatKakaoMessage(settings: PenaltySettings, task: Task): String {
        val partnerName = settings.target.partnerName.ifBlank { "책임 파트너" }
        return settings.kakaoMessageTemplate
            .replace("{partnerName}", partnerName)
            .replace("{taskTitle}", task.title.trim())
            .replace("{taskDescription}", task.description.trim())
            .trim()
    }
}
