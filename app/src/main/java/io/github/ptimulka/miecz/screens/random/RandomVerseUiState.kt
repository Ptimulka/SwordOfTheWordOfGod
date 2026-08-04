package io.github.ptimulka.miecz.screens.random

import io.github.ptimulka.miecz.data.Verse

data class RandomVerseUiState(
    val randomVerse: Verse? = null,
    val isEasy: Boolean = false,
    val countdown: Int = 10,
    val isCountingDown: Boolean = true
)
