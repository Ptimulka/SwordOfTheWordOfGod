package io.github.ptimulka.miecz.screens.mnemonic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.MnemonicImageHelper
import io.github.ptimulka.miecz.repositories.ChosenPicture
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = MnemonicViewModel.Factory::class)
class MnemonicViewModel @AssistedInject constructor(
    @Assisted("sectionId") private val sectionId: Int,
    @Assisted("sectionName") private val sectionName: String,
    @Assisted("verses") private val verses: List<Verse>,
    @Assisted("assetNames") private val assetNames: List<String>,
    private val repository: MnemonicRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(
        MnemonicUiState(
            sectionId = sectionId,
            sectionName = sectionName
        )
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<MnemonicEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch(ioDispatcher) {
            val data = verses.mapIndexed { index, verse ->
                val assetName = assetNames.getOrNull(index) ?: ""
                VerseMnemonicData(
                    verse = verse,
                    userBitmap = repository.loadPicture(sectionId, index),
                    importedBitmap = repository.loadImportedPicture(sectionId, index),
                    defaultBitmap = repository.loadDefaultPicture(assetName),
                    chosen = repository.loadChoice(sectionId, index) ?: ChosenPicture.DEFAULT
                )
            }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(verseData = data) }
            }
        }
    }

    fun onEvent(event: MnemonicEvent, context: Context? = null) {
        when (event) {
            // List Screen
            is MnemonicEvent.SelectChoice -> {
                repository.saveChoice(sectionId, event.index, event.choice)
                updateChoiceInState(event.index, event.choice)
            }
            is MnemonicEvent.ShowZoom -> _state.update { it.copy(zoomedBitmap = event.bitmap) }
            MnemonicEvent.DownloadAll -> downloadAll()
            is MnemonicEvent.EnterDrawing -> {
                _state.update {
                    it.copy(
                        currentScreen = MnemonicScreen.DRAWING,
                        drawingState = DrawingState(
                            verseIndex = event.index,
                            isDirty = false
                        )
                    )
                }
            }
            is MnemonicEvent.EnterImport -> {
                _state.update {
                    it.copy(
                        currentScreen = MnemonicScreen.IMPORT,
                        importState = ImportState(verseIndex = event.index)
                    )
                }
                viewModelScope.launch { _effects.send(MnemonicEffect.LaunchImagePicker) }
            }
            MnemonicEvent.BackFromList -> {
                viewModelScope.launch { _effects.send(MnemonicEffect.FinishActivity) }
            }

            // Drawing Screen
            is MnemonicEvent.AddStroke -> {
                _state.update { s ->
                    val stroke = DrawingStroke(event.points, s.drawingState.selectedColor, s.drawingState.strokeWidthPx)
                    s.copy(
                        drawingState = s.drawingState.copy(
                            strokes = s.drawingState.strokes + stroke,
                            undoStack = s.drawingState.undoStack + stroke,
                            redoStack = emptyList(),
                            isDirty = true
                        )
                    )
                }
            }
            is MnemonicEvent.UpdateCurrentPoints -> {
                _state.update { it.copy(drawingState = it.drawingState.copy(currentPoints = event.points)) }
            }
            MnemonicEvent.Undo -> {
                _state.update { s ->
                    if (s.drawingState.undoStack.isEmpty()) return@update s
                    val last = s.drawingState.undoStack.last()
                    s.copy(
                        drawingState = s.drawingState.copy(
                            strokes = s.drawingState.strokes.dropLast(1),
                            undoStack = s.drawingState.undoStack.dropLast(1),
                            redoStack = s.drawingState.redoStack + last
                        )
                    )
                }
            }
            MnemonicEvent.Redo -> {
                _state.update { s ->
                    if (s.drawingState.redoStack.isEmpty()) return@update s
                    val last = s.drawingState.redoStack.last()
                    s.copy(
                        drawingState = s.drawingState.copy(
                            strokes = s.drawingState.strokes + last,
                            undoStack = s.drawingState.undoStack + last,
                            redoStack = s.drawingState.redoStack.dropLast(1)
                        )
                    )
                }
            }
            is MnemonicEvent.SetColor -> _state.update { it.copy(drawingState = it.drawingState.copy(selectedColor = event.color)) }
            is MnemonicEvent.SetStrokeWidth -> _state.update { it.copy(drawingState = it.drawingState.copy(strokeWidthPx = event.widthPx)) }
            is MnemonicEvent.SaveDrawing -> saveDrawing(event.canvasSize)
            MnemonicEvent.RequestBackFromDrawing -> {
                if (_state.value.drawingState.isDirty) {
                    _state.update { it.copy(drawingState = it.drawingState.copy(showExitDialog = true)) }
                } else {
                    _state.update { it.copy(currentScreen = MnemonicScreen.LIST) }
                }
            }
            MnemonicEvent.ConfirmDiscardDrawing -> {
                _state.update { it.copy(currentScreen = MnemonicScreen.LIST) }
            }
            MnemonicEvent.DismissExitDialog -> {
                _state.update { it.copy(drawingState = it.drawingState.copy(showExitDialog = false)) }
            }

            // Import Screen
            is MnemonicEvent.ImageSelected -> {
                if (context != null) {
                    handleImageUri(context, event.uri)
                }
            }
            is MnemonicEvent.UpdateTransform -> {
                _state.update { s ->
                    s.copy(
                        importState = s.importState.copy(
                            offsetX = s.importState.offsetX + event.pan.x,
                            offsetY = s.importState.offsetY + event.pan.y,
                            scale = (s.importState.scale * event.zoom).coerceIn(0.3f, 5f),
                            rotation = s.importState.rotation + event.rotation
                        )
                    )
                }
            }
            is MnemonicEvent.SetRotation -> {
                _state.update { it.copy(importState = it.importState.copy(rotation = event.degrees)) }
            }
            is MnemonicEvent.ConfirmImport -> confirmImport(event.canvasSize)
            MnemonicEvent.CancelImport -> {
                _state.update { it.copy(currentScreen = MnemonicScreen.LIST) }
            }
        }
    }

    private fun handleImageUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(ioDispatcher) {
            val bitmap = MnemonicImageHelper.loadBitmapFromUri(context, uri)
            withContext(Dispatchers.Main) {
                if (bitmap == null) {
                    viewModelScope.launch { _effects.send(MnemonicEffect.ShowToast(MnemonicMessage(R.string.image_import_load_failed))) }
                    _state.update { it.copy(currentScreen = MnemonicScreen.LIST) }
                } else {
                    _state.update { it.copy(importState = it.importState.copy(selectedBitmap = bitmap)) }
                }
            }
        }
    }

    private fun updateChoiceInState(index: Int, choice: ChosenPicture) {
        _state.update { s ->
            if (s.verseData.isEmpty()) return@update s
            val newData = s.verseData.toMutableList()
            newData[index] = newData[index].copy(chosen = choice)
            s.copy(verseData = newData)
        }
    }

    private fun downloadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isDownloading = true, downloadMessage = null) }
            val count = withContext(ioDispatcher) {
                repository.downloadAllImages(sectionId, verses, assetNames)
            }
            val message = if (count == 0) 
                MnemonicMessage(R.string.download_images_none)
            else 
                MnemonicMessage(R.string.download_images_done, listOf(count))
            
            _state.update { it.copy(isDownloading = false, downloadMessage = message) }
        }
    }

    private fun saveDrawing(canvasSize: androidx.compose.ui.unit.IntSize) {
        val s = _state.value
        val drawing = s.drawingState
        viewModelScope.launch(Dispatchers.Default) {
            val bg = s.verseData[drawing.verseIndex].userBitmap
            val bitmap = MnemonicImageHelper.renderDrawingToBitmap(
                background = bg,
                strokes = drawing.strokes,
                canvasWidth = canvasSize.width,
                canvasHeight = canvasSize.height
            )
            withContext(ioDispatcher) {
                repository.savePicture(sectionId, drawing.verseIndex, bitmap)
                repository.saveChoice(sectionId, drawing.verseIndex, ChosenPicture.USER)
            }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(currentScreen = MnemonicScreen.LIST) }
                loadAllData() // Refresh list
            }
        }
    }

    private fun confirmImport(canvasSize: androidx.compose.ui.unit.IntSize) {
        val s = _state.value
        val imp = s.importState
        if (imp.selectedBitmap == null) return

        viewModelScope.launch(Dispatchers.Default) {
            val cropped = MnemonicImageHelper.cropImageTo34Ratio(
                bitmap = imp.selectedBitmap,
                offsetX = imp.offsetX,
                offsetY = imp.offsetY,
                scale = imp.scale,
                rotation = imp.rotation,
                canvasWidth = canvasSize.width,
                canvasHeight = canvasSize.height
            )
            withContext(ioDispatcher) {
                repository.saveImportedPicture(sectionId, imp.verseIndex, cropped)
                repository.saveChoice(sectionId, imp.verseIndex, ChosenPicture.IMPORTED)
            }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(currentScreen = MnemonicScreen.LIST) }
                loadAllData()
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("sectionId") sectionId: Int,
            @Assisted("sectionName") sectionName: String,
            @Assisted("verses") verses: List<Verse>,
            @Assisted("assetNames") assetNames: List<String>
        ): MnemonicViewModel
    }
}
