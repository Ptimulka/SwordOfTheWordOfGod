package io.github.ptimulka.miecz.screens.review

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.launchGame
import io.github.ptimulka.miecz.repositories.UserSectionRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.UserVersesGroupsRepository

@Composable
fun ReviewVersesScreen(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    
    val reviewName = stringResource(R.string.repeat_level_name)
    val vm: ReviewViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReviewViewModel(
                    UserProgressRepository(context),
                    UserSectionRepository(context),
                    UserVersesGroupsRepository(context),
                    reviewName
                ) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()

    // Trigger a data refresh whenever this screen enters the composition (e.g. switching tabs)
    LaunchedEffect(Unit) {
        vm.onEvent(ReviewEvent.OnResume)
    }
    
    // Lifecycle handling
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.onEvent(ReviewEvent.OnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(vm.effects) {
        vm.effects.collect { effect ->
            when (effect) {
                is ReviewEffect.LaunchReview -> {
                    val sectionWithName = effect.section.copy(
                        name = context.getString(R.string.repeat_level_name)
                    )
                    launchGame(context, sectionWithName, effect.riddleTypes, 0)
                }
            }
        }
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

            if (!state.isReviewAvailable) {
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

                if (isLandscape) {
                    Row {
                        ChooseRiddlesCount(
                            selectedCount = state.selectedCount,
                            onOptionSelected = { vm.onEvent(ReviewEvent.SetCount(it)) }
                        )

                        if (state.shieldsCount < UserProgressRepository.MAX_SHIELDS) {
                            Spacer(modifier = Modifier.width(32.dp))
                            PlayForShieldsSection(
                                playForShields = state.playForShields,
                                maxPossibleReward = state.maxPossibleReward,
                                onPlayForShieldsChange = { vm.onEvent(ReviewEvent.SetPlayForShields(it)) }
                            )
                        }
                    }
                } else {
                    ChooseRiddlesCount(
                        selectedCount = state.selectedCount,
                        onOptionSelected = { vm.onEvent(ReviewEvent.SetCount(it)) }
                    )

                    if (state.shieldsCount < UserProgressRepository.MAX_SHIELDS) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PlayForShieldsSection(
                            playForShields = state.playForShields,
                            maxPossibleReward = state.maxPossibleReward,
                            onPlayForShieldsChange = { vm.onEvent(ReviewEvent.SetPlayForShields(it)) }
                        )
                    }
                }
            }
        }

        if (state.isReviewAvailable) {
            Button(
                onClick = { vm.onEvent(ReviewEvent.StartReview) },
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
private fun ChooseRiddlesCount(selectedCount: Int, onOptionSelected: (Int) -> Unit) {
    val options = listOf(5, 8, 10)
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
fun PlayForShieldsSection(
    playForShields: Boolean, 
    maxPossibleReward: Int, 
    onPlayForShieldsChange: (Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.play_for_shields),
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        PlayForShieldsSwitch(playForShields = playForShields, onPlayForShieldsChange = onPlayForShieldsChange)
        Spacer(modifier = Modifier.height(8.dp))
        
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
