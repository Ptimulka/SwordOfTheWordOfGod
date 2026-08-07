package io.github.ptimulka.miecz.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.*

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
                onDismiss = { onEvent(GameEvent.CancelDialog) },
                onConfirm = { onEvent(GameEvent.ConfirmDialog) }
            )
        }
        GameDialogState.NoShields -> {
            NoShieldsDialog(onConfirm = { onEvent(GameEvent.ConfirmDialog) })
        }
        GameDialogState.NoMoreShieldsPlay -> {
            NoPlayingForShieldsDialog(onConfirm = { onEvent(GameEvent.ConfirmDialog) })
        }
        is GameDialogState.NewRecord -> {
            NewRecordDialog(
                timeMs = dialog.timeMs,
                onConfirm = { onEvent(GameEvent.ConfirmDialog) }
            )
        }
        is GameDialogState.NewStreak -> {
            NewStreakRecordDialog(
                streak = dialog.streak,
                onConfirm = { onEvent(GameEvent.ConfirmDialog) }
            )
        }
        is GameDialogState.ConnectSuccess -> {
            ConnectSuccessDialog(
                timeMs = dialog.timeMs,
                messageRes = dialog.messageRes,
                onConfirm = { onEvent(GameEvent.ConfirmDialog) }
            )
        }
        GameDialogState.GenericSuccess -> {
            GameSuccessDialog(onConfirm = { onEvent(GameEvent.ConfirmDialog) })
        }
        is GameDialogState.SuccessForShields -> {
            GameSuccessForShieldsDialog(
                receivedShields = dialog.reward,
                onConfirm = { onEvent(GameEvent.ConfirmDialog) }
            )
        }
        is GameDialogState.RetentionGained -> {
            RetentionGainedDialog(
                gained = dialog.percentage,
                onConfirm = { onEvent(GameEvent.ConfirmDialog) }
            )
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

        // Content Area - Displays the current riddle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (state.currentIndex < state.riddles.size) {
                RiddleRouter(
                    riddle = state.riddles[state.currentIndex],
                    sectionId = state.sectionId,
                    sectionVerses = state.originalVerses,
                    assetNames = state.assetNames,
                    onSuccess = { elapsedMs -> onEvent(GameEvent.OnRiddleSuccess(elapsedMs)) },
                    onShieldLoss = { 
                        onEvent(GameEvent.OnShieldLoss)
                        // If we are in repeat-for-shields or out of shields, it's a fatal loss (don't show local result dialog)
                        state.sectionId == io.github.ptimulka.miecz.GameActivity.SECTION_ID_REPEAT_FOR_SHIELDS || state.shieldsCount <= 1
                    }
                )
            }
        }
    }
}
