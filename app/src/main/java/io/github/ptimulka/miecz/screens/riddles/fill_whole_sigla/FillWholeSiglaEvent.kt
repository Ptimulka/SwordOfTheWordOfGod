package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

sealed interface FillWholeSiglaEvent {
    data class UpdateBook(val value: String) : FillWholeSiglaEvent
    data class UpdateChapter(val value: String) : FillWholeSiglaEvent
    data class UpdateVerse(val value: String) : FillWholeSiglaEvent
    data object Check : FillWholeSiglaEvent
    data object DismissResult : FillWholeSiglaEvent
    data object DismissHint : FillWholeSiglaEvent
}
