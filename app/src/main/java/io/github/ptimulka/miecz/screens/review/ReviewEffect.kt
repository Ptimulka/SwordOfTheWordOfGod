package io.github.ptimulka.miecz.screens.review

import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section

sealed interface ReviewEffect {
    data class LaunchReview(
        val section: Section,
        val riddleTypes: List<RiddleType>
    ) : ReviewEffect
}
