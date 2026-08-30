package io.github.ptimulka.miecz.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.main.LevelButtonState
import io.github.ptimulka.miecz.data.Constants
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.RiddlesOrderRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = GameLevelViewModel.Factory::class)
class GameLevelViewModel @AssistedInject constructor(
    private val progressRepo: ProgressRepository,
    private val sectionRepo: SectionRepository,
    private val groupsRepo: VersesGroupsRepository,
    private val riddlesOrderRepo: RiddlesOrderRepository,
    @Assisted private val autoStartRefreshLoop: Boolean = true
) : ViewModel() {

    private val _state = MutableStateFlow(GameLevelUiState())
    val state = _state.asStateFlow()

    private var shieldRefreshJob: Job? = null
    private var shieldAutoHideJob: Job? = null
    private var lampAutoHideJob: Job? = null

    init {
        loadInitialData()
        if (autoStartRefreshLoop) {
            startShieldRefreshLoop()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val riddlesOrder = riddlesOrderRepo.getRiddlesOrder()
            _state.update { it.copy(riddlesOrder = riddlesOrder) }
            refreshProgress()
        }
    }

    fun onEvent(event: GameLevelEvent) {
        when (event) {
            GameLevelEvent.OnResume -> {
                progressRepo.applyDailyRetentionDecay()
                viewModelScope.launch {
                    refreshProgress()
                }
                checkPendingNotifications()
            }
            GameLevelEvent.ToggleShieldInfo -> toggleShieldInfo()
            GameLevelEvent.ToggleLampInfo -> toggleLampInfo()
            GameLevelEvent.DismissUnlockedDialog -> _state.update { 
                it.copy(progress = it.progress.copy(unlockedSectionId = -1)) 
            }
            GameLevelEvent.NavigateToChooseGroups -> _state.update { it.copy(showChooseVerseGroups = true, selectedGroupIds = emptyList()) }
            GameLevelEvent.CancelChooseGroups -> _state.update { it.copy(showChooseVerseGroups = false, selectedGroupIds = emptyList()) }
            GameLevelEvent.ConfirmGroupSelection -> confirmGroupSelection()
            is GameLevelEvent.ShowSectionVerses -> _state.update { it.copy(selectedSectionForDialog = event.section) }
            is GameLevelEvent.ToggleGroupSelection -> toggleGroupSelection(event.groupId)
        }
    }

    private suspend fun refreshProgress() {
        _state.update { it.copy(isLoading = true) }
        val usedGroupIds = progressRepo.getAllUsedGroupIds()
        val allGroups = groupsRepo.loadVerseGroups()
        val baseSections = sectionRepo.loadInitialSections()
        val fullSections = buildFullSectionList(baseSections)
        val riddlesOrder = _state.value.riddlesOrder

        val sectionStates = fullSections.associate { section ->
            val finishedLevels = (1..Constants.LEVELS_PER_SECTION).filter { progressRepo.isLevelFinished(section.id, it) }.toSet()
            val retention = progressRepo.getRetention(section.id)
            val isSiglaFinished = progressRepo.isSiglaFinished(section.id)
            val isVerseFinished = progressRepo.isVerseFinished(section.id)
            
            val isConnectPartsDoneToday = progressRepo.isConnectDoneToday(section.id, RiddleType.CONNECT_PARTS.name)
            val isConnectPairsDoneToday = progressRepo.isConnectDoneToday(section.id, RiddleType.CONNECT_PAIRS.name)
            
            val verseContributions = section.verses.indices.map {
                progressRepo.retentionContributionForRepeats(
                    progressRepo.getVerseRepeatCountToday(section.id, it)
                )
            }
            val totalEarnedRepeats = verseContributions.sum()
            val totalPossibleRepeats = section.verses.size * Constants.REPEAT_REWARD_HIGH
            val repeatAloudPercent = if (totalPossibleRepeats > 0) {
                (totalEarnedRepeats * 100) / totalPossibleRepeats
            } else 0
            
            val isRepeatAloudDoneToday = repeatAloudPercent >= 100

            val areChallengesFinished = isSiglaFinished && isVerseFinished
            val effectiveRetention = if (areChallengesFinished) Constants.MAX_RETENTION else retention
            val raysReach = (if (effectiveRetention >= Constants.RAYS_MIN_RETENTION) ((effectiveRetention - Constants.RAYS_MIN_RETENTION) / Constants.RAYS_STEP_RETENTION) + 1 else 0)
                .coerceAtMost(Constants.LEVELS_PER_SECTION)

            val levels = riddlesOrder.mapIndexed { index, riddleList ->
                val levelNumber = index + 1
                val isFinished = finishedLevels.contains(levelNumber)
                val isPreviousFinished = if (levelNumber > 1) finishedLevels.contains(levelNumber - 1) else true
                
                val progressionUnlocked = isFinished || isPreviousFinished || areChallengesFinished
                val raysUnlocked = levelNumber <= raysReach
                
                val levelState = when {
                    section.id > progressRepo.getCurrentSection() -> LevelButtonState.LOCKED
                    isFinished -> LevelButtonState.FULL
                    progressionUnlocked && raysUnlocked -> LevelButtonState.FULL
                    progressionUnlocked != raysUnlocked -> LevelButtonState.HALF
                    else -> LevelButtonState.LOCKED
                }

                val lockMessageRes = when {
                    section.id > progressRepo.getCurrentSection() || levelState == LevelButtonState.FULL -> null
                    !progressionUnlocked && !raysUnlocked -> R.string.level_locked_need_both
                    !progressionUnlocked -> R.string.level_locked_need_previous
                    else -> R.string.level_locked_need_retention
                }

                LevelUiState(
                    levelNumber = levelNumber,
                    isFinished = isFinished,
                    state = levelState,
                    lockMessageRes = lockMessageRes,
                    riddles = riddleList
                )
            }

            section.id to SectionState(
                finishedLevels = finishedLevels,
                levels = levels,
                raysReach = raysReach,
                retention = retention,
                isSiglaFinished = isSiglaFinished,
                isVerseFinished = isVerseFinished,
                dailyRetentionMaxed = isConnectPartsDoneToday && isConnectPairsDoneToday && isRepeatAloudDoneToday,
                connectPartsRetentionPercent = if (isConnectPartsDoneToday) 100 else 0,
                connectPairsRetentionPercent = if (isConnectPairsDoneToday) 100 else 0,
                repeatAloudRetentionPercent = repeatAloudPercent,
                isConnectPartsDoneToday = isConnectPartsDoneToday,
                isConnectPairsDoneToday = isConnectPairsDoneToday,
                isRepeatAloudDoneToday = isRepeatAloudDoneToday,
                bestTimeParts = progressRepo.getBestTime(section.id, RiddleType.CONNECT_PARTS.name),
                bestTimePairs = progressRepo.getBestTime(section.id, RiddleType.CONNECT_PAIRS.name)
            )
        }

        _state.update { 
            it.copy(
                isLoading = false,
                sections = fullSections,
                sectionStates = sectionStates,
                allVerseGroups = allGroups,
                usedGroupIds = usedGroupIds,
                progress = it.progress.copy(
                    currentSectionId = progressRepo.getCurrentSection(),
                    shieldsCount = progressRepo.getShieldsCount(),
                    dayStreak = progressRepo.getCurrentDayStreak(),
                    playedToday = progressRepo.hasPlayedToday(),
                    timeToNextShieldMs = progressRepo.getTimeToNextShield(),
                    availableGroupsCount = allGroups.count { g -> !usedGroupIds.contains(g.id) }
                )
            )
        }
    }

    private fun refreshShieldsStatus() {
        _state.update { 
            it.copy(
                progress = it.progress.copy(
                    shieldsCount = progressRepo.getShieldsCount(),
                    timeToNextShieldMs = progressRepo.getTimeToNextShield()
                )
            )
        }
    }

    private fun toggleGroupSelection(groupId: Int) {
        _state.update { s ->
            val current = s.selectedGroupIds
            val next = if (current.contains(groupId)) {
                current - groupId
            } else if (current.size < Constants.MAX_CUSTOM_SECTION_GROUPS) {
                current + groupId
            } else {
                current
            }
            s.copy(selectedGroupIds = next)
        }
    }

    private fun confirmGroupSelection() {
        val selection = _state.value.selectedGroupIds
        if (selection.size == Constants.MAX_CUSTOM_SECTION_GROUPS) {
            val nextId = (_state.value.sections.lastOrNull()?.id ?: Constants.BASE_SECTIONS_COUNT) + 1
            progressRepo.saveCustomSection(nextId, selection[0], selection[1])
            _state.update { it.copy(showChooseVerseGroups = false, selectedGroupIds = emptyList()) }
            viewModelScope.launch {
                refreshProgress()
            }
        }
    }

    private suspend fun buildFullSectionList(baseSections: List<Section>): List<Section> {
        val allVerseGroups = groupsRepo.loadVerseGroups()
        val customCount = progressRepo.getCustomSectionsCount()
        
        val customSections = (1..customCount).mapNotNull { index ->
            val sectionId = Constants.CUSTOM_SECTION_START_ID + index - 1
            progressRepo.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                val g1 = allVerseGroups.find { it.id == id1 }
                val g2 = allVerseGroups.find { it.id == id2 }
                if (g1 != null && g2 != null) {
                    val verses = g1.verses + g2.verses
                    val assetNames = g1.verses.mapIndexed { i, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id1, i + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    } + g2.verses.mapIndexed { i, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id2, i + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    }
                    Section(id = sectionId, name = "${g1.name} i ${g2.name}", verses = verses, assetNames = assetNames)
                } else null
            }
        }
        return baseSections + customSections
    }

    private fun startShieldRefreshLoop() {
        shieldRefreshJob?.cancel()
        shieldRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(Constants.SHIELD_REFRESH_DELAY)
                progressRepo.refreshShields()
                refreshShieldsStatus()
            }
        }
    }

    private fun toggleShieldInfo() {
        val nextVisible = !_state.value.isShieldInfoVisible
        _state.update { it.copy(isShieldInfoVisible = nextVisible) }
        
        shieldAutoHideJob?.cancel()
        if (nextVisible) {
            shieldAutoHideJob = viewModelScope.launch {
                // High-frequency refresh loop while the toast is visible
                var iterations = 0
                while (iterations < Constants.SHIELD_INFO_AUTO_HIDE_ITERATIONS) {
                    progressRepo.refreshShields()
                    refreshShieldsStatus()
                    delay(Constants.SHIELD_INFO_REFRESH_INTERVAL)
                    iterations++
                }
                _state.update { it.copy(isShieldInfoVisible = false) }
            }
        }
    }

    private fun toggleLampInfo() {
        val nextVisible = !_state.value.isLampInfoVisible
        _state.update { it.copy(isLampInfoVisible = nextVisible) }
        
        lampAutoHideJob?.cancel()
        if (nextVisible) {
            lampAutoHideJob = viewModelScope.launch {
                delay(Constants.LAMP_INFO_AUTO_HIDE_DELAY)
                _state.update { it.copy(isLampInfoVisible = false) }
            }
        }
    }

    private fun checkPendingNotifications() {
        val repeatHint = progressRepo.consumePendingRepeatHint()
        val unlocked = progressRepo.consumePendingSectionUnlocked()
        
        if (repeatHint || unlocked != -1) {
            _state.update { 
                it.copy(
                    progress = it.progress.copy(
                        showRepeatHint = repeatHint,
                        unlockedSectionId = unlocked
                    )
                )
            }
        }
        
        if (repeatHint) {
            viewModelScope.launch {
                delay(Constants.REPEAT_HINT_AUTO_HIDE_DELAY)
                _state.update { it.copy(progress = it.progress.copy(showRepeatHint = false)) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(autoStartRefreshLoop: Boolean = true): GameLevelViewModel
    }
}
