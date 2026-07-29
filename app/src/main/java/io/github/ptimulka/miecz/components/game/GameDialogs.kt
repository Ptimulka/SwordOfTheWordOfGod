package io.github.ptimulka.miecz.components.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import io.github.ptimulka.miecz.helpers.formatTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import io.github.ptimulka.miecz.R

/**
 * Standard result dialog shared by all riddle screens.
 *
 * @param isCorrect whether the answer was correct (drives title text/color and body message).
 * @param onConfirm invoked on the OK button and, when [dismissable], on outside/back dismissal.
 * @param dismissable when false the dialog can only be closed via the OK button
 *        (used where dismissing outside would skip follow-up logic).
 * @param extraContent optional extra content rendered below the standard message,
 *        inside a vertically scrollable column (e.g. a word-diff comparison).
 */
@Composable
fun RiddleResultDialog(
    isCorrect: Boolean,
    onConfirm: () -> Unit,
    dismissable: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { if (dismissable) onConfirm() },
        properties = if (dismissable) DialogProperties()
            else DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                text = if (isCorrect) stringResource(R.string.correct_answer)
                    else stringResource(R.string.wrong_answer),
                color = if (isCorrect) colorResource(R.color.correct_answer_green) else Color.Red,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (isCorrect) stringResource(R.string.success_message)
                    else stringResource(R.string.failure_message)
                )
                extraContent?.invoke()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun GameExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_confirmation_title)) },
        text = { Text(stringResource(R.string.exit_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.yes_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.no_button)) }
        }
    )
}

@Composable
fun GameSuccessDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.all_riddles_completed_title),
                color = colorResource(R.color.correct_answer_green),
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(stringResource(R.string.all_riddles_completed_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun NoShieldsDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.game_over),
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.cant_play_more))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok_button))
            }
        }
    )
}

@Composable
fun GameSuccessForShieldsDialog(receivedShields: Int, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            val text =
                if(receivedShields == 2)
                    stringResource(R.string.all_riddles_completed_for_shields_2_title)
                else stringResource(R.string.all_riddles_completed_for_shields_title)
            Text(
                text = text,
                color = colorResource(R.color.correct_answer_green),
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(stringResource(R.string.all_riddles_completed_for_shields_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun NewRecordDialog(timeMs: Long, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.new_record_title),
                color = colorResource(R.color.record_gold),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.new_record_message, formatTime(timeMs)))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun ConnectSuccessDialog(timeMs: Long, messageRes: Int = R.string.connect_pairs_success_message, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.correct_answer),
                color = colorResource(R.color.correct_answer_green),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(stringResource(messageRes))
                Text(stringResource(R.string.connect_success_time, formatTime(timeMs)))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun NewStreakRecordDialog(streak: Int, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.new_streak_record_title),
                color = colorResource(R.color.record_gold),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.new_streak_record_message, streak))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun RetentionGainedDialog(gained: Int, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.retention_gained_title),
                color = colorResource(R.color.game_button_yellow_dark),
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(stringResource(R.string.retention_gained_message, gained)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok_button)) }
        }
    )
}

@Composable
fun NoPlayingForShieldsDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.game_over_for_shields),
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.cant_play_more_for_shields))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok_button))
            }
        }
    )
}