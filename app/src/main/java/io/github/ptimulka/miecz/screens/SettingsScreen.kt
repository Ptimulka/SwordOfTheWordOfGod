package io.github.ptimulka.miecz.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.helpers.NotificationHelper
import io.github.ptimulka.miecz.helpers.formatTime
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.SettingsRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository

@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val userProgressRepository = remember { UserProgressRepository(context) }
    var notificationsEnabled by remember { mutableStateOf(repository.isNotificationsEnabled()) }
    var notificationHour by remember { mutableIntStateOf(repository.getNotificationHour()) }
    var notificationMinute by remember { mutableIntStateOf(repository.getNotificationMinute()) }

    // Bumped after a progress reset so every achievement value is re-read
    var resetTrigger by remember { mutableIntStateOf(0) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetFinalConfirm by remember { mutableStateOf(false) }

    val bestParts = remember(resetTrigger) { userProgressRepository.getBestTimeOverall(RiddleType.CONNECT_PARTS.name, context) }
    val bestPairs = remember(resetTrigger) { userProgressRepository.getBestTimeOverall(RiddleType.CONNECT_PAIRS.name, context) }
    val levelStreak = remember(resetTrigger) { userProgressRepository.getLevelStreak() }
    val bestLevelStreak = remember(resetTrigger) {
        userProgressRepository.reconcileBestStreaks()
        userProgressRepository.getBestLevelStreak()
    }
    val dayStreak = remember(resetTrigger) { userProgressRepository.getCurrentDayStreak() }
    val bestDayStreak = remember(resetTrigger) { userProgressRepository.getBestDayStreak() }
    val learnedVerses = remember(resetTrigger) { countLearnedVerses(context, userProgressRepository) }
    val reviewedVerses = remember(resetTrigger) { userProgressRepository.getTotalReviewedVerses() }
    val aloudRepeats = remember(resetTrigger) { userProgressRepository.getTotalAloudRepeats() }
    val finishedLevels = remember(resetTrigger) { userProgressRepository.getFinishedLevelsCount() }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.reset_progress_title),
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.reset_progress_message)) },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; showResetFinalConfirm = true }) {
                    Text(stringResource(R.string.reset_progress_confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (showResetFinalConfirm) {
        AlertDialog(
            onDismissRequest = { showResetFinalConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.reset_progress_final_title),
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.reset_progress_final_message)) },
            confirmButton = {
                TextButton(onClick = {
                    userProgressRepository.clearAllProgress()
                    MnemonicPicturesRepository(context).clearAllPictures()
                    showResetFinalConfirm = false
                    resetTrigger++
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.reset_progress_done),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }) {
                    Text(stringResource(R.string.reset_progress_final_confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetFinalConfirm = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // Top bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(R.color.game_button_yellow_dark),
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_notifications_section),
                fontSize = 13.sp,
                color = colorResource(R.color.game_button_yellow_dark),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.settings_daily_reminder), fontSize = 16.sp)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        notificationsEnabled = enabled
                        repository.setNotificationsEnabled(enabled)
                        if (enabled) {
                            NotificationHelper.scheduleDailyNotification(context, notificationHour, notificationMinute)
                        } else {
                            NotificationHelper.cancelDailyNotification(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorResource(R.color.game_button_yellow_dark),
                        checkedTrackColor = colorResource(R.color.game_button_yellow_light)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_notification_time),
                    fontSize = 16.sp,
                    color = if (notificationsEnabled) Color.Unspecified else Color.Gray
                )
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                notificationHour = hour
                                notificationMinute = minute
                                repository.setNotificationTime(hour, minute)
                                NotificationHelper.scheduleDailyNotification(context, hour, minute)
                            },
                            notificationHour,
                            notificationMinute,
                            true
                        ).show()
                    },
                    enabled = notificationsEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.game_button_yellow_dark),
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Text(
                        text = "%02d:%02d".format(notificationHour, notificationMinute),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_achievements_section),
                fontSize = 13.sp,
                color = colorResource(R.color.game_button_yellow_dark),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val noRecord = stringResource(R.string.settings_no_achievement)

            // 3 rows × 2 achievement cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCard(
                    iconRes = R.drawable.buttonsquareparts,
                    label = stringResource(R.string.settings_best_connect_parts),
                    value = bestParts?.let { formatTime(it.timeMs) } ?: noRecord,
                    secondary = bestParts?.sectionName
                )
                AchievementCard(
                    iconRes = R.drawable.buttonsquarepairs,
                    label = stringResource(R.string.settings_best_connect_pairs),
                    value = bestPairs?.let { formatTime(it.timeMs) } ?: noRecord,
                    secondary = bestPairs?.sectionName
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCard(
                    iconRes = R.drawable.buttonhigh,
                    label = stringResource(R.string.settings_level_streak_current),
                    value = levelStreak.toString()
                )
                AchievementCard(
                    iconRes = R.drawable.cup,
                    label = stringResource(R.string.settings_level_streak_best),
                    value = bestLevelStreak.toString()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCard(
                    iconRes = R.drawable.buttonlamp,
                    label = stringResource(R.string.settings_day_streak_current),
                    value = dayStreak.toString()
                )
                AchievementCard(
                    iconRes = R.drawable.cup,
                    label = stringResource(R.string.settings_day_streak_best),
                    value = bestDayStreak.toString()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCard(
                    iconRes = R.drawable.buttonverse,
                    label = stringResource(R.string.settings_learned_verses),
                    value = learnedVerses.toString()
                )
                AchievementCard(
                    iconRes = R.drawable.buttonhigh,
                    label = stringResource(R.string.settings_finished_levels),
                    value = finishedLevels.toString()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCard(
                    iconRes = R.drawable.ic_repeat_achievement,
                    label = stringResource(R.string.settings_repeated_verses),
                    value = reviewedVerses.toString()
                )
                AchievementCard(
                    iconRes = R.drawable.buttonsquarespeak,
                    label = stringResource(R.string.settings_aloud_repeated_verses),
                    value = aloudRepeats.toString()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_info_section),
                fontSize = 13.sp,
                color = colorResource(R.color.game_button_yellow_dark),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = stringResource(R.string.settings_info_github), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(4.dp))

            val uriHandler = LocalUriHandler.current
            val githubUrl = stringResource(R.string.settings_github_url)
            Text(
                text = githubUrl,
                fontSize = 14.sp,
                color = colorResource(R.color.game_button_yellow_dark),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(githubUrl) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_reset_section),
                fontSize = 13.sp,
                color = colorResource(R.color.game_button_yellow_dark),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = stringResource(R.string.settings_reset_progress),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Sum of verses across all sections whose special challenges are finished. */
private fun countLearnedVerses(
    context: android.content.Context,
    userProgressRepository: UserProgressRepository
): Int {
    var total = 0
    SectionRepository(context).loadInitialSections().forEach { section ->
        if (userProgressRepository.areSpecialChallengesFinished(section.id)) {
            total += section.verses.size
        }
    }
    val groups = VersesGroupsRepository(context).loadVerseGroups()
    val customCount = userProgressRepository.getCustomSectionsCount()
    for (i in 1..customCount) {
        val sectionId = 5 + i - 1
        if (userProgressRepository.areSpecialChallengesFinished(sectionId)) {
            userProgressRepository.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                val g1 = groups.find { it.id == id1 }
                val g2 = groups.find { it.id == id2 }
                if (g1 != null && g2 != null) {
                    total += g1.verses.size + g2.verses.size
                }
            }
        }
    }
    return total
}

@Composable
private fun RowScope.AchievementCard(
    iconRes: Int,
    label: String,
    value: String,
    secondary: String? = null
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(colorResource(R.color.game_button_yellow_light).copy(alpha = 0.18f))
            .border(1.dp, colorResource(R.color.game_button_yellow_light), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(46.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            color = colorResource(R.color.record_gold),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (secondary != null) {
            Text(
                text = secondary,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
