package io.github.ptimulka.miecz.screens.game

sealed interface GameEffect {
    data object FinishGame : GameEffect
}
