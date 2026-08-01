package io.github.ptimulka.miecz.screens.riddles.word_scramble

sealed interface WordScrambleEffect {
    data object Success : WordScrambleEffect
}
