package com.smartpet.todo.penalty

import com.smartpet.todo.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class PenaltyMessageFormatterTest {

    @Test
    fun formatKakaoMessage_replacesSupportedPlaceholders() {
        val settings = PenaltySettings(
            target = PenaltyTarget(partnerName = "민수"),
            kakaoMessageTemplate = "[마더] {partnerName}: {taskTitle} / {taskDescription}"
        )
        val task = Task(title = "운동", description = "러닝 30분")

        val formatted = PenaltyMessageFormatter.formatKakaoMessage(settings, task)

        assertEquals("[마더] 민수: 운동 / 러닝 30분", formatted)
    }
}
