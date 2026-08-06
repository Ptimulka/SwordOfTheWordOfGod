package io.github.ptimulka.miecz.components.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.formatTime
import io.github.ptimulka.miecz.screens.settings.AchievementStats
import io.github.ptimulka.miecz.screens.settings.SettingsEvent
import io.github.ptimulka.miecz.screens.settings.SettingsUiState

@Composable
fun NotificationSection(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    val context = LocalContext.current
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
            checked = state.notificationsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.ToggleNotifications(it)) },
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
            color = if (state.notificationsEnabled) Color.Unspecified else Color.Gray
        )
        Button(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onEvent(SettingsEvent.UpdateNotificationTime(hour, minute)) },
                    state.notificationHour,
                    state.notificationMinute,
                    true
                ).show()
            },
            enabled = state.notificationsEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.game_button_yellow_dark),
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text(
                text = "%02d:%02d".format(state.notificationHour, state.notificationMinute),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AchievementsSection(stats: AchievementStats) {
    Text(
        text = stringResource(R.string.settings_achievements_section),
        fontSize = 13.sp,
        color = colorResource(R.color.game_button_yellow_dark),
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    val noRecord = stringResource(R.string.settings_no_achievement)

    AchievementRow {
        AchievementCard(
            iconRes = R.drawable.buttonsquareparts,
            label = stringResource(R.string.settings_best_connect_parts),
            value = stats.bestParts?.let { formatTime(it.timeMs) } ?: noRecord,
            secondary = stats.bestParts?.sectionName
        )
        AchievementCard(
            iconRes = R.drawable.buttonsquarepairs,
            label = stringResource(R.string.settings_best_connect_pairs),
            value = stats.bestPairs?.let { formatTime(it.timeMs) } ?: noRecord,
            secondary = stats.bestPairs?.sectionName
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    AchievementRow {
        AchievementCard(
            iconRes = R.drawable.buttonhigh,
            label = stringResource(R.string.settings_level_streak_current),
            value = stats.levelStreak.toString()
        )
        AchievementCard(
            iconRes = R.drawable.cup,
            label = stringResource(R.string.settings_level_streak_best),
            value = stats.bestLevelStreak.toString()
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    AchievementRow {
        AchievementCard(
            iconRes = R.drawable.buttonlamp,
            label = stringResource(R.string.settings_day_streak_current),
            value = stats.dayStreak.toString()
        )
        AchievementCard(
            iconRes = R.drawable.cup,
            label = stringResource(R.string.settings_day_streak_best),
            value = stats.bestDayStreak.toString()
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    AchievementRow {
        AchievementCard(
            iconRes = R.drawable.buttonverse,
            label = stringResource(R.string.settings_learned_verses),
            value = stats.learnedVerses.toString()
        )
        AchievementCard(
            iconRes = R.drawable.buttonhigh,
            label = stringResource(R.string.settings_finished_levels),
            value = stats.finishedLevels.toString()
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    AchievementRow {
        AchievementCard(
            iconRes = R.drawable.ic_repeat_achievement,
            label = stringResource(R.string.settings_repeated_verses),
            value = stats.reviewedVerses.toString()
        )
        AchievementCard(
            iconRes = R.drawable.buttonsquarespeak,
            label = stringResource(R.string.settings_aloud_repeated_verses),
            value = stats.aloudRepeats.toString()
        )
    }
}

@Composable
fun InfoSection(appVersion: String) {
    Text(
        text = stringResource(R.string.settings_info_section),
        fontSize = 13.sp,
        color = colorResource(R.color.game_button_yellow_dark),
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    val linkColor = colorResource(R.color.game_button_yellow_dark)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
    )

    val githubUrl = stringResource(R.string.settings_github_url)
    val githubText = buildAnnotatedString {
        append(stringResource(R.string.settings_info_github))
        append(" ")
        withLink(LinkAnnotation.Url(githubUrl, linkStyle)) {
            append(stringResource(R.string.settings_info_github_link))
        }
        append(".")
    }
    Text(text = githubText, fontSize = 14.sp)

    Spacer(modifier = Modifier.height(12.dp))

    val privacyUrl = stringResource(R.string.settings_privacy_policy_url)
    val privacyText = buildAnnotatedString {
        withLink(LinkAnnotation.Url(privacyUrl, linkStyle)) {
            append(stringResource(R.string.settings_privacy_policy))
        }
    }
    Text(text = privacyText, fontSize = 14.sp)

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.app_name),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = colorResource(R.color.game_button_yellow_dark)
    )

    Text(
        text = stringResource(R.string.settings_version, appVersion),
        fontSize = 13.sp,
        color = Color.Gray
    )
}

@Composable
fun ResetSection(onRequestReset: () -> Unit) {
    Text(
        text = stringResource(R.string.settings_reset_section),
        fontSize = 13.sp,
        color = colorResource(R.color.game_button_yellow_dark),
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onRequestReset,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text(
            text = stringResource(R.string.settings_reset_progress),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
