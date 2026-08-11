package io.github.ptimulka.miecz.screens.game

sealed interface GameEvent {
    data class OnRiddleSuccess(val elapsedMs: Long? = null) : GameEvent
    data object OnShieldLoss : GameEvent
    data object ConfirmDialog : GameEvent
    data object CancelDialog : GameEvent
    data object RequestExit : GameEvent
}
