package io.github.ptimulka.miecz.screens.main

import androidx.lifecycle.ViewModel
import io.github.ptimulka.miecz.repositories.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val progressRepo: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state = _state.asStateFlow()

    init {
        refreshVisibility()
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.RefreshTabVisibility -> {
                refreshVisibility()
            }
        }
    }

    private fun refreshVisibility() {
        val currentSection = progressRepo.getCurrentSection()
        _state.update { it.copy(isReviewTabVisible = currentSection >= 3) }
    }
}
