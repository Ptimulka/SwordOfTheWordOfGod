package io.github.ptimulka.miecz.screens.main

sealed interface MainEvent {
    data object RefreshTabVisibility : MainEvent
}
