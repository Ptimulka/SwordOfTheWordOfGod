package io.github.ptimulka.miecz.screens.riddles.base

sealed interface RiddleEffect {
    data class Success(val elapsedMs: Long? = null) : RiddleEffect
}
