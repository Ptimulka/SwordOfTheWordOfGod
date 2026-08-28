package io.github.ptimulka.miecz.screens.riddles.base

import android.graphics.Bitmap

sealed interface RiddlePhase {
    data object Answering : RiddlePhase
    data class ShowingHint(val bitmap: Bitmap) : RiddlePhase
    data class ShowingReward(val bitmap: Bitmap) : RiddlePhase
    data class Result(val correct: Boolean) : RiddlePhase
}

interface BaseRiddleUiState {
    val phase: RiddlePhase
    val hintBitmap: Bitmap?
}

sealed interface RiddleEvent {
    data object Check : RiddleEvent
    data object DismissResult : RiddleEvent
    data object ShowHint : RiddleEvent
    data object DismissHint : RiddleEvent
}
