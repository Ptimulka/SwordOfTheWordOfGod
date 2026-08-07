package io.github.ptimulka.miecz.screens.main

sealed interface MainEvent {
    data class SelectScreen(val screen: Screen) : MainEvent
    data object RefreshTabVisibility : MainEvent
}
