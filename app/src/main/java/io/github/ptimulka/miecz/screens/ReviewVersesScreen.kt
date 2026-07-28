package io.github.ptimulka.miecz.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.GameActivity
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_FOR_SHIELDS
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_NORMAL
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.delay

@Composable
fun ReviewVersesScreen(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val userProgressRepository = remember { UserProgressRepository(context) }
    val sectionRepository = remember { SectionRepository(context) }
    val versesGroupsRepository = remember { VersesGroupsRepository(context) }

    val currentSectionId = userProgressRepository.getCurrentSection()

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val shieldsCount = remember(refreshTrigger) { userProgressRepository.getShieldsCount() }

    val options = listOf(5, 8, 10)
    var selectedCount by rememberSaveable { mutableIntStateOf(10) }
    var playForShields by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            userProgressRepository.refreshShields()
            refreshTrigger++
            if (userProgressRepository.getShieldsCount() >= UserProgressRepository.MAX_SHIELDS) {
                playForShields = false
            }
            delay(10000) // Refresh each 10 seconds
        }
    }

    val knownVersesSections = remember(currentSectionId) {
        val sectionsList = mutableListOf<Pair<List<Verse>, List<String>>>()

        val baseSections = listOf(
            R.raw.section01, R.raw.section02, R.raw.section03, R.raw.section04
        )
        baseSections.forEachIndexed { index, resId ->
            val sectionId = index + 1
            if (sectionId < currentSectionId) {
                sectionRepository.loadSection(resId)?.let {
                    if (it.verses.isNotEmpty()) sectionsList.add(it.verses to it.assetNames)
                }
            }
        }

        val customCount = userProgressRepository.getCustomSectionsCount()
        val allGroups = versesGroupsRepository.loadVerseGroups()
        for (i in 1..customCount) {
            val sectionId = 5 + i - 1
            if (sectionId < currentSectionId) {
                userProgressRepository.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                    val g1 = allGroups.find { it.id == id1 }
                    val g2 = allGroups.find { it.id == id2 }
                    val g1Verses = g1?.verses ?: emptyList()
                    val g2Verses = g2?.verses ?: emptyList()
                    val allVerses = g1Verses + g2Verses
                    val allNames = g1Verses.mapIndexed { j, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id1, j + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    } + g2Verses.mapIndexed { j, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id2, j + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    }
                    val nameMap = allVerses.zip(allNames).toMap()
                    val sectionVerses = allVerses.distinct()
                    val sectionAssetNames = sectionVerses.map { nameMap[it] ?: "" }
                    if (sectionVerses.isNotEmpty()) {
                        sectionsList.add(sectionVerses to sectionAssetNames)
                    }
                }
            }
        }
        sectionsList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(id = R.color.game_button_yellow_dark),
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(R.string.review_screen_caption),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (knownVersesSections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_verses_to_review),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                if(isLandscape) {
                    Row {
                        ChooseRiddlesCount(
                            options = options,
                            selectedCount = selectedCount,
                            onOptionSelected = { count -> selectedCount = count }
                        )

                        if(shieldsCount < UserProgressRepository.MAX_SHIELDS) {
                            Spacer(modifier = Modifier.width(32.dp))
                            PlayForShieldsSection(
                                playForShields = playForShields,
                                selectedCount = selectedCount,
                                shieldsCount = shieldsCount,
                                onPlayForShieldsChange = { playForShields = it }
                            )
                        }
                    }
                } else {
                    ChooseRiddlesCount(
                        options = options,
                        selectedCount = selectedCount,
                        onOptionSelected = { count -> selectedCount = count }
                    )

                    if(shieldsCount < UserProgressRepository.MAX_SHIELDS) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PlayForShieldsSection(
                            playForShields = playForShields,
                            selectedCount = selectedCount,
                            shieldsCount = shieldsCount,
                            onPlayForShieldsChange = { playForShields = it }
                        )
                    }
                }



            }
        }

        if (knownVersesSections.isNotEmpty()) {
            Button(
                onClick = {
                    val availableRiddleTypes = listOf(
                        RiddleType.QUIZ_NORMAL.name,
                        RiddleType.MULTI_QUIZ.name,
                        RiddleType.WORD_SCRAMBLE_EASY.name,
                        RiddleType.WORD_SCRAMBLE_NORMAL.name,
                        RiddleType.FILL_SIGLA_BOOK.name,
                        RiddleType.FILL_SIGLA_CHAPTER.name,
                        RiddleType.FILL_SIGLA_VERSE.name,
                        RiddleType.FILL_WORDS_EASY.name,
                        RiddleType.FILL_WORDS_NORMAL.name,
                        RiddleType.FILL_MORE_WORDS_EASY.name,
                        RiddleType.FILL_MORE_WORDS_NORMAL.name
                    )

                    val riddleTypes = ArrayList(availableRiddleTypes.shuffled().take(selectedCount))
                    val startSectionIdx = userProgressRepository.getRepeatSectionIndex()
                    val startVerseIdx = userProgressRepository.getRepeatVerseIndex()
                    val numSections = knownVersesSections.size

                    val reviewVerses = mutableListOf<Verse>()
                    val reviewAssetNames = mutableListOf<String>()

                    (0 until selectedCount).forEach { i ->
                        val currentSectionIdx = (startSectionIdx + i) % numSections
                        val currentVerseIdx = (startVerseIdx + (startSectionIdx + i) / numSections) % 10

                        val (sectionVerses, sectionAssetNames) = knownVersesSections[currentSectionIdx]
                        reviewVerses.add(sectionVerses[currentVerseIdx])
                        reviewAssetNames.add(sectionAssetNames.getOrElse(currentVerseIdx) { "" })
                    }

                    val sectionId = if(playForShields) SECTION_ID_REPEAT_FOR_SHIELDS else SECTION_ID_REPEAT_NORMAL
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putStringArrayListExtra(GameActivity.ARG_LEVEL_RIDDLE_TYPES, riddleTypes)
                        putExtra(GameActivity.ARG_SECTION_ID, sectionId)
                        putExtra(GameActivity.ARG_SECTION_NAME, context.getString(R.string.repeat_level_name))
                        putExtra(GameActivity.ARG_LEVEL_NUMBER, 0)
                        putParcelableArrayListExtra(GameActivity.ARG_SECTION_VERSES, ArrayList(reviewVerses))
                        putStringArrayListExtra(GameActivity.ARG_ASSET_NAMES, ArrayList(reviewAssetNames))
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
            ) {
                Text(
                    text = stringResource(R.string.start_review),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ChooseRiddlesCount(options: List<Int>, selectedCount: Int, onOptionSelected: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.select_riddles_count),
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray
        )
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { count ->
                val isSelected = selectedCount == count
                OutlinedButton(
                    onClick = { onOptionSelected(count) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) colorResource(id = R.color.game_button_yellow_light).copy(
                            alpha = 0.3f
                        ) else Color.Transparent,
                        contentColor = if (isSelected) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray
                    ),
                    border = if (isSelected) BorderStroke(
                        2.dp,
                        colorResource(id = R.color.game_button_yellow_dark)
                    ) else BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(
                        text = count.toString(),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun PlayForShieldsSection(playForShields: Boolean, selectedCount: Int, shieldsCount: Int, onPlayForShieldsChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.play_for_shields),
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        PlayForShieldsSwitch(playForShields = playForShields, onPlayForShieldsChange = onPlayForShieldsChange)
        Spacer(modifier = Modifier.height(8.dp))
        val rewardCount = if (selectedCount == 10) 2 else 1
        val maxPossibleReward = (UserProgressRepository.MAX_SHIELDS - shieldsCount).coerceAtMost(rewardCount)
        val textColor = if(playForShields) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray
        Text(
            text = stringResource(R.string.reward_for_playing, maxPossibleReward),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = FontWeight.Bold
        )

    }
}

@Composable
fun PlayForShieldsSwitch(playForShields: Boolean, onPlayForShieldsChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.no_button),
            fontWeight = if (!playForShields) FontWeight.Bold else FontWeight.Normal,
            color = if (!playForShields) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray
        )
        Switch(
            checked = playForShields,
            onCheckedChange = onPlayForShieldsChange,
            modifier = Modifier.padding(horizontal = 12.dp),
            thumbContent = { Spacer(modifier = Modifier.size(SwitchDefaults.IconSize)) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colorResource(id = R.color.game_button_yellow_dark),
                checkedBorderColor = colorResource(id = R.color.game_button_yellow_dark),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colorResource(id = R.color.game_button_yellow_dark),
                uncheckedBorderColor = colorResource(id = R.color.game_button_yellow_dark)
            )
        )
        Text(
            text = stringResource(R.string.yes_button),
            fontWeight = if (playForShields) FontWeight.Bold else FontWeight.Normal,
            color = if (playForShields) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray
        )
    }
}