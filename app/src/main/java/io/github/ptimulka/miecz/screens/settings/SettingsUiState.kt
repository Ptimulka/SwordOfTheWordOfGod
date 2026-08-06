package io.github.ptimulka.miecz.screens.settings

import io.github.ptimulka.miecz.repositories.BestTimeEntry

data class AchievementStats(
    val bestParts: BestTimeEntry? = null,
    val bestPairs: BestTimeEntry? = null,
    val levelStreak: Int = 0,
    val bestLevelStreak: Int = 0,
    val dayStreak: Int = 0,
    val bestDayStreak: Int = 0,
    val learnedVerses: Int = 0,
    val reviewedVerses: Int = 0,
    val aloudRepeats: Int = 0,
    val finishedLevels: Int = 0
)

enum class SettingsDialogState {
    NONE, CONFIRM_RESET, FINAL_CONFIRM_RESET
}

data class SettingsUiState(
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 20,
    val notificationMinute: Int = 0,
    val achievements: AchievementStats = AchievementStats(),
    val appVersion: String = "",
    val dialogState: SettingsDialogState = SettingsDialogState.NONE
)
