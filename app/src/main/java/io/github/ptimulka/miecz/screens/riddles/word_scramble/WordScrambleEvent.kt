package io.github.ptimulka.miecz.screens.riddles.word_scramble

import io.github.ptimulka.miecz.data.WordItem

sealed interface WordScrambleEvent {
    data class PlaceWord(val word: WordItem) : WordScrambleEvent
    data class UnplaceWord(val word: WordItem) : WordScrambleEvent
    data class SelectForReorder(val word: WordItem?) : WordScrambleEvent
    data class MoveLeft(val index: Int) : WordScrambleEvent
    data class MoveRight(val index: Int) : WordScrambleEvent
    data object Reset : WordScrambleEvent
    data object Check : WordScrambleEvent
    data object DismissResult : WordScrambleEvent
    data object ShowHint : WordScrambleEvent
    data object DismissHint : WordScrambleEvent
}
