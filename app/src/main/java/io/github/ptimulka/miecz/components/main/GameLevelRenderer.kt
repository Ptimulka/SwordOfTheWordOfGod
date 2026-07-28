package io.github.ptimulka.miecz.components.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.helpers.formatTime
import io.github.ptimulka.miecz.helpers.launchGame
import io.github.ptimulka.miecz.repositories.UserProgressRepository

const val SPECIAL_CHALLENGE_RIDDLES_COUNT = 10

// Retention rays fan out from the heart, reaching one more level per +8% (from 4% → level 1).
private const val SECTION_LEVEL_COUNT = 12

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.renderSection(
    section: Section,
    isLocked: Boolean,
    riddlesOrder: List<List<RiddleType>>,
    userProgressRepository: UserProgressRepository,
    refreshTrigger: Int,
    onShowVerses: (Section) -> Unit,
    onDrawPictures: (Section) -> Unit,
    isShieldsEmpty: Boolean,
    onNoShieldsClick: () -> Unit
) {
    stickyHeader {
        SectionHeader(section = section, isLocked = isLocked)
    }

    item {
        SectionTopButtonsArea(
            section = section,
            isLocked = isLocked,
            userProgressRepository = userProgressRepository,
            refreshTrigger = refreshTrigger,
            onShowVerses = onShowVerses,
            onDrawPictures = onDrawPictures
        )
    }

    itemsIndexed(riddlesOrder) { index, riddleListForLevel ->
        val levelNumber = index + 1
        val areChallengesFinished = remember(refreshTrigger, section.id) {
            userProgressRepository.areSpecialChallengesFinished(section.id)
        }
        val isFinished = remember(refreshTrigger, section.id, levelNumber) {
            userProgressRepository.isLevelFinished(section.id, levelNumber)
        }
        val isPreviousFinished = remember(refreshTrigger, section.id, levelNumber) {
            if (levelNumber > 1) userProgressRepository.isLevelFinished(section.id, levelNumber - 1) else true
        }

        // Retention rays from the heart reach one more level per +8% (from 4% → level 1).
        val effectiveRetention = remember(refreshTrigger, section.id, areChallengesFinished) {
            if (areChallengesFinished) 100 else userProgressRepository.getRetention(section.id)
        }
        val raysReach = (if (effectiveRetention >= 4) ((effectiveRetention - 4) / 8) + 1 else 0)
            .coerceAtMost(SECTION_LEVEL_COUNT)

        // Two conditions to play a level: previous level finished AND retention rays reach it.
        val progressionUnlocked = isFinished || isPreviousFinished || areChallengesFinished
        val raysUnlocked = levelNumber <= raysReach
        val state = when {
            isLocked -> LevelButtonState.LOCKED
            isFinished -> LevelButtonState.FULL          // already-finished levels stay playable
            progressionUnlocked && raysUnlocked -> LevelButtonState.FULL
            progressionUnlocked != raysUnlocked -> LevelButtonState.HALF
            else -> LevelButtonState.LOCKED
        }
        val lockMessage = when {
            isLocked || state == LevelButtonState.FULL -> null
            !progressionUnlocked && !raysUnlocked -> stringResource(R.string.level_locked_need_both)
            !progressionUnlocked -> stringResource(R.string.level_locked_need_previous)
            else -> stringResource(R.string.level_locked_need_retention)
        }

        val context = LocalContext.current
        LevelItem(
            levelIndex = levelNumber,
            index = index,
            isFinished = isFinished,
            state = state,
            raysReach = raysReach,
            lockMessage = lockMessage,
            onLevelClick = {
                if (isShieldsEmpty) {
                    onNoShieldsClick()
                } else {
                    launchGame(context, section, riddleListForLevel, levelNumber)
                }
            }
        )
    }

    item {
        SpecialChallengesRow(
            section = section,
            isLocked = isLocked,
            lastLevelNumber = riddlesOrder.size,
            userProgressRepository = userProgressRepository,
            refreshTrigger = refreshTrigger,
            isShieldsEmpty = isShieldsEmpty,
            onNoShieldsClick = onNoShieldsClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.renderChooseNextSection(
    nextId: Int,
    isLocked: Boolean,
    onChooseClick: () -> Unit
) {
    stickyHeader {
        SectionHeader(
            section = Section(
                id = nextId,
                name = stringResource(R.string.choose_next_verses),
                verses = emptyList()
            ),
            isLocked = isLocked
        )
    }

    item {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.choose_next_verses_info),
                textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray,
                modifier = Modifier.padding(all = 16.dp)
            )
            Image(
                painter = painterResource(id = if (isLocked) R.drawable.buttonsquarelow else R.drawable.buttonsquare),
                contentDescription = stringResource(id = R.string.choose_next_verses),
                modifier = Modifier
                    .size(150.dp)
                    .then(if (!isLocked) Modifier.clickable { onChooseClick() } else Modifier),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.renderAllVersesLearnedSection(nextId: Int, isLocked: Boolean) {
    stickyHeader {
        SectionHeader(
            section = Section(
                id = nextId,
                name = if (isLocked) "" else stringResource(R.string.all_verses_learned_title),
                verses = emptyList()
            ),
            isLocked = isLocked,
            showNumber = false
        )
    }
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLocked)
                    stringResource(id = R.string.no_more_verses_message)
                else stringResource(id = R.string.all_verses_learned_message),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            if (!isLocked) {
                Image(
                    painter = painterResource(id = R.drawable.cup),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun SectionTopButtonsArea(
    section: Section,
    isLocked: Boolean,
    userProgressRepository: UserProgressRepository,
    refreshTrigger: Int,
    onShowVerses: (Section) -> Unit,
    onDrawPictures: (Section) -> Unit
) {
    val context = LocalContext.current
    val bestTimeParts = remember(refreshTrigger, section.id) {
        userProgressRepository.getBestTime(section.id, RiddleType.CONNECT_PARTS.name)
    }
    val bestTimePairs = remember(refreshTrigger, section.id) {
        userProgressRepository.getBestTime(section.id, RiddleType.CONNECT_PAIRS.name)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = if (isLocked) R.drawable.buttonsquarelow else R.drawable.buttonsquare),
                contentDescription = stringResource(id = R.string.show_verses),
                modifier = Modifier
                    .size(150.dp)
                    .then(if (!isLocked) Modifier.clickable { onShowVerses(section) } else Modifier),
                contentScale = ContentScale.Fit
            )
            Text(text = stringResource(id = R.string.show_verses_button_caption), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = if (isLocked) R.drawable.buttonpictureslow else R.drawable.buttonpictures),
                contentDescription = stringResource(id = R.string.draw_pictures),
                modifier = Modifier
                    .size(150.dp)
                    .then(if (!isLocked) Modifier.clickable { onDrawPictures(section) } else Modifier),
                contentScale = ContentScale.Fit
            )
            Text(text = stringResource(id = R.string.draw_pictures_button_caption), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color.Gray)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
        SquareGameButton(
            iconRes = if (isLocked) R.drawable.buttonsquarepartslow else R.drawable.buttonsquareparts,
            labelRes = R.string.connect_parts_caption,
            isLocked = isLocked,
            recordText = if (bestTimeParts >= 0) stringResource(R.string.best_time, formatTime(bestTimeParts)) else null,
            onClick = {
                launchGame(
                    context,
                    section,
                    listOf(RiddleType.CONNECT_PARTS),
                    levelNumber = 0
                )
            }
        )
        SquareGameButton(
            iconRes = if (isLocked) R.drawable.buttonsquarepairslow else R.drawable.buttonsquarepairs,
            labelRes = R.string.connect_pairs_caption,
            isLocked = isLocked,
            recordText = if (bestTimePairs >= 0) stringResource(R.string.best_time, formatTime(bestTimePairs)) else null,
            onClick = {
                launchGame(
                    context,
                    section,
                    listOf(RiddleType.CONNECT_PAIRS),
                    levelNumber = 0
                )
            }
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    val retention = remember(refreshTrigger, section.id) {
        userProgressRepository.getRetention(section.id)
    }
    val isSectionFinished = remember(refreshTrigger, section.id) {
        userProgressRepository.areSpecialChallengesFinished(section.id)
    }
    val dailyRetentionMaxed = remember(refreshTrigger, section.id) {
        val connectsDone =
            userProgressRepository.isConnectDoneToday(section.id, RiddleType.CONNECT_PARTS.name) &&
            userProgressRepository.isConnectDoneToday(section.id, RiddleType.CONNECT_PAIRS.name)
        val versesMaxed = section.verses.indices.all {
            userProgressRepository.retentionContributionForRepeats(
                userProgressRepository.getVerseRepeatCountToday(section.id, it)
            ) >= 3
        }
        connectsDone && versesMaxed
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SquareGameButton(
            iconRes = if (isLocked) R.drawable.buttonsquarespeaklow else R.drawable.buttonsquarespeak,
            labelRes = R.string.repeat_verse_button_caption,
            isLocked = isLocked,
            onClick = {
                launchGame(
                    context,
                    section,
                    listOf(RiddleType.REPEAT_VERSE),
                    levelNumber = 0
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        HeartRetentionButton(
            isLocked = isLocked,
            isFinished = isSectionFinished,
            retentionPercent = retention,
            dailyMaxReached = dailyRetentionMaxed,
            labelRes = R.string.retention_button_caption
        )
    }
}

@Composable
private fun SpecialChallengesRow(
    section: Section,
    isLocked: Boolean,
    lastLevelNumber: Int,
    userProgressRepository: UserProgressRepository,
    refreshTrigger: Int,
    isShieldsEmpty: Boolean,
    onNoShieldsClick: () -> Unit
) {
    val context = LocalContext.current

    // Hint shown only for the current section (reached but not yet finished) while its last
    // standard level is still not completed.
    val showUnlockHint = remember(refreshTrigger, section.id, isLocked, lastLevelNumber) {
        !isLocked &&
            !userProgressRepository.areSpecialChallengesFinished(section.id) &&
            !userProgressRepository.isLevelFinished(section.id, lastLevelNumber)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showUnlockHint) {
            Text(
                text = stringResource(R.string.challenges_unlock_hint),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ChallengeButton(
                isLocked = isLocked,
                isFinished = remember(
                    refreshTrigger,
                    section.id
                ) { userProgressRepository.isSiglaFinished(section.id) },
                iconRes = if (isLocked) R.drawable.buttonsiglalow else R.drawable.buttonsigla,
                labelRes = R.string.fill_whole_sigla_button_caption,
                onClick = {
                    if(isShieldsEmpty) {
                        onNoShieldsClick()
                    } else {
                        launchGame(
                            context,
                            section,
                            ArrayList(List(SPECIAL_CHALLENGE_RIDDLES_COUNT) { RiddleType.FILL_WHOLE_SIGLA }),
                            levelNumber = 0
                        )
                    }
                }
            )
            ChallengeButton(
                isLocked = isLocked,
                isFinished = remember(
                    refreshTrigger,
                    section.id
                ) { userProgressRepository.isVerseFinished(section.id) },
                iconRes = if (isLocked) R.drawable.buttonverselow else R.drawable.buttonverse,
                labelRes = R.string.fill_whole_verse_button_caption,
                onClick = {
                    if(isShieldsEmpty) {
                        onNoShieldsClick()
                    } else {
                        launchGame(
                            context,
                            section,
                            ArrayList(List(SPECIAL_CHALLENGE_RIDDLES_COUNT) { RiddleType.FILL_WHOLE_VERSE }),
                            levelNumber = 0
                        )
                    }
                }
            )
        }
    }
}