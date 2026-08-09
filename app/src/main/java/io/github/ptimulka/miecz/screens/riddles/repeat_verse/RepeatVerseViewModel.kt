package io.github.ptimulka.miecz.screens.riddles.repeat_verse

import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.calculateWordSimilarity
import io.github.ptimulka.miecz.helpers.normalizeVerseText
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEvent
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SIMILARITY_THRESHOLD = 50f

data class RepeatVerseArgs(
    val sectionId: Int,
    val sectionVerses: List<Verse>,
    val assetNames: List<String>
)

class RepeatVerseViewModel(
    private val args: RepeatVerseArgs,
    private val progressRepository: ProgressRepository,
    private val mnemonicRepo: MnemonicRepository
) : BaseRiddleViewModel<RepeatVerseUiState>(
    initialState = RepeatVerseUiState(),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId
) {

    init {
        updateRetentionState()
    }

    fun onEvent(event: RepeatVerseEvent) {
        when (event) {
            is RepeatVerseEvent.SelectVerse -> selectVerse(event.index)
            is RepeatVerseEvent.SetListening -> _state.update { it.copy(isListening = event.isListening) }
            is RepeatVerseEvent.UpdatePartialText -> _state.update { it.copy(partialText = event.text) }
            is RepeatVerseEvent.ProcessResult -> processResult(event.recognized)
            is RepeatVerseEvent.ShowZoom -> _state.update { it.copy(zoomIndex = event.index) }
            RepeatVerseEvent.RequestPermission -> viewModelScope.launch { 
                // Handled in screen
            }
        }
    }
    
    override fun checkAnswer() {}
    override fun updatePhase(state: RepeatVerseUiState, newPhase: RiddlePhase): RepeatVerseUiState {
        return state.copy(phase = newPhase)
    }
    override fun updateHintBitmap(state: RepeatVerseUiState, bitmap: android.graphics.Bitmap?): RepeatVerseUiState {
        return state.copy(hintBitmap = bitmap)
    }

    private fun selectVerse(index: Int?) {
        _state.update { 
            it.copy(
                selectedIndex = index,
                repeatCount = if (index != null) progressRepository.getVerseRepeatCountToday(args.sectionId, index) else 0,
                lastSimilarity = -1f,
                partialText = ""
            )
        }
        updateRetentionState()
        
        // Load the bitmap for the thumbnail zoom
        if (index != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val bitmap = mnemonicRepo.loadActivePicture(args.sectionId, index, args.assetNames.getOrNull(index))
                _state.update { it.copy(hintBitmap = bitmap) }
            }
        } else {
            _state.update { it.copy(hintBitmap = null) }
        }
    }

    private fun processResult(recognized: String) {
        val idx = _state.value.selectedIndex ?: return
        val verse = args.sectionVerses.getOrNull(idx) ?: return

        val userWords = normalizeVerseText(recognized).split(' ').filter { it.isNotEmpty() }
        val cleanVerse = verse.text.replace("_", " ").replace("*", "")
        val verseWords = normalizeVerseText(cleanVerse).split(' ').filter { it.isNotEmpty() }
        val similarity = calculateWordSimilarity(userWords, verseWords)

        _state.update { it.copy(lastSimilarity = similarity, partialText = "", isListening = false) }

        if (similarity >= SIMILARITY_THRESHOLD) {
            val newCount = progressRepository.incrementVerseRepeatToday(args.sectionId, idx)
            progressRepository.incrementTotalAloudRepeats()
            progressRepository.updateDayStreak()
            
            _state.update { it.copy(repeatCount = newCount) }
            updateRetentionState()
        }
    }

    private fun updateRetentionState() {
        val count = _state.value.repeatCount
        val verseRetention = progressRepository.retentionContributionForRepeats(count)
        
        val sectionRetention = args.sectionVerses.indices.sumOf {
            progressRepository.retentionContributionForRepeats(
                progressRepository.getVerseRepeatCountToday(args.sectionId, it)
            )
        }
        
        val finished = progressRepository.areSpecialChallengesFinished(args.sectionId)
        
        val maxed = args.sectionVerses.indices.filter {
            progressRepository.getVerseRepeatCountToday(args.sectionId, it) >= 10
        }.toSet()

        _state.update { 
            it.copy(
                verseRetentionToday = verseRetention,
                sectionRetentionToday = sectionRetention,
                isSectionFinished = finished,
                maxedIndices = maxed
            )
        }
    }
}
