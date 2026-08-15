package io.github.ptimulka.miecz.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_FOR_SHIELDS
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_NORMAL
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.helpers.ReviewVerseProvider
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val progressRepo: ProgressRepository,
    private val sectionRepo: SectionRepository,
    private val groupsRepo: VersesGroupsRepository,
    @param:Named("reviewSectionName") private val sectionName: String
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ReviewEffect>()
    val effects = _effects.receiveAsFlow()

    private var refreshJob: Job? = null

    init {
        loadData()
        startRefreshLoop()
    }

    fun onEvent(event: ReviewEvent) {
        when (event) {
            ReviewEvent.OnResume -> loadData()
            is ReviewEvent.SetCount -> _state.update { it.copy(selectedCount = event.count) }.also { updateReward() }
            is ReviewEvent.SetPlayForShields -> _state.update { 
                val canPlay = it.shieldsCount < UserProgressRepository.MAX_SHIELDS
                it.copy(playForShields = if (canPlay) event.play else false) 
            }
            ReviewEvent.StartReview -> startReview()
        }
    }

    private fun loadData() {
        val currentSectionId = progressRepo.getCurrentSection()
        val knownVerses = ReviewVerseProvider.loadKnownVerses(currentSectionId, sectionRepo, progressRepo, groupsRepo)
        val shields = progressRepo.getShieldsCount()
        
        _state.update { 
            it.copy(
                knownVersesSections = knownVerses,
                shieldsCount = shields,
                playForShields = if (shields >= UserProgressRepository.MAX_SHIELDS) false else it.playForShields
            )
        }
        updateReward()
    }

    private fun updateReward() {
        val s = _state.value
        val rewardCount = if (s.selectedCount == 10) 2 else 1
        val maxPossibleReward = (UserProgressRepository.MAX_SHIELDS - s.shieldsCount).coerceAtLeast(0).coerceAtMost(rewardCount)
        _state.update { it.copy(maxPossibleReward = maxPossibleReward) }
    }

    private fun startRefreshLoop() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(10000)
                progressRepo.refreshShields()
                loadData()
            }
        }
    }

    private fun startReview() {
        val s = _state.value
        if (!s.isReviewAvailable) return

        val riddleTypes = ReviewVerseProvider.getShuffledRiddleTypes(s.selectedCount)
            .map { RiddleType.valueOf(it) }
        val (verses, assets) = ReviewVerseProvider.pickReviewVerses(
            s.knownVersesSections,
            s.selectedCount,
            progressRepo.getRepeatSectionIndex(),
            progressRepo.getRepeatVerseIndex()
        )

        val sectionId = if (s.playForShields) SECTION_ID_REPEAT_FOR_SHIELDS else SECTION_ID_REPEAT_NORMAL
        
        viewModelScope.launch {
            _effects.send(ReviewEffect.LaunchReview(
                section = Section(
                    id = sectionId,
                    name = sectionName,
                    verses = verses,
                    assetNames = assets
                ),
                riddleTypes = riddleTypes
            ))
        }
    }
}
