package io.github.ptimulka.miecz.screens.riddles.connect

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.ConnectDataBuilder
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import kotlinx.coroutines.flow.update

class ConnectPairsViewModel(
    verses: List<Verse>,
    sectionId: Int,
    assetNames: List<String>,
    mnemonicRepo: MnemonicRepository
) : BaseConnectViewModel(verses, sectionId, assetNames, mnemonicRepo) {

    init {
        val (left, right) = ConnectDataBuilder.buildPairsData(verses)
        _state.update { it.copy(leftItems = left, rightItems = right) }
    }
}
