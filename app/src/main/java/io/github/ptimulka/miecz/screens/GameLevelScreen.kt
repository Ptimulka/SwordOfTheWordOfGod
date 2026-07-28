package io.github.ptimulka.miecz.screens

import io.github.ptimulka.miecz.components.game.NoShieldsDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.main.SectionVersesDialog
import io.github.ptimulka.miecz.components.main.renderAllVersesLearnedSection
import io.github.ptimulka.miecz.components.main.renderChooseNextSection
import io.github.ptimulka.miecz.MnemonicPicturesActivity
import io.github.ptimulka.miecz.components.main.renderSection
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.repositories.RiddlesOrderRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameLevelScreen(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val userProgressRepository = remember { UserProgressRepository(context) }
    val versesGroupsRepository = remember { VersesGroupsRepository(context) }
    
    // Refresh Logic
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showRepeatHint by remember { mutableStateOf(false) }
    var unlockedSectionNumber by remember { mutableIntStateOf(-1) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userProgressRepository.applyDailyRetentionDecay()
                refreshTrigger++
                if (userProgressRepository.consumePendingRepeatHint()) showRepeatHint = true
                val unlocked = userProgressRepository.consumePendingSectionUnlocked()
                if (unlocked > 0) unlockedSectionNumber = unlocked
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(showRepeatHint) {
        if (showRepeatHint) { delay(30000); showRepeatHint = false }
    }
    LaunchedEffect(Unit) {
        userProgressRepository.applyDailyRetentionDecay()
        refreshTrigger++
    }
    LaunchedEffect(Unit) {
        while(true) {
            userProgressRepository.refreshShields()
            refreshTrigger++
            delay(60000) // Wait 1 minute
        }
    }

    val baseSections = remember { SectionRepository(context).loadInitialSections() }
    val allVerseGroups = remember { versesGroupsRepository.loadVerseGroups() }
    
    val customSections = remember(refreshTrigger, allVerseGroups) {
        val count = userProgressRepository.getCustomSectionsCount()
        (1..count).mapNotNull { index ->
            val sectionId = 5 + index - 1
            userProgressRepository.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                val g1 = allVerseGroups.find { it.id == id1 }
                val g2 = allVerseGroups.find { it.id == id2 }
                if (g1 != null && g2 != null) {
                    val verses = g1.verses + g2.verses
                    val assetNames = g1.verses.mapIndexed { i, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id1, i + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    } + g2.verses.mapIndexed { i, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id2, i + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    }
                    Section(id = sectionId, name = "${g1.name} i ${g2.name}", verses = verses, assetNames = assetNames)
                } else null
            }
        }
    }
    
    val allSections = baseSections + customSections
    val riddlesOrder = remember { RiddlesOrderRepository(context).getRiddlesOrder() }
    var selectedSection by remember { mutableStateOf<Section?>(null) }
    var showChooseVerseGroups by rememberSaveable { mutableStateOf(false) }

    if (showChooseVerseGroups) {

        // Choosing verse groups for next custom section
        ChooseVerseGroupsScreen(
            onGroupsSelected = { id1, id2 ->
                val nextId = allSections.lastOrNull()?.id?.plus(1) ?: 5
                userProgressRepository.saveCustomSection(nextId, id1, id2)
                showChooseVerseGroups = false
                refreshTrigger++
            },
            onBack = { showChooseVerseGroups = false },
            contentPadding = contentPadding
        )
    } else {

        // Sections with paths of buttons

        // Dialog with list of verses
        selectedSection?.let { section ->
            SectionVersesDialog(section = section, onDismissRequest = { selectedSection = null })
        }

        // Shields logic
        var showNoShieldsDialog by remember { mutableStateOf(false) }
        val shieldsCount = remember(refreshTrigger) { userProgressRepository.getShieldsCount() }
        val isShieldsEmpty = shieldsCount <= 0

        // Day streak
        val playedToday = remember(refreshTrigger) { userProgressRepository.hasPlayedToday() }
        val dayStreak = remember(refreshTrigger) { userProgressRepository.getCurrentDayStreak() }

        if (showNoShieldsDialog) {
            NoShieldsDialog(onConfirm = { showNoShieldsDialog = false })
        }

        var isShieldInfoVisible by remember { mutableStateOf(false) }
        var shieldCooldownText by remember { mutableStateOf("") }
        var isLampInfoVisible by remember { mutableStateOf(false) }
        var shieldPillWidthPx by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current

        fun formatCooldown(ms: Long): String {
            val minutes = (ms / 1000) / 60
            val seconds = (ms / 1000) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        LaunchedEffect(isShieldInfoVisible) {
            while (isShieldInfoVisible) {
                val currentShields = userProgressRepository.refreshShields()
                if (currentShields != shieldsCount) refreshTrigger++
                if (currentShields >= UserProgressRepository.MAX_SHIELDS) {
                    shieldCooldownText = context.getString(R.string.all_shields_full)
                    delay(1000)
                    continue
                }
                val remaining = userProgressRepository.getTimeToNextShield()
                if (remaining > 0) {
                    shieldCooldownText =
                        "${context.getString(R.string.next_shield_in)}\n${formatCooldown(remaining)}"
                } else {
                    refreshTrigger++
                    isShieldInfoVisible = false
                }
                delay(1000)
            }
        }

        // Auto-hide the info toasts after 7 s (they then fade out over 3 s)
        LaunchedEffect(isShieldInfoVisible) {
            if (isShieldInfoVisible) { delay(7000); isShieldInfoVisible = false }
        }
        LaunchedEffect(isLampInfoVisible) {
            if (isLampInfoVisible) { delay(7000); isLampInfoVisible = false }
        }

        // Jump to current section logic
        val currentSectionId = remember(refreshTrigger) { userProgressRepository.getCurrentSection() }
        val currentScrollIndex = remember(allSections, currentSectionId, riddlesOrder) {
            var totalRows = 0
            for (section in allSections) {
                if (section.id == currentSectionId) break
                totalRows += 1 + 1 + riddlesOrder.size + 1
            }
            totalRows
        }

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(allSections) {
            if (allSections.isNotEmpty()) {
                listState.animateScrollToItem(currentScrollIndex)
            }
        }

        val showJumpButton by remember {
            derivedStateOf {
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) {
                    false
                } else {
                    val lastVisibleIndex = visibleItems.last().index
                    lastVisibleIndex < currentScrollIndex
                }
            }
        }

        // Choose next custom section verses
        val usedGroupIds = remember(refreshTrigger) { userProgressRepository.getAllUsedGroupIds() }
        val availableGroupsCount = allVerseGroups.count { !usedGroupIds.contains(it.id) }

        // Congratulate the user after they unlock a new section. When there are no more verse
        // groups left to learn (same condition as the trophy cup), show the "all learned" message.
        if (unlockedSectionNumber > 0) {
            AlertDialog(
                onDismissRequest = { unlockedSectionNumber = -1 },
                title = {
                    Text(
                        text = stringResource(R.string.section_unlocked_title),
                        color = colorResource(R.color.correct_answer_green),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (availableGroupsCount >= 2)
                            stringResource(R.string.section_unlocked_message, unlockedSectionNumber)
                        else stringResource(R.string.section_unlocked_all_message)
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { unlockedSectionNumber = -1 }) {
                        Text(stringResource(R.string.ok_button))
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // Render sections
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                allSections.forEach { section ->
                    renderSection(
                        section = section,
                        isLocked = section.id > currentSectionId,
                        riddlesOrder = riddlesOrder,
                        userProgressRepository = userProgressRepository,
                        refreshTrigger = refreshTrigger,
                        onShowVerses = { selectedSection = it },
                        onDrawPictures = { s ->
                            context.startActivity(
                                MnemonicPicturesActivity.createIntent(
                                    context,
                                    s.id,
                                    s.name,
                                    ArrayList(s.verses),
                                    ArrayList(s.assetNames)
                                )
                            )
                        },
                        isShieldsEmpty = isShieldsEmpty,
                        onNoShieldsClick = { showNoShieldsDialog = true }
                    )
                }

                val lastSection = allSections.lastOrNull()
                val isLastFinished = lastSection != null && userProgressRepository.areSpecialChallengesFinished(lastSection.id)
                val nextId = (lastSection?.id ?: 4) + 1
                val isPlaceholderLocked = !isLastFinished || nextId > currentSectionId

                if (availableGroupsCount >= 2) {
                    renderChooseNextSection(
                        nextId = nextId,
                        isLocked = isPlaceholderLocked,
                        onChooseClick = { showChooseVerseGroups = true }
                    )
                } else {
                    renderAllVersesLearnedSection(nextId = nextId, isLocked = isPlaceholderLocked)
                }
            }

            // Floating shields + streak column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        bottom = contentPadding.calculateBottomPadding() + 12.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Repeat hint toast — topmost, full width
                if (showRepeatHint) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorResource(R.color.toast_text),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = stringResource(R.string.repeat_for_shields_hint),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                // Info toasts — shield toast above the shield pill, lamp toast above the lamp pill.
                // The lamp toast is anchored at a fixed leading offset (the shield pill width) so
                // fading the shield toast out over 3 s never shifts the lamp toast sideways.
                Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isShieldInfoVisible,
                        modifier = Modifier.align(Alignment.BottomStart),
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 3000)
                        )
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { isShieldInfoVisible = false }
                        ) {
                            Text(
                                text = shieldCooldownText,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isLampInfoVisible,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = with(density) { shieldPillWidthPx.toDp() } + 10.dp),
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 3000)
                        )
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { isLampInfoVisible = false }
                        ) {
                            Text(
                                text = stringResource(
                                    when {
                                        playedToday -> R.string.day_streak_toast
                                        dayStreak > 0 -> R.string.day_streak_toast_keep
                                        else -> R.string.day_streak_toast_start
                                    }
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Buttons row — never changes, layout always stable
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { isShieldInfoVisible = !isShieldInfoVisible },
                        modifier = Modifier
                            .height(40.dp)
                            .onSizeChanged { shieldPillWidthPx = it.width },
                        shape = CircleShape,
                        color = colorResource(id = R.color.game_button_yellow_dark),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.buttonshield),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = shieldsCount.toString(),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        onClick = { isLampInfoVisible = !isLampInfoVisible },
                        modifier = Modifier.height(40.dp),
                        shape = CircleShape,
                        color = colorResource(if (playedToday) R.color.game_button_yellow_dark else R.color.game_button_grey_dark),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (playedToday) R.drawable.buttonlamp else R.drawable.buttonlamplow),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = dayStreak.toString(),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } // end buttons Row
            } // end Column

            // Jump to section floating button
            if (showJumpButton) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(currentScrollIndex)
                        }
                    },
                    containerColor = colorResource(id = R.color.game_button_yellow_dark),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = contentPadding.calculateBottomPadding() + 12.dp,
                            end = 12.dp
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = stringResource(R.string.jump_to_current_section),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
