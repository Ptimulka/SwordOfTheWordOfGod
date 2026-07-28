package io.github.ptimulka.miecz.screens

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.ptimulka.miecz.GameActivity
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.delay

private const val INITIAL_COUNTDOWN = 10

@Composable
fun RandomVerseScreen(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val repository = remember { VersesGroupsRepository(context) }
    val allVerses = remember { repository.loadVerseGroups().flatMap { it.verses } }

    var randomVerse by rememberSaveable { mutableStateOf(allVerses.random()) }
    var isEasy by rememberSaveable { mutableStateOf(false) }
    var countdown by rememberSaveable { mutableIntStateOf(INITIAL_COUNTDOWN) }
    var isCountingDown by rememberSaveable { mutableStateOf(true) }

    val onDrawAnother = {
        randomVerse = allVerses.random()
        countdown = INITIAL_COUNTDOWN
        isCountingDown = true
    }

    val onStartNow = {
        countdown = 0
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !isCountingDown) {
                onDrawAnother()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isCountingDown, randomVerse) {
        if (isCountingDown) {
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
            isCountingDown = false
            val riddleTypes = if (isEasy) {
                arrayListOf(
                    "QUIZ_EASY",
                    "FILL_WORDS_EASY",
                    "FILL_SIGLA_BOOK",
                    "FILL_MORE_WORDS_EASY",
                    "FILL_SIGLA_CHAPTER"
                )
            } else {
                arrayListOf(
                    "QUIZ_NORMAL",
                    "FILL_WORDS_NORMAL",
                    "MULTI_QUIZ",
                    "FILL_MORE_WORDS_NORMAL",
                    "FILL_SIGLA_VERSE"
                )
            }
            val intent = Intent(context, GameActivity::class.java).apply {
                putStringArrayListExtra(GameActivity.ARG_LEVEL_RIDDLE_TYPES, riddleTypes)
                putExtra(GameActivity.ARG_SECTION_ID, 0)
                putExtra(GameActivity.ARG_SECTION_NAME, context.getString(R.string.random_verse))
                putExtra(GameActivity.ARG_LEVEL_NUMBER, 0)
                putParcelableArrayListExtra(GameActivity.ARG_SECTION_VERSES, arrayListOf(randomVerse))
            }
            context.startActivity(intent)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(id = R.color.game_button_yellow_dark),
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(R.string.random_screen_caption),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                    VerseCard(randomVerse = randomVerse)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DifficultySection(isEasy = isEasy, onEasyChange = { isEasy = it })
                    Spacer(modifier = Modifier.height(24.dp))
                    DrawAnotherButton(onClick = onDrawAnother, modifier = Modifier.fillMaxWidth().height(50.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    StartButton(countdown = countdown, onClick = onStartNow, modifier = Modifier.fillMaxWidth().height(50.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                DifficultySection(isEasy = isEasy, onEasyChange = { isEasy = it })
                Spacer(modifier = Modifier.height(24.dp))
                VerseCard(randomVerse = randomVerse)
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DrawAnotherButton(onClick = onDrawAnother, modifier = Modifier.weight(1f).height(50.dp))
                    StartButton(countdown = countdown, onClick = onStartNow, modifier = Modifier.weight(1f).height(50.dp))
                }
            }
        }
    }
}

@Composable
fun DrawAnotherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
    ) {
        Text(
            text = stringResource(R.string.draw_another),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StartButton(countdown: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
    ) {
        Text(
            text = stringResource(R.string.start_countdown, countdown),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DifficultySection(isEasy: Boolean, onEasyChange: (Boolean) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.difficulty_level) + ":",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(end = 8.dp)
            )
            DifficultySwitch(isEasy = isEasy, onEasyChange = onEasyChange)
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.difficulty_level),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            DifficultySwitch(isEasy = isEasy, onEasyChange = onEasyChange)
        }
    }
}

@Composable
fun DifficultySwitch(isEasy: Boolean, onEasyChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.difficulty_normal),
            fontWeight = if (!isEasy) FontWeight.Bold else FontWeight.Normal,
            color = if (!isEasy) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray
        )
        Switch(
            checked = isEasy,
            onCheckedChange = onEasyChange,
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
            text = stringResource(R.string.difficulty_easy),
            fontWeight = if (isEasy) FontWeight.Bold else FontWeight.Normal,
            color = if (isEasy) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray
        )
    }
}

@Composable
fun VerseCard(randomVerse: Verse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "${randomVerse.book} ${randomVerse.chapter},${randomVerse.number}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colorResource(id = R.color.game_button_yellow_dark)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = buildAnnotatedVerseText(randomVerse.text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}