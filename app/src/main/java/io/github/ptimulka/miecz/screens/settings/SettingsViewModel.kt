package io.github.ptimulka.miecz.screens.settings

import androidx.lifecycle.ViewModel
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.helpers.NotificationScheduler
import io.github.ptimulka.miecz.repositories.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val progressRepo: ProgressRepository,
    private val mnemonicRepo: MnemonicRepository,
    private val sectionRepo: SectionRepository,
    private val groupsRepo: VersesGroupsRepository,
    private val notificationScheduler: NotificationScheduler,
    @param:Named("appVersion") private val appVersion: String
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = settingsRepo.isNotificationsEnabled(),
            notificationHour = settingsRepo.getNotificationHour(),
            notificationMinute = settingsRepo.getNotificationMinute(),
            appVersion = appVersion
        )
    )
    val state = _state.asStateFlow()

    init {
        refreshAchievements()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleNotifications -> {
                settingsRepo.setNotificationsEnabled(event.enabled)
                _state.update { it.copy(notificationsEnabled = event.enabled) }
                if (event.enabled) {
                    notificationScheduler.scheduleDailyNotification(_state.value.notificationHour, _state.value.notificationMinute)
                } else {
                    notificationScheduler.cancelDailyNotification()
                }
            }
            is SettingsEvent.UpdateNotificationTime -> {
                settingsRepo.setNotificationTime(event.hour, event.minute)
                _state.update { it.copy(notificationHour = event.hour, notificationMinute = event.minute) }
                notificationScheduler.scheduleDailyNotification(event.hour, event.minute)
            }
            SettingsEvent.RequestReset -> {
                _state.update { it.copy(dialogState = SettingsDialogState.CONFIRM_RESET) }
            }
            SettingsEvent.ConfirmReset -> {
                _state.update { it.copy(dialogState = SettingsDialogState.FINAL_CONFIRM_RESET) }
            }
            SettingsEvent.FinalConfirmReset -> {
                progressRepo.clearAllProgress()
                mnemonicRepo.clearAllPictures()
                _state.update { it.copy(dialogState = SettingsDialogState.NONE) }
                refreshAchievements()
            }
            SettingsEvent.CancelReset -> {
                _state.update { it.copy(dialogState = SettingsDialogState.NONE) }
            }
            SettingsEvent.OnResume -> {
                refreshAchievements()
            }
        }
    }

    private fun refreshAchievements() {
        val stats = AchievementStats(
            bestParts = progressRepo.getBestTimeOverall(RiddleType.CONNECT_PARTS.name, sectionRepo, groupsRepo),
            bestPairs = progressRepo.getBestTimeOverall(RiddleType.CONNECT_PAIRS.name, sectionRepo, groupsRepo),
            levelStreak = progressRepo.getLevelStreak(),
            bestLevelStreak = run {
                progressRepo.reconcileBestStreaks()
                progressRepo.getBestLevelStreak()
            },
            dayStreak = progressRepo.getCurrentDayStreak(),
            bestDayStreak = progressRepo.getBestDayStreak(),
            learnedVerses = countLearnedVerses(),
            reviewedVerses = progressRepo.getTotalReviewedVerses(),
            aloudRepeats = progressRepo.getTotalAloudRepeats(),
            finishedLevels = progressRepo.getFinishedLevelsCount()
        )
        _state.update { it.copy(achievements = stats) }
    }

    private fun countLearnedVerses(): Int {
        var total = 0
        sectionRepo.loadInitialSections().forEach { section ->
            if (progressRepo.areSpecialChallengesFinished(section.id)) {
                total += section.verses.size
            }
        }
        val groups = groupsRepo.loadVerseGroups()
        val customCount = progressRepo.getCustomSectionsCount()
        for (i in 1..customCount) {
            val sectionId = 5 + i - 1
            if (progressRepo.areSpecialChallengesFinished(sectionId)) {
                progressRepo.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                    val g1 = groups.find { it.id == id1 }
                    val g2 = groups.find { it.id == id2 }
                    if (g1 != null && g2 != null) {
                        total += g1.verses.size + g2.verses.size
                    }
                }
            }
        }
        return total
    }
}
