package io.github.ptimulka.miecz.screens.mnemonic

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.ChosenPicture

enum class MnemonicScreen {
    LIST, DRAWING, IMPORT
}

data class MnemonicMessage(
    val resId: Int,
    val formatArgs: List<Any> = emptyList()
)

data class VerseMnemonicData(
    val verse: Verse,
    val userBitmap: Bitmap? = null,
    val importedBitmap: Bitmap? = null,
    val defaultBitmap: Bitmap? = null,
    val chosen: ChosenPicture? = null
) {
    val effectiveChosen: ChosenPicture
        get() = when (chosen) {
            null -> when {
                defaultBitmap != null -> ChosenPicture.DEFAULT
                importedBitmap != null -> ChosenPicture.IMPORTED
                userBitmap != null -> ChosenPicture.USER
                else -> ChosenPicture.NONE
            }
            ChosenPicture.DEFAULT -> if (defaultBitmap != null) ChosenPicture.DEFAULT
            else if (importedBitmap != null) ChosenPicture.IMPORTED
            else if (userBitmap != null) ChosenPicture.USER
            else ChosenPicture.NONE
            ChosenPicture.IMPORTED -> if (importedBitmap != null) ChosenPicture.IMPORTED
            else if (userBitmap != null) ChosenPicture.USER
            else if (defaultBitmap != null) ChosenPicture.DEFAULT
            else ChosenPicture.NONE
            ChosenPicture.USER -> if (userBitmap != null) ChosenPicture.USER
            else if (importedBitmap != null) ChosenPicture.IMPORTED
            else if (defaultBitmap != null) ChosenPicture.DEFAULT
            else ChosenPicture.NONE
            ChosenPicture.NONE -> ChosenPicture.NONE
        }
}

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthPx: Float
)

data class DrawingState(
    val verseIndex: Int = 0,
    val strokes: List<DrawingStroke> = emptyList(),
    val undoStack: List<DrawingStroke> = emptyList(),
    val redoStack: List<DrawingStroke> = emptyList(),
    val currentPoints: List<Offset> = emptyList(),
    val selectedColor: Color = Color.Black,
    val strokeWidthPx: Float = 8f,
    val isDirty: Boolean = false,
    val showExitDialog: Boolean = false
)

data class ImportState(
    val verseIndex: Int = 0,
    val selectedBitmap: Bitmap? = null,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val isProcessing: Boolean = false
)

data class MnemonicUiState(
    val sectionId: Int = 0,
    val sectionName: String = "",
    val currentScreen: MnemonicScreen = MnemonicScreen.LIST,
    val verseData: List<VerseMnemonicData> = emptyList(),
    val drawingState: DrawingState = DrawingState(),
    val importState: ImportState = ImportState(),
    val zoomedBitmap: Bitmap? = null,
    val downloadMessage: MnemonicMessage? = null,
    val isDownloading: Boolean = false
)
