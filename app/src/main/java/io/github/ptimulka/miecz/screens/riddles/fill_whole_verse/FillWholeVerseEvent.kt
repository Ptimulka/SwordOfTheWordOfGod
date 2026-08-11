package io.github.ptimulka.miecz.screens.riddles.fill_whole_verse

sealed interface FillWholeVerseEvent {
    data class UpdateInput(val value: String) : FillWholeVerseEvent
    data object Check : FillWholeVerseEvent
    data object DismissResult : FillWholeVerseEvent
    data object ShowHint : FillWholeVerseEvent
    data object DismissHint : FillWholeVerseEvent
}
