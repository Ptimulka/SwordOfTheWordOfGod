package io.github.ptimulka.miecz.screens.riddles.repeat_verse

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.calculateWordSimilarity
import io.github.ptimulka.miecz.helpers.normalizeVerseText
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

private const val SIMILARITY_THRESHOLD = 50f

data class RepeatVerseArgs(
    val sectionId: Int,
    val sectionVerses: List<Verse>,
    val assetNames: List<String>
)

@HiltViewModel(assistedFactory = RepeatVerseViewModel.Factory::class)
class RepeatVerseViewModel @AssistedInject constructor(
    @Assisted private val args: RepeatVerseArgs,
    private val progressRepository: ProgressRepository,
    private val mnemonicRepo: MnemonicRepository,
    private val ioDispatcher: CoroutineDispatcher
) : BaseRiddleViewModel<RepeatVerseUiState>(
    initialState = RepeatVerseUiState(),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId
) {

    init {
        updateRetentionState()
        loadAllThumbnails()
    }

    private fun loadAllThumbnails() {
        viewModelScope.launch(ioDispatcher) {
            val thumbs = args.sectionVerses.mapIndexed { index, verse ->
                val effectiveSectionId = verse.originalSectionId ?: args.sectionId
                val effectiveVerseIndex = verse.originalVerseIndex ?: index
                mnemonicRepo.loadActivePicture(effectiveSectionId, effectiveVerseIndex, args.assetNames.getOrNull(index))
            }
            launch(Dispatchers.Main) {
                _state.update { it.copy(thumbnails = thumbs) }
            }
        }
    }

    fun onEvent(event: RepeatVerseEvent) {
        when (event) {
            is RepeatVerseEvent.SelectVerse -> selectVerse(event.index)
            is RepeatVerseEvent.SetListening -> _state.update { it.copy(isListening = event.isListening) }
            is RepeatVerseEvent.UpdatePartialText -> _state.update { it.copy(partialText = event.text) }
            is RepeatVerseEvent.ProcessResult -> processResult(event.recognized)
            is RepeatVerseEvent.ShowZoom -> _state.update { it.copy(zoomIndex = event.index) }
        }
    }
    
    override fun checkAnswer() {}
    override fun updatePhase(state: RepeatVerseUiState, newPhase: RiddlePhase): RepeatVerseUiState {
        return state.copy(phase = newPhase)
    }
    override fun updateHintBitmap(state: RepeatVerseUiState, bitmap: Bitmap?): RepeatVerseUiState {
        return state.copy(hintBitmap = bitmap)
    }

    private fun selectVerse(index: Int?) {
        _state.update { 
            it.copy(
                selectedIndex = index,
                repeatCount = if (index != null) progressRepository.getVerseRepeatCountToday(args.sectionId, index) else 0,
                lastSimilarity = -1f,
                partialText = "",
                hintBitmap = if (index != null) it.thumbnails.getOrNull(index) else null
            )
        }
        updateRetentionState()
    }

    private fun processResult(recognized: String) {
        val idx = _state.value.selectedIndex ?: return
        val verse = args.sectionVerses.getOrNull(idx) ?: return

        val userWords = normalizeVerseText(recognized).split(' ').filter { it.isNotEmpty() }
        val verseWithSpaces = verse.text.replace("_", " ")
        val wordsWithOptional = normalizeVerseText(verseWithSpaces.replace("*", "")).split(' ').filter { it.isNotEmpty() }
        val wordsWithoutOptional = normalizeVerseText(verseWithSpaces.replace(Regex("\\*.*?\\*"), "")).split(' ').filter { it.isNotEmpty() }

        val similarity1 = calculateWordSimilarity(userWords, wordsWithOptional)
        val similarity2 = calculateWordSimilarity(userWords, wordsWithoutOptional)

        val similarity = if (similarity1 > similarity2) similarity1 else similarity2

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

    @AssistedFactory
    interface Factory {
        fun create(args: RepeatVerseArgs): RepeatVerseViewModel
    }
}
