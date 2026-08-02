package io.github.ptimulka.miecz.screens.review

sealed interface ReviewEvent {
    data object OnResume : ReviewEvent
    data class SetCount(val count: Int) : ReviewEvent
    data class SetPlayForShields(val play: Boolean) : ReviewEvent
    data object StartReview : ReviewEvent
}
