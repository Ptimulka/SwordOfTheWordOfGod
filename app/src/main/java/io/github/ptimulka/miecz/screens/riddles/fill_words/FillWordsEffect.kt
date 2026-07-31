package io.github.ptimulka.miecz.screens.riddles.fill_words

sealed interface FillWordsEffect {
    data object Success : FillWordsEffect
}
