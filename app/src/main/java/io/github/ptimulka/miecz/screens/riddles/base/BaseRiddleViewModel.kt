package io.github.ptimulka.miecz.screens.riddles.base

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseRiddleViewModel<S : BaseRiddleUiState>(
    initialState: S,
    private val mnemonicRepo: MnemonicRepository? = null,
    private val sectionId: Int = 0,
    private val verseIndex: Int = 0,
    private val assetName: String? = null
) : ViewModel() {

    protected val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    protected val _effects = Channel<RiddleEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        loadHintBitmap()
    }

    private fun loadHintBitmap() {
        if (mnemonicRepo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = mnemonicRepo.loadActivePicture(sectionId, verseIndex, assetName)
                _state.update { updateHintBitmap(it, bitmap) }
            }
        }
    }

    fun onBaseEvent(event: RiddleEvent) {
        when (event) {
            RiddleEvent.Check -> checkAnswer()
            RiddleEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is RiddlePhase.Result && phase.correct) {
                    emitSuccess()
                }
                _state.update { updatePhase(it, RiddlePhase.Answering) }
            }
            RiddleEvent.ShowHint -> _state.update { updatePhase(it, RiddlePhase.ShowingHint) }
            RiddleEvent.DismissHint -> {
                val s = _state.value
                if (s.phase is RiddlePhase.ShowingReward) {
                    _state.update { updatePhase(it, RiddlePhase.Result(correct = true)) }
                } else {
                    _state.update { updatePhase(it, RiddlePhase.Answering) }
                }
            }
        }
    }

    protected abstract fun checkAnswer()
    
    protected abstract fun updatePhase(state: S, newPhase: RiddlePhase): S
    protected abstract fun updateHintBitmap(state: S, bitmap: Bitmap?): S

    protected fun emitSuccess(elapsedMs: Long? = null) {
        viewModelScope.launch {
            _effects.send(RiddleEffect.Success(elapsedMs))
        }
    }
    
    protected fun setResult(correct: Boolean, hasHint: Boolean = false) {
        if (correct && hasHint) {
            _state.update { updatePhase(it, RiddlePhase.ShowingReward) }
        } else {
            _state.update { updatePhase(it, RiddlePhase.Result(correct)) }
        }
    }
}
