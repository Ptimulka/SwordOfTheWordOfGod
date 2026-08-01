package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

sealed interface FillWholeSiglaEffect {
    data object Success : FillWholeSiglaEffect
}
