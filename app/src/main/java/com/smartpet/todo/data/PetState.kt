package com.smartpet.todo.data

/**
 * Represents the pet's mood state
 */
enum class PetMood {
    HAPPY,      // All tasks completed or no urgent tasks
    WORRIED,    // Tasks due within 1 hour
    CHASING,    // Tasks due within 30 minutes
    ANGRY       // Overdue tasks exist
}

/**
 * Pet state including mood and message
 */
data class PetState(
    val mood: PetMood = PetMood.HAPPY,
    val message: String = "오늘도 화이팅! 🐾",
    val emoji: String = "🐱"
) {
    companion object {
        fun fromInterventionLevel(level: Int): PetState {
            return when (level) {
                0 -> PetState(
                    mood = PetMood.HAPPY,
                    message = "오늘도 화이팅! 🐾",
                    emoji = "😊"
                )
                1 -> PetState(
                    mood = PetMood.WORRIED,
                    message = "슬슬 시작해볼까요? 🐱",
                    emoji = "😺"
                )
                2 -> PetState(
                    mood = PetMood.CHASING,
                    message = "⚠️ 서두르세요! 제가 쫓아갈 수도…",
                    emoji = "😼"
                )
                3 -> PetState(
                    mood = PetMood.ANGRY,
                    message = "🏃 로봇이 쫓아오고 있어요!! 빨리 완료하세요!",
                    emoji = "😾"
                )
                else -> PetState()
            }
        }
    }
}
