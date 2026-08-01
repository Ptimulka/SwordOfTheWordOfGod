package io.github.ptimulka.miecz.screens.riddles.connect

import android.graphics.Bitmap
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.ConnectDataBuilder
import kotlinx.coroutines.flow.update

class ConnectPartsViewModel(
    verses: List<Verse>,
    hintBitmaps: Map<Int, Bitmap>
) : BaseConnectViewModel(hintBitmaps) {

    init {
        val (left, right) = ConnectDataBuilder.buildPartsData(verses)
        _state.update { it.copy(leftItems = left, rightItems = right) }
    }
}
