package io.github.ptimulka.miecz.screens.riddles.base

interface RiddleEffect {
    data class Success(val elapsedMs: Long? = null) : RiddleEffect
}
