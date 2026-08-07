package io.github.ptimulka.miecz.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_FOR_SHIELDS
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Riddle
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.updateProgress
import io.github.ptimulka.miecz.repositories.ProgressRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(
    private val sectionId: Int,
    private val sectionName: String,
    private val levelNumber: Int,
    private val sectionVerses: List<Verse>,
    private val assetNames: List<String>,
    private val levelRiddleTypeNames: List<String>,
    private val progressRepo: ProgressRepository,
    autoStartRefreshLoop: Boolean = true
) : ViewModel() {

    private val _state = MutableStateFlow(
        GameUiState(
            sectionId = sectionId,
            sectionName = sectionName,
            levelNumber = levelNumber,
            assetNames = assetNames,
            originalVerses = sectionVerses,
            shieldsCount = progressRepo.getShieldsCount()
        )
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<GameEffect>()
    val effects = _effects.receiveAsFlow()

    private val startTimeMs = System.currentTimeMillis()
    private var shieldLostThisLevel = false
    private var shieldRefreshJob: Job? = null
    
    // Waterfall queue for dialogs
    private val dialogQueue = mutableListOf<GameDialogState>()

    init {
        initRiddles()
        if (autoStartRefreshLoop) {
            startShieldRefreshLoop()
        }
    }

    private fun initRiddles() {
        val riddleTypes = levelRiddleTypeNames.mapNotNull { name ->
            runCatching { RiddleType.valueOf(name) }.getOrNull()
        }

        val generatedRiddles = riddleTypes.mapIndexed { index, riddleType ->
            val verse = sectionVerses.getOrElse(index) { sectionVerses.last() }
            Riddle(riddleType, verse)
        }.shuffled()

        _state.update { it.copy(
            riddles = generatedRiddles,
            isShieldsVisible = generatedRiddles.none { r -> r.type == RiddleType.REPEAT_VERSE }
        ) }
    }

    private fun startShieldRefreshLoop() {
        shieldRefreshJob?.cancel()
        shieldRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60000)
                val newShields = progressRepo.refreshShields()
                if (newShields != _state.value.shieldsCount) {
                    _state.update { it.copy(shieldsCount = newShields) }
                }
            }
        }
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.OnRiddleSuccess -> handleSuccess(event.elapsedMs)
            GameEvent.OnShieldLoss -> handleShieldLoss()
            GameEvent.ConfirmDialog -> nextDialogOrFinish()
            GameEvent.CancelDialog -> _state.update { it.copy(dialogState = GameDialogState.None) }
            GameEvent.RequestExit -> _state.update { it.copy(dialogState = GameDialogState.ExitConfirmation) }
        }
    }

    private fun handleSuccess(riddleElapsedMs: Long?) {
        val s = _state.value
        if (s.currentIndex < s.riddles.size - 1) {
            _state.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else {
            completeLevel(riddleElapsedMs)
        }
    }

    private fun completeLevel(riddleElapsedMs: Long?) {
        progressRepo.updateDayStreak()

        val riddles = _state.value.riddles
        val singleType = riddles.map { it.type }.toSet().singleOrNull()
        val isConnect = singleType == RiddleType.CONNECT_PARTS || singleType == RiddleType.CONNECT_PAIRS

        // 1. Award retention (before updateProgress)
        var retentionGained = 0
        if (sectionId >= 1) {
            retentionGained = when {
                isConnect -> progressRepo.awardRetentionForConnectLevel(sectionId, singleType!!.name)
                levelNumber in 1..12 -> progressRepo.awardRetentionForStandardLevel(sectionId, levelNumber)
                else -> 0
            }
        }

        // 2. Mark progress
        updateProgress(sectionId, levelNumber, progressRepo, riddles)

        // 3. Perfect streak logic
        val excludedFromStreak = setOf(RiddleType.CONNECT_PARTS, RiddleType.CONNECT_PAIRS, RiddleType.REPEAT_VERSE)
        val countsForStreak = sectionId >= 1 && !shieldLostThisLevel && riddles.none { it.type in excludedFromStreak }
        if (countsForStreak) {
            progressRepo.incrementLevelStreak()
            val current = progressRepo.getLevelStreak()
            if (progressRepo.updateBestLevelStreak(current)) {
                dialogQueue.add(GameDialogState.NewStreak(current))
            }
        }

        // 4. Connect/Record logic
        if (isConnect) {
            val elapsed = riddleElapsedMs ?: (System.currentTimeMillis() - startTimeMs)
            val isNewRecord = progressRepo.updateBestTime(sectionId, singleType!!.name, elapsed)
            if (isNewRecord) {
                dialogQueue.add(GameDialogState.NewRecord(elapsed))
            } else {
                val msgRes = if (singleType == RiddleType.CONNECT_PARTS) R.string.connect_parts_success_message else R.string.connect_pairs_success_message
                dialogQueue.add(GameDialogState.ConnectSuccess(elapsed, msgRes))
            }
        } else if (riddles.size > 1) {
            if (sectionId == SECTION_ID_REPEAT_FOR_SHIELDS) {
                val reward = if (riddles.size == 10) 2 else 1
                dialogQueue.add(GameDialogState.SuccessForShields(reward))
            } else {
                dialogQueue.add(GameDialogState.GenericSuccess)
            }
        }

        // 5. Add retention to queue
        if (retentionGained > 0) {
            dialogQueue.add(GameDialogState.RetentionGained(retentionGained))
        }

        nextDialogOrFinish()
    }

    private fun handleShieldLoss() {
        if (sectionId == SECTION_ID_REPEAT_FOR_SHIELDS) {
            _state.update { it.copy(dialogState = GameDialogState.NoMoreShieldsPlay) }
            return
        }

        if (sectionId < 1) return

        progressRepo.refreshShields()
        progressRepo.decreaseShields()
        progressRepo.resetLevelStreak()
        shieldLostThisLevel = true
        
        val newCount = progressRepo.getShieldsCount()
        _state.update { it.copy(shieldsCount = newCount) }
        
        if (newCount <= 0) {
            progressRepo.setPendingRepeatHint()
            _state.update { it.copy(dialogState = GameDialogState.NoShields) }
        }
    }

    private fun nextDialogOrFinish() {
        if (dialogQueue.isNotEmpty()) {
            val next = dialogQueue.removeAt(0)
            _state.update { it.copy(dialogState = next) }
        } else {
            viewModelScope.launch { _effects.send(GameEffect.FinishGame) }
        }
    }
}
