package io.github.ptimulka.miecz.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val INITIAL_COUNTDOWN = 10

class RandomVerseViewModel(
    private val repository: VersesGroupsRepository,
    private val sectionName: String
) : ViewModel() {

    private val _state = MutableStateFlow(RandomVerseUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<RandomVerseEffect>()
    val effects = _effects.receiveAsFlow()

    private val allVerses: List<Verse> by lazy {
        repository.loadVerseGroups().flatMap { it.verses }
    }

    private var countdownJob: Job? = null

    init {
        // Only pick initial verse if we don't have one
        if (_state.value.randomVerse == null) {
            drawAnother(shouldAutoStart = false)
        }
    }

    fun onEvent(event: RandomVerseEvent) {
        when (event) {
            is RandomVerseEvent.EnterScreen -> {
                if (event.isNewTabEntry) {
                    _state.update { it.copy(countdown = INITIAL_COUNTDOWN, isCountingDown = true) }
                }
                startCountdown()
            }
            RandomVerseEvent.LeaveScreen -> {
                countdownJob?.cancel()
            }
            RandomVerseEvent.DrawAnother -> drawAnother(shouldAutoStart = true)
            is RandomVerseEvent.SetEasy -> _state.update { it.copy(isEasy = event.isEasy) }
            RandomVerseEvent.StartNow -> {
                _state.update { it.copy(countdown = 0) }
                finishCountdown()
            }
        }
    }

    private fun drawAnother(shouldAutoStart: Boolean) {
        if (allVerses.isEmpty()) return
        
        _state.update { 
            it.copy(
                randomVerse = allVerses.random(),
                countdown = INITIAL_COUNTDOWN,
                isCountingDown = shouldAutoStart
            )
        }
        if (shouldAutoStart) {
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        if (!_state.value.isCountingDown) return

        countdownJob = viewModelScope.launch {
            while (_state.value.countdown > 0) {
                delay(1000)
                _state.update { it.copy(countdown = it.countdown - 1) }
            }
            finishCountdown()
        }
    }

    private fun finishCountdown() {
        countdownJob?.cancel()
        val s = _state.value
        if (s.randomVerse == null) return

        _state.update { it.copy(isCountingDown = false) }

        val riddleTypes = if (s.isEasy) {
            listOf(
                RiddleType.QUIZ_EASY,
                RiddleType.FILL_WORDS_EASY,
                RiddleType.FILL_SIGLA_BOOK,
                RiddleType.FILL_MORE_WORDS_EASY,
                RiddleType.FILL_SIGLA_CHAPTER
            )
        } else {
            listOf(
                RiddleType.QUIZ_NORMAL,
                RiddleType.FILL_WORDS_NORMAL,
                RiddleType.MULTI_QUIZ,
                RiddleType.FILL_MORE_WORDS_NORMAL,
                RiddleType.FILL_SIGLA_VERSE
            )
        }

        viewModelScope.launch {
            _effects.send(RandomVerseEffect.LaunchGame(
                section = Section(
                    id = 0,
                    name = sectionName,
                    verses = listOf(s.randomVerse),
                    assetNames = emptyList()
                ),
                riddleTypes = riddleTypes
            ))
        }
    }
}
