package io.github.ptimulka.miecz.screens.riddles.repeat_verse

import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class RepeatVerseUiState(
    val selectedIndex: Int? = null,
    val repeatCount: Int = 0,
    val isListening: Boolean = false,
    val lastSimilarity: Float = -1f,
    val partialText: String = "",
    val zoomIndex: Int? = null,
    val verseRetentionToday: Int = 0,
    val sectionRetentionToday: Int = 0,
    val isSectionFinished: Boolean = false,
    val maxedIndices: Set<Int> = emptySet(),
    override val phase: RiddlePhase = RiddlePhase.Answering,
    override val hintBitmap: android.graphics.Bitmap? = null
) : BaseRiddleUiState
