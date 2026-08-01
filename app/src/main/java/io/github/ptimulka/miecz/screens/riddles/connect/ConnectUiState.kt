package io.github.ptimulka.miecz.screens.riddles.connect

import android.graphics.Bitmap

data class ConnectItem(val id: Int, val text: String)

data class ConnectUiState(
    val leftItems: List<ConnectItem> = emptyList(),
    val rightItems: List<ConnectItem> = emptyList(),
    val selectedLeft: ConnectItem? = null,
    val selectedRight: ConnectItem? = null,
    val wrongPair: Pair<ConnectItem, ConnectItem>? = null,
    val justMatchedId: Int? = null,
    val previewBitmap: Bitmap? = null,
    val isLocked: Boolean = false,
    val completedElapsedMs: Long = -1L
)
