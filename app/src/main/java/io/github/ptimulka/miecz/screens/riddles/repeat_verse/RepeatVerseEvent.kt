package io.github.ptimulka.miecz.screens.riddles.repeat_verse

sealed interface RepeatVerseEvent {
    data class SelectVerse(val index: Int?) : RepeatVerseEvent
    data class SetListening(val isListening: Boolean) : RepeatVerseEvent
    data class UpdatePartialText(val text: String) : RepeatVerseEvent
    data class ProcessResult(val recognized: String) : RepeatVerseEvent
    data class ShowZoom(val index: Int?) : RepeatVerseEvent
    data object RequestPermission : RepeatVerseEvent
}
