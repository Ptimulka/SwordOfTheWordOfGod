package io.github.ptimulka.miecz.screens.riddles.connect

import android.graphics.Bitmap
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class ConnectItem(val id: Int, val text: String)

data class ConnectUiState(
    val leftItems: List<ConnectItem> = emptyList(),
    val rightItems: List<ConnectItem> = emptyList(),
    val selectedLeft: ConnectItem? = null,
    val selectedRight: ConnectItem? = null,
    val wrongPair: Pair<ConnectItem, ConnectItem>? = null,
    val justMatchedId: Int? = null,
    val previewBitmap: Bitmap? = null,
    val buttonBitmaps: Map<Int, Bitmap> = emptyMap(),
    val isLocked: Boolean = false,
    val completedElapsedMs: Long = -1L,
    override val phase: RiddlePhase = RiddlePhase.Answering,
    override val hintBitmap: Bitmap? = null
) : BaseRiddleUiState
