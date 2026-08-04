package io.github.ptimulka.miecz.screens.groups

sealed interface VerseGroupsEvent {
    data class UpdateSearch(val query: String) : VerseGroupsEvent
    data object ClearSearch : VerseGroupsEvent
    data class ToggleExpand(val id: Int) : VerseGroupsEvent
    data class ShowPreview(val assetName: String) : VerseGroupsEvent
    data object DismissPreview : VerseGroupsEvent
}
