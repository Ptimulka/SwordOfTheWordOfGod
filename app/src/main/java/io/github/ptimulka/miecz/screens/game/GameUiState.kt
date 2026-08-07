package io.github.ptimulka.miecz.screens.game

import io.github.ptimulka.miecz.data.Riddle
import io.github.ptimulka.miecz.data.Verse

sealed interface GameDialogState {
    data object None : GameDialogState
    data object ExitConfirmation : GameDialogState
    data object NoShields : GameDialogState
    data object NoMoreShieldsPlay : GameDialogState
    data class NewRecord(val timeMs: Long) : GameDialogState
    data class NewStreak(val streak: Int) : GameDialogState
    data class ConnectSuccess(val timeMs: Long, val messageRes: Int) : GameDialogState
    data object GenericSuccess : GameDialogState
    data class RetentionGained(val percentage: Int) : GameDialogState
    data class SuccessForShields(val reward: Int) : GameDialogState
}

data class GameUiState(
    val riddles: List<Riddle> = emptyList(),
    val currentIndex: Int = 0,
    val shieldsCount: Int = 5,
    val isShieldsVisible: Boolean = true,
    val dialogState: GameDialogState = GameDialogState.None,
    val isGameOver: Boolean = false,
    val sectionId: Int = 0,
    val sectionName: String = "",
    val levelNumber: Int = 0,
    val assetNames: List<String> = emptyList(),
    val originalVerses: List<Verse> = emptyList()
)
