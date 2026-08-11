package io.github.ptimulka.miecz.screens.random

import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section

sealed interface RandomVerseEffect {
    data class LaunchGame(
        val section: Section,
        val riddleTypes: List<RiddleType>
    ) : RandomVerseEffect
}
