package io.github.ptimulka.miecz.screens.mnemonic

import android.net.Uri

sealed interface MnemonicEffect {
    data object FinishActivity : MnemonicEffect
    data class ShowToast(val message: MnemonicMessage) : MnemonicEffect
    data object LaunchImagePicker : MnemonicEffect
}
