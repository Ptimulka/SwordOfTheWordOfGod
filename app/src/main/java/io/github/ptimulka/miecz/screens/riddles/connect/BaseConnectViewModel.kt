package io.github.ptimulka.miecz.screens.riddles.connect

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.Constants
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseConnectViewModel(
    protected val verses: List<Verse>,
    protected val sectionId: Int,
    protected val assetNames: List<String>,
    protected val mnemonicRepo: MnemonicRepository
) : ViewModel() {

    protected val _state = MutableStateFlow(ConnectUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<RiddleEffect>()
    val effects = _effects.receiveAsFlow()

    private var startTimeMs: Long = System.currentTimeMillis()

    init {
        loadHintBitmaps()
    }

    private fun loadHintBitmaps() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bitmaps = mutableMapOf<Int, Bitmap>()
            verses.forEachIndexed { index, verse ->
                val assetName = assetNames.getOrNull(index)
                mnemonicRepo.loadActivePicture(sectionId, index, assetName)?.let {
                    bitmaps[verse.hashCode()] = it
                }
            }
            _state.update { it.copy(buttonBitmaps = bitmaps) }
        }
    }

    fun onEvent(event: ConnectEvent) {
        when (event) {
            is ConnectEvent.SelectLeft -> selectLeft(event.item)
            is ConnectEvent.SelectRight -> selectRight(event.item)
            ConnectEvent.DismissHint -> dismissImage()
        }
    }

    private fun selectLeft(item: ConnectItem) {
        if (_state.value.isLocked) return
        
        val s = _state.value
        if (s.selectedLeft == item) {
            _state.update { it.copy(selectedLeft = null, wrongPair = null) }
            return
        }

        _state.update { it.copy(selectedLeft = item, wrongPair = null) }
        checkMatch()
    }

    private fun selectRight(item: ConnectItem) {
        if (_state.value.isLocked) return

        val s = _state.value
        if (s.selectedRight == item) {
            _state.update { it.copy(selectedRight = null, wrongPair = null) }
            return
        }

        _state.update { it.copy(selectedRight = item, wrongPair = null) }
        checkMatch()
    }

    private fun checkMatch() {
        val s = _state.value
        val left = s.selectedLeft ?: return
        val right = s.selectedRight ?: return

        if (left.id == right.id) {
            handleMatch(left.id)
        } else {
            handleError(left, right)
        }
    }

    private fun handleMatch(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(justMatchedId = id) }
            delay(Constants.MATCH_ANIMATION_MS)
            
            val bitmap = _state.value.buttonBitmaps[id]
            if (bitmap != null) {
                _state.update { it.copy(previewBitmap = bitmap, justMatchedId = null) }
            } else {
                removeMatched(id)
            }
        }
    }

    private fun dismissImage() {
        val s = _state.value
        val id = s.selectedLeft?.id ?: s.selectedRight?.id ?: return
        _state.update { it.copy(previewBitmap = null) }
        removeMatched(id)
    }

    private fun removeMatched(id: Int) {
        _state.update { 
            val nextLeft = it.leftItems.filter { item -> item.id != id }
            val nextRight = it.rightItems.filter { item -> item.id != id }
            
            val isFinished = nextLeft.isEmpty()
            val elapsed = if (isFinished) System.currentTimeMillis() - startTimeMs else -1L
            
            it.copy(
                leftItems = nextLeft,
                rightItems = nextRight,
                selectedLeft = null,
                selectedRight = null,
                justMatchedId = null,
                completedElapsedMs = elapsed
            )
        }
        
        val elapsed = _state.value.completedElapsedMs
        if (elapsed >= 0) {
            viewModelScope.launch { _effects.send(RiddleEffect.Success(elapsed)) }
        }
    }

    private fun handleError(left: ConnectItem, right: ConnectItem) {
        viewModelScope.launch {
            _state.update { it.copy(wrongPair = left to right, isLocked = true) }
            delay(Constants.WRONG_PAIR_LOCKOUT_MS)
            _state.update { it.copy(wrongPair = null, isLocked = false, selectedLeft = null, selectedRight = null) }
        }
    }
}
