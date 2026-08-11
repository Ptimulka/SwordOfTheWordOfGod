package io.github.ptimulka.miecz.screens.mnemonic

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import io.github.ptimulka.miecz.repositories.ChosenPicture

sealed interface MnemonicEvent {
    // List Screen
    data class SelectChoice(val index: Int, val choice: ChosenPicture) : MnemonicEvent
    data class ShowZoom(val bitmap: Bitmap?) : MnemonicEvent
    data object DownloadAll : MnemonicEvent
    data class EnterDrawing(val index: Int) : MnemonicEvent
    data class EnterImport(val index: Int) : MnemonicEvent
    data object BackFromList : MnemonicEvent

    // Drawing Screen
    data class AddStroke(val points: List<Offset>) : MnemonicEvent
    data class UpdateCurrentPoints(val points: List<Offset>) : MnemonicEvent
    data object Undo : MnemonicEvent
    data object Redo : MnemonicEvent
    data class SetColor(val color: Color) : MnemonicEvent
    data class SetStrokeWidth(val widthPx: Float) : MnemonicEvent
    data class SaveDrawing(val canvasSize: IntSize) : MnemonicEvent
    data object RequestBackFromDrawing : MnemonicEvent
    data object ConfirmDiscardDrawing : MnemonicEvent
    data object DismissExitDialog : MnemonicEvent

    // Import Screen
    data class ImageSelected(val uri: Uri) : MnemonicEvent
    data class UpdateTransform(val pan: Offset, val zoom: Float, val rotation: Float) : MnemonicEvent
    data class SetRotation(val degrees: Float) : MnemonicEvent
    data class ConfirmImport(val canvasSize: IntSize) : MnemonicEvent
    data object CancelImport : MnemonicEvent
}
