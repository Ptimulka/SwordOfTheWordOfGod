package io.github.ptimulka.miecz.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.main.LevelButtonState
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

class GameLevelViewModel(
    private val progressRepo: ProgressRepository,
    private val sectionRepo: SectionRepository,
    private val groupsRepo: VersesGroupsRepository,
    private val riddlesOrderRepo: RiddlesOrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameLevelUiState())
    val state = _state.asStateFlow()

    private var shieldRefreshJob: Job? = null
    private var shieldAutoHideJob: Job? = null
    private var lampAutoHideJob: Job? = null

    init {
        loadInitialData()
        startShieldRefreshLoop()
    }

    private fun loadInitialData() {
        val riddlesOrder = riddlesOrderRepo.getRiddlesOrder()
        _state.update { it.copy(riddlesOrder = riddlesOrder) }
        refreshProgress()
    }

    fun onEvent(event: GameLevelEvent) {
        when (event) {
            GameLevelEvent.OnResume -> {
                progressRepo.applyDailyRetentionDecay()
                refreshProgress()
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

    private fun refreshProgress() {
        val usedGroupIds = progressRepo.getAllUsedGroupIds()
        val allGroups = groupsRepo.loadVerseGroups()
        val baseSections = sectionRepo.loadInitialSections()
        val fullSections = buildFullSectionList(baseSections)
        val riddlesOrder = riddlesOrderRepo.getRiddlesOrder()

        val sectionStates = fullSections.associate { section ->
            val finishedLevels = (1..12).filter { progressRepo.isLevelFinished(section.id, it) }.toSet()
            val retention = progressRepo.getRetention(section.id)
            val isSiglaFinished = progressRepo.isSiglaFinished(section.id)
            val isVerseFinished = progressRepo.isVerseFinished(section.id)
            
            val connectsDone = progressRepo.isConnectDoneToday(section.id, io.github.ptimulka.miecz.data.RiddleType.CONNECT_PARTS.name) &&
                              progressRepo.isConnectDoneToday(section.id, io.github.ptimulka.miecz.data.RiddleType.CONNECT_PAIRS.name)
            
            val versesMaxed = section.verses.indices.all {
                progressRepo.retentionContributionForRepeats(
                    progressRepo.getVerseRepeatCountToday(section.id, it)
                ) >= 3
            }

            val areChallengesFinished = isSiglaFinished && isVerseFinished
            val effectiveRetention = if (areChallengesFinished) 100 else retention
            val raysReach = (if (effectiveRetention >= 4) ((effectiveRetention - 4) / 8) + 1 else 0)
                .coerceAtMost(12) // SECTION_LEVEL_COUNT

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
                dailyRetentionMaxed = connectsDone && versesMaxed,
                bestTimeParts = progressRepo.getBestTime(section.id, io.github.ptimulka.miecz.data.RiddleType.CONNECT_PARTS.name),
                bestTimePairs = progressRepo.getBestTime(section.id, io.github.ptimulka.miecz.data.RiddleType.CONNECT_PAIRS.name)
            )
        }

        _state.update { 
            it.copy(
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
            } else if (current.size < 2) {
                current + groupId
            } else {
                current
            }
            s.copy(selectedGroupIds = next)
        }
    }

    private fun confirmGroupSelection() {
        val selection = _state.value.selectedGroupIds
        if (selection.size == 2) {
            val nextId = (_state.value.sections.lastOrNull()?.id ?: 4) + 1
            progressRepo.saveCustomSection(nextId, selection[0], selection[1])
            _state.update { it.copy(showChooseVerseGroups = false, selectedGroupIds = emptyList()) }
            refreshProgress()
        }
    }

    private fun buildFullSectionList(baseSections: List<Section>): List<Section> {
        val allVerseGroups = groupsRepo.loadVerseGroups()
        val customCount = progressRepo.getCustomSectionsCount()
        
        val customSections = (1..customCount).mapNotNull { index ->
            val sectionId = 5 + index - 1
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
                delay(60000)
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
                while (iterations < 7) {
                    progressRepo.refreshShields()
                    refreshShieldsStatus()
                    delay(1000)
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
                delay(7000)
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
                delay(30000)
                _state.update { it.copy(progress = it.progress.copy(showRepeatHint = false)) }
            }
        }
    }
}
