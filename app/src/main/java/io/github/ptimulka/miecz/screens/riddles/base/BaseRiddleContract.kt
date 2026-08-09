package io.github.ptimulka.miecz.screens.riddles.base

sealed interface RiddlePhase {
    data object Answering : RiddlePhase
    data object ShowingHint : RiddlePhase
    data object ShowingReward : RiddlePhase
    data class Result(val correct: Boolean) : RiddlePhase
}

interface BaseRiddleUiState {
    val phase: RiddlePhase
    val hintBitmap: android.graphics.Bitmap?
}

sealed interface RiddleEvent {
    data object Check : RiddleEvent
    data object DismissResult : RiddleEvent
    data object ShowHint : RiddleEvent
    data object DismissHint : RiddleEvent
}
