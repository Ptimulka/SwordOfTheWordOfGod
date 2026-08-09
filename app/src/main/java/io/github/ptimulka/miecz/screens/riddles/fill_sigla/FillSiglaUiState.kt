package io.github.ptimulka.miecz.screens.riddles.fill_sigla

import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class FillSiglaUiState(
    val userInput: String = "",
    override val phase: RiddlePhase = RiddlePhase.Answering,
    override val hintBitmap: android.graphics.Bitmap? = null
) : BaseRiddleUiState
