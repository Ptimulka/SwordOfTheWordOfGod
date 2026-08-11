package io.github.ptimulka.miecz.screens.main

import android.os.Parcelable
import io.github.ptimulka.miecz.R
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Screen(val route: String, val resourceId: Int) : Parcelable {
    data object Levels : Screen("levels", R.string.levels_tab_caption)
    data object Random : Screen("random", R.string.random_tab_caption)
    data object Review : Screen("review", R.string.review_tab_caption)
    data object VerseGroups : Screen("verse_groups", R.string.verse_groups_tab_caption)
    data object Settings : Screen("settings", R.string.settings_tab_caption)
}

data class MainUiState(
    val selectedScreen: Screen = Screen.Levels,
    val isReviewTabVisible: Boolean = false
)
