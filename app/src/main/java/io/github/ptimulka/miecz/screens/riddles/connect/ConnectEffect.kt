package io.github.ptimulka.miecz.screens.riddles.connect

sealed interface ConnectEffect {
    data class Success(val elapsedMs: Long) : ConnectEffect
}
