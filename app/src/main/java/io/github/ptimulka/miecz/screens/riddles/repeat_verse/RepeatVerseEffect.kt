package io.github.ptimulka.miecz.screens.riddles.repeat_verse

import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect

sealed interface RepeatVerseEffect : RiddleEffect {
    data class StartListening(val preferOffline: Boolean) : RepeatVerseEffect
    data object RequestPermission : RepeatVerseEffect
}
