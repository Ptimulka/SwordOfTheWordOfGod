package io.github.ptimulka.miecz.screens.main

import io.github.ptimulka.miecz.data.Section

sealed interface GameLevelEvent {
    data object OnResume : GameLevelEvent
    data object ToggleShieldInfo : GameLevelEvent
    data object ToggleLampInfo : GameLevelEvent
    data object DismissUnlockedDialog : GameLevelEvent
    data object NavigateToChooseGroups : GameLevelEvent
    data object CancelChooseGroups : GameLevelEvent
    data object ConfirmGroupSelection : GameLevelEvent
    data class ShowSectionVerses(val section: Section?) : GameLevelEvent
    data class ToggleGroupSelection(val groupId: Int) : GameLevelEvent
    data object ClearLampAnimation : GameLevelEvent
}
