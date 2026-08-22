package io.github.ptimulka.miecz.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptimulka.miecz.MnemonicPicturesActivity
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.NoShieldsDialog
import io.github.ptimulka.miecz.components.main.SectionVersesDialog
import io.github.ptimulka.miecz.components.main.renderAllVersesLearnedSection
import io.github.ptimulka.miecz.components.main.renderChooseNextSection
import io.github.ptimulka.miecz.components.main.renderSection
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun GameLevelScreen(contentPadding: PaddingValues = PaddingValues()) {
    val vm: GameLevelViewModel = hiltViewModel<GameLevelViewModel, GameLevelViewModel.Factory> { factory ->
        factory.create(autoStartRefreshLoop = true)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger a data refresh whenever this screen enters the composition (e.g. switching tabs)
    LaunchedEffect(Unit) {
        vm.onEvent(GameLevelEvent.OnResume)
    }

    // Lifecycle handling for app-level resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.onEvent(GameLevelEvent.OnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (state.showChooseVerseGroups) {
        ChooseVerseGroupsScreen(
            state = state,
            onEvent = vm::onEvent,
            contentPadding = contentPadding
        )
    } else {
        GameLevelMainContent(state, vm::onEvent, contentPadding)
    }
}

@Composable
private fun GameLevelMainContent(
    state: GameLevelUiState,
    onEvent: (GameLevelEvent) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    // Dialog with list of verses
    state.selectedSectionForDialog?.let { section ->
        SectionVersesDialog(section = section, onDismissRequest = { onEvent(GameLevelEvent.ShowSectionVerses(null)) })
    }

    // Shields logic
    var showNoShieldsDialog by remember { mutableStateOf(false) }
    if (showNoShieldsDialog) {
        NoShieldsDialog(onConfirm = { showNoShieldsDialog = false })
    }

    // Jump to current section logic
    val currentScrollIndex = remember(state.sections, state.progress.currentSectionId, state.riddlesOrder) {
        var totalRows = 0
        for (section in state.sections) {
            if (section.id == state.progress.currentSectionId) break
            totalRows += 1 + 1 + state.riddlesOrder.size + 1
        }
        totalRows
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var hasAutoScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(state.sections, state.sectionStates, currentScrollIndex) {
        if (!hasAutoScrolled && state.sections.isNotEmpty() && state.sectionStates.isNotEmpty()) {
            listState.scrollToItem(currentScrollIndex)
            hasAutoScrolled = true
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

    // Congratulation dialog
    if (state.progress.unlockedSectionId != -1) {
        SectionUnlockedDialog(
            sectionId = state.progress.unlockedSectionId,
            onDismiss = { onEvent(GameLevelEvent.DismissUnlockedDialog) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            state.sections.forEach { section ->
                val sectionState = state.sectionStates[section.id] ?: SectionState()
                renderSection(
                    section = section,
                    sectionState = sectionState,
                    isLocked = section.id > state.progress.currentSectionId,
                    isShieldsEmpty = state.isShieldsEmpty,
                    onShowVerses = { onEvent(GameLevelEvent.ShowSectionVerses(it)) },
                    onDrawPictures = { s ->
                        MnemonicPicturesActivity.createIntent(
                            context,
                            s.id,
                            s.name,
                            ArrayList(s.verses),
                            ArrayList(s.assetNames)
                        ).let { intent -> context.startActivity(intent) }
                    },
                    onNoShieldsClick = { showNoShieldsDialog = true }
                )
            }

            val lastSection = state.sections.lastOrNull()
            val isLastFinished = lastSection != null && (state.sectionStates[lastSection.id]?.areSpecialChallengesFinished == true)
            val nextId = (lastSection?.id ?: 4) + 1
            val isPlaceholderLocked = !isLastFinished || nextId > state.progress.currentSectionId

            if (state.progress.availableGroupsCount >= 2) {
                renderChooseNextSection(
                    nextId = nextId,
                    isLocked = isPlaceholderLocked,
                    onChooseClick = { onEvent(GameLevelEvent.NavigateToChooseGroups) }
                )
            } else {
                renderAllVersesLearnedSection(nextId = nextId, isLocked = isPlaceholderLocked)
            }
        }

        GameLevelOverlays(state, onEvent, contentPadding)

        // Jump button
        if (showJumpButton) {
            JumpToCurrentButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 12.dp, end = 12.dp),
                onClick = {
                    scope.launch { listState.animateScrollToItem(currentScrollIndex) }
                }
            )
        }
    }
}

@Composable
private fun GameLevelOverlays(
    state: GameLevelUiState,
    onEvent: (GameLevelEvent) -> Unit,
    contentPadding: PaddingValues
) {
    val density = LocalDensity.current
    var shieldPillWidthPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding() + 12.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (state.progress.showRepeatHint && state.progress.currentSectionId >= 3) {
            RepeatHintToast()
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
            ShieldInfoToast(
                isVisible = state.isShieldInfoVisible,
                cooldownMs = state.progress.timeToNextShieldMs,
                shieldsCount = state.progress.shieldsCount,
                onDismiss = { onEvent(GameLevelEvent.ToggleShieldInfo) }
            )

            LampInfoToast(
                isVisible = state.isLampInfoVisible,
                playedToday = state.progress.playedToday,
                dayStreak = state.progress.dayStreak,
                offsetX = with(density) { shieldPillWidthPx.toDp() } + 10.dp,
                onDismiss = { onEvent(GameLevelEvent.ToggleLampInfo) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProgressPill(
                iconRes = R.drawable.buttonshield,
                value = state.progress.shieldsCount.toString(),
                onClick = { onEvent(GameLevelEvent.ToggleShieldInfo) },
                onSizeChanged = { shieldPillWidthPx = it.width }
            )

            ProgressPill(
                iconRes = if (state.progress.playedToday) R.drawable.buttonlamp else R.drawable.buttonlamplow,
                value = state.progress.dayStreak.toString(),
                onClick = { onEvent(GameLevelEvent.ToggleLampInfo) },
                backgroundColor = if (state.progress.playedToday)
                    colorResource(R.color.game_button_yellow_dark)
                    else colorResource(R.color.game_button_grey_dark)
            )
        }
    }
}

@Composable
private fun ShieldInfoToast(
    isVisible: Boolean,
    cooldownMs: Long,
    shieldsCount: Int,
    onDismiss: () -> Unit
) {
    val text = if (shieldsCount >= UserProgressRepository.MAX_SHIELDS) {
        stringResource(R.string.all_shields_full)
    } else {
        val minutes = (cooldownMs / 1000) / 60
        val seconds = (cooldownMs / 1000) % 60
        val cooldownStr = String.format(Locale.getDefault(), "%02d:%02d", minutes.toInt(), seconds.toInt())
        "${stringResource(R.string.next_shield_in)}\n$cooldownStr"
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 3000))
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable(onClick = onDismiss)
        ) {
            Text(
                text = text,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LampInfoToast(
    isVisible: Boolean,
    playedToday: Boolean,
    dayStreak: Int,
    offsetX: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier.padding(start = offsetX),
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 3000))
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable(onClick = onDismiss)
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

@Composable
private fun ProgressPill(
    iconRes: Int,
    value: String,
    onClick: () -> Unit,
    backgroundColor: Color = colorResource(id = R.color.game_button_yellow_dark),
    onSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .onSizeChanged { onSizeChanged(it) },
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RepeatHintToast() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colorResource(R.color.game_button_yellow_dark),
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

@Composable
private fun JumpToCurrentButton(
    modifier: Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = colorResource(id = R.color.game_button_yellow_dark),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_down),
            contentDescription = stringResource(R.string.jump_to_current_section),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SectionUnlockedDialog(sectionId: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.section_unlocked_title),
                color = colorResource(R.color.correct_answer_green),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Text(stringResource(R.string.section_unlocked_message, sectionId))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.ok_button))
            }
        }
    )
}
