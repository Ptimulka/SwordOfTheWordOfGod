package io.github.ptimulka.miecz.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ptimulka.miecz.GameActivity
import io.github.ptimulka.miecz.components.game.ConnectSuccessDialog
import io.github.ptimulka.miecz.components.game.GameExitDialog
import io.github.ptimulka.miecz.components.game.GameSuccessDialog
import io.github.ptimulka.miecz.components.game.GameSuccessForShieldsDialog
import io.github.ptimulka.miecz.components.game.GameTopBar
import io.github.ptimulka.miecz.components.game.NewRecordDialog
import io.github.ptimulka.miecz.components.game.NewStreakRecordDialog
import io.github.ptimulka.miecz.components.game.NoPlayingForShieldsDialog
import io.github.ptimulka.miecz.components.game.NoShieldsDialog
import io.github.ptimulka.miecz.components.game.RetentionGainedDialog
import io.github.ptimulka.miecz.components.game.RiddleRouter

@Composable
fun GameScreen(
    state: GameUiState,
    onEvent: (GameEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog management
    when (val dialog = state.dialogState) {
        GameDialogState.ExitConfirmation -> {
            GameExitDialog(
                onDismiss = { onEvent(GameEvent.CancelDialog) }
            ) { onEvent(GameEvent.ConfirmDialog) }
        }
        GameDialogState.NoShields -> {
            NoShieldsDialog { onEvent(GameEvent.ConfirmDialog) }
        }
        GameDialogState.NoMoreShieldsPlay -> {
            NoPlayingForShieldsDialog { onEvent(GameEvent.ConfirmDialog) }
        }
        is GameDialogState.NewRecord -> {
            NewRecordDialog(timeMs = dialog.timeMs) {
                onEvent(GameEvent.ConfirmDialog)
            }
        }
        is GameDialogState.NewStreak -> {
            NewStreakRecordDialog(streak = dialog.streak) {
                onEvent(GameEvent.ConfirmDialog)
            }
        }
        is GameDialogState.ConnectSuccess -> {
            ConnectSuccessDialog(
                timeMs = dialog.timeMs,
                messageRes = dialog.messageRes
            ) { onEvent(GameEvent.ConfirmDialog) }
        }
        GameDialogState.GenericSuccess -> {
            GameSuccessDialog { onEvent(GameEvent.ConfirmDialog) }
        }
        is GameDialogState.SuccessForShields -> {
            GameSuccessForShieldsDialog(receivedShields = dialog.reward) {
                onEvent(GameEvent.ConfirmDialog)
            }
        }
        is GameDialogState.RetentionGained -> {
            RetentionGainedDialog(gained = dialog.percentage) {
                onEvent(GameEvent.ConfirmDialog)
            }
        }
        GameDialogState.None -> {}
    }

    BackHandler(enabled = state.currentIndex > 0) {
        onEvent(GameEvent.RequestExit)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        GameTopBar(
            sectionId = state.sectionId,
            sectionName = state.sectionName,
            levelNumber = state.levelNumber,
            currentRiddleIndex = state.currentIndex,
            totalRiddles = state.riddles.size,
            shieldsCount = state.shieldsCount,
            showShields = state.isShieldsVisible,
            onExitClick = {
                if (state.currentIndex > 0) onEvent(GameEvent.RequestExit) else onEvent(GameEvent.ConfirmDialog)
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (state.currentIndex < state.riddles.size) {
                key(state.currentIndex) {
                    RiddleRouter(
                        riddle = state.riddles[state.currentIndex],
                        sectionId = state.sectionId,
                        riddleIndex = state.currentIndex,
                        sectionVerses = state.originalVerses,
                        assetNames = state.assetNames,
                        onSuccess = { elapsedMs -> onEvent(GameEvent.OnRiddleSuccess(elapsedMs)) },
                        onShieldLoss = { 
                            onEvent(GameEvent.OnShieldLoss)
                            // If we are in repeat-for-shields or out of shields, it's a fatal loss (don't show local result dialog)
                            state.sectionId == GameActivity.SECTION_ID_REPEAT_FOR_SHIELDS || (state.shieldsCount <= 1)
                        }
                    )
                }
            }
        }
    }
}
