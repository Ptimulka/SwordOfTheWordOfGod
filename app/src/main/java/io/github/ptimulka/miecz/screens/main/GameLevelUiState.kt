package io.github.ptimulka.miecz.screens.main

import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section

data class ProgressSummary(
    val currentSectionId: Int = 1,
    val shieldsCount: Int = 5,
    val dayStreak: Int = 0,
    val playedToday: Boolean = false,
    val timeToNextShieldMs: Long = 0,
    val unlockedSectionId: Int = -1,
    val showRepeatHint: Boolean = false,
    val availableGroupsCount: Int = 0
)

data class SectionState(
    val finishedLevels: Set<Int> = emptySet(),
    val retention: Int = 0,
    val isSiglaFinished: Boolean = false,
    val isVerseFinished: Boolean = false,
    val dailyRetentionMaxed: Boolean = false,
    val bestTimeParts: Long = -1L,
    val bestTimePairs: Long = -1L
) {
    val areSpecialChallengesFinished: Boolean get() = isSiglaFinished && isVerseFinished
}

data class GameLevelUiState(
    val sections: List<Section> = emptyList(),
    val sectionStates: Map<Int, SectionState> = emptyMap(),
    val riddlesOrder: List<List<RiddleType>> = emptyList(),
    val progress: ProgressSummary = ProgressSummary(),
    val isShieldInfoVisible: Boolean = false,
    val isLampInfoVisible: Boolean = false,
    val showChooseVerseGroups: Boolean = false,
    val selectedSectionForDialog: Section? = null,
    val allVerseGroups: List<io.github.ptimulka.miecz.data.VerseGroup> = emptyList(),
    val usedGroupIds: Set<Int> = emptySet(),
    val selectedGroupIds: List<Int> = emptyList()
) {
    val isShieldsEmpty: Boolean get() = progress.shieldsCount <= 0
}
