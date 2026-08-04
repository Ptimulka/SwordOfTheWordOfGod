package io.github.ptimulka.miecz.screens.random

sealed interface RandomVerseEvent {
    data class EnterScreen(val isNewTabEntry: Boolean) : RandomVerseEvent
    data object LeaveScreen : RandomVerseEvent
    data object DrawAnother : RandomVerseEvent
    data class SetEasy(val isEasy: Boolean) : RandomVerseEvent
    data object StartNow : RandomVerseEvent
}
