package io.github.ptimulka.miecz.screens.review

import io.github.ptimulka.miecz.data.Verse

data class ReviewUiState(
    val shieldsCount: Int = 5,
    val selectedCount: Int = 10,
    val playForShields: Boolean = false,
    val knownVersesSections: List<Pair<List<Verse>, List<String>>> = emptyList(),
    val maxPossibleReward: Int = 0
) {
    val isReviewAvailable: Boolean get() = knownVersesSections.isNotEmpty()
}
