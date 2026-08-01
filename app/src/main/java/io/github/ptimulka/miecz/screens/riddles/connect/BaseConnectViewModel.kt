package io.github.ptimulka.miecz.screens.riddles.connect

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseConnectViewModel(
    private val hintBitmaps: Map<Int, Bitmap>
) : ViewModel() {

    protected val _state = MutableStateFlow(ConnectUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ConnectEffect>()
    val effects = _effects.receiveAsFlow()

    private var startTimeMs: Long = System.currentTimeMillis()

    fun onEvent(event: ConnectEvent) {
        when (event) {
            is ConnectEvent.SelectLeft -> selectLeft(event.item)
            is ConnectEvent.SelectRight -> selectRight(event.item)
            ConnectEvent.DismissImage -> dismissImage()
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
            delay(300) // Match animation time
            
            val bitmap = hintBitmaps[id]
            if (bitmap != null) {
                _state.update { it.copy(previewBitmap = bitmap, justMatchedId = null) }
            } else {
                removeMatched(id)
            }
        }
    }

    private fun dismissImage() {
        val s = _state.value
        val id = s.selectedLeft?.id ?: return // The matched ID
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
            viewModelScope.launch { _effects.send(ConnectEffect.Success(elapsed)) }
        }
    }

    private fun handleError(left: ConnectItem, right: ConnectItem) {
        viewModelScope.launch {
            _state.update { it.copy(wrongPair = left to right, isLocked = true) }
            delay(2000) // Lockout time
            _state.update { it.copy(wrongPair = null, isLocked = false, selectedLeft = null, selectedRight = null) }
        }
    }
}
