package io.github.ptimulka.miecz.screens.riddles.connect

sealed interface ConnectEvent {
    data class SelectLeft(val item: ConnectItem) : ConnectEvent
    data class SelectRight(val item: ConnectItem) : ConnectEvent
    data object DismissImage : ConnectEvent
}
