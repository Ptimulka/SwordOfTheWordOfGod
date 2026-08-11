package io.github.ptimulka.miecz.screens.riddles.fill_sigla

sealed interface FillSiglaEvent {
    data class UpdateInput(val value: String) : FillSiglaEvent
    data object Check : FillSiglaEvent
    data object DismissResult : FillSiglaEvent
    data object DismissHint : FillSiglaEvent
}
