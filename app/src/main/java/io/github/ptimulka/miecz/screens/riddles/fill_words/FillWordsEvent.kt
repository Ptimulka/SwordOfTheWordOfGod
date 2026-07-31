package io.github.ptimulka.miecz.screens.riddles.fill_words

sealed interface FillWordsEvent {
    data class UpdateInput(val index: Int, val value: String) : FillWordsEvent
    data object Check : FillWordsEvent
    data object DismissResult : FillWordsEvent
    data object ShowHint : FillWordsEvent
    data object DismissHint : FillWordsEvent
}
