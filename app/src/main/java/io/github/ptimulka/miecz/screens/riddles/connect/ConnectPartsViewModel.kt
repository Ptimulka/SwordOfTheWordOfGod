package io.github.ptimulka.miecz.screens.riddles.connect

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.ConnectDataBuilder
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import kotlinx.coroutines.flow.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = ConnectPartsViewModel.Factory::class)
class ConnectPartsViewModel @AssistedInject constructor(
    @Assisted("verses") verses: List<Verse>,
    @Assisted("sectionId") sectionId: Int,
    @Assisted("assetNames") assetNames: List<String>,
    mnemonicRepo: MnemonicRepository
) : BaseConnectViewModel(verses, sectionId, assetNames, mnemonicRepo) {

    init {
        val (left, right) = ConnectDataBuilder.buildPartsData(verses)
        _state.update { it.copy(leftItems = left, rightItems = right) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("verses") verses: List<Verse>,
            @Assisted("sectionId") sectionId: Int,
            @Assisted("assetNames") assetNames: List<String>
        ): ConnectPartsViewModel
    }
}
