package io.github.ptimulka.miecz.screens.riddles.repeat_verse

sealed interface RepeatVerseEffect {
    data class StartListening(val preferOffline: Boolean) : RepeatVerseEffect
    data object RequestPermission : RepeatVerseEffect
}
