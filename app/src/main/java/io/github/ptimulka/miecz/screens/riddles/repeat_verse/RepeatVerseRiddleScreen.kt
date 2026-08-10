package io.github.ptimulka.miecz.screens.riddles.repeat_verse

import android.Manifest
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.helpers.createPolishSpeechIntent
import io.github.ptimulka.miecz.repositories.UserMnemonicPicturesRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect

private const val SIMILARITY_THRESHOLD = 50f
private val MAX_REPEATS = UserProgressRepository.MAX_VERSE_REPEATS_PER_DAY

@Composable
fun RepeatVerseRiddleScreen(
    sectionVerses: List<Verse>,
    sectionId: Int,
    assetNames: List<String>,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    val vm: RepeatVerseViewModel = viewModel(
        key = "RepeatVerseVM_${sectionId}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RepeatVerseViewModel(
                    RepeatVerseArgs(sectionId, sectionVerses, assetNames),
                    UserProgressRepository(context),
                    UserMnemonicPicturesRepository(context)
                ) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var lastAttemptPreferredOffline by remember { mutableStateOf(false) }

    fun startListening(preferOffline: Boolean = true) {
        lastAttemptPreferredOffline = preferOffline
        vm.onEvent(RepeatVerseEvent.SetListening(true))
        speechRecognizer.startListening(
            createPolishSpeechIntent(
                preferOffline = preferOffline,
                maxResults = 1,
                partialResults = true
            )
        )
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                vm.onEvent(RepeatVerseEvent.ProcessResult(matches?.getOrNull(0) ?: ""))
            }
            override fun onError(error: Int) {
                val offlineUnavailable = error == SpeechRecognizer.ERROR_NETWORK ||
                    error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_SERVER
                if (lastAttemptPreferredOffline && offlineUnavailable) {
                    startListening(preferOffline = false)
                } else {
                    vm.onEvent(RepeatVerseEvent.SetListening(false))
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.getOrNull(0) ?: ""
                vm.onEvent(RepeatVerseEvent.UpdatePartialText(partial))
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    LaunchedEffect(vm.effects) {
        vm.effects.collect { effect ->
            when (effect) {
                RepeatVerseEffect.RequestPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                is RepeatVerseEffect.StartListening -> startListening(effect.preferOffline)
                is RiddleEffect.Success -> onSuccess()
            }
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize()) {
        if (isLandscape) {
            LandscapeRepeatLayout(state, sectionVerses, sectionId, assetNames, UserMnemonicPicturesRepository(context), vm::onEvent)
        } else {
            PortraitRepeatLayout(state, sectionVerses, sectionId, assetNames, UserMnemonicPicturesRepository(context), vm::onEvent)
        }

        val hint = state.hintBitmap
        if (state.zoomIndex != null && hint != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { vm.onEvent(RepeatVerseEvent.ShowZoom(null)) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = hint.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.85f)
                )
            }
        }
    }
}

@Composable
private fun PortraitRepeatLayout(
    state: RepeatVerseUiState,
    sectionVerses: List<Verse>,
    sectionId: Int,
    assetNames: List<String>,
    mnemonicRepository: UserMnemonicPicturesRepository,
    onEvent: (RepeatVerseEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GalleryArea(
            Modifier.fillMaxWidth().weight(1f),
            state,
            sectionVerses,
            sectionId,
            assetNames,
            mnemonicRepository,
            onEvent
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        val selectedVerse = state.selectedIndex?.let { sectionVerses.getOrNull(it) }
        if (selectedVerse != null) {
            VerseContentArea(selectedVerse, state, onEvent, isLandscape = false)
        } else {
            EmptyHintArea(isLandscape = false)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        RetentionCaptionArea(state, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LandscapeRepeatLayout(
    state: RepeatVerseUiState,
    sectionVerses: List<Verse>,
    sectionId: Int,
    assetNames: List<String>,
    mnemonicRepository: UserMnemonicPicturesRepository,
    onEvent: (RepeatVerseEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        GalleryArea(
            Modifier.fillMaxHeight().weight(1f),
            state,
            sectionVerses,
            sectionId,
            assetNames,
            mnemonicRepository,
            onEvent,
            isLandscape = true
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val selectedVerse = state.selectedIndex?.let { sectionVerses.getOrNull(it) }
            if (selectedVerse != null) {
                VerseContentArea(selectedVerse, state, onEvent, isLandscape = true)
            } else {
                EmptyHintArea(isLandscape = true)
            }
        }
    }
}

@Composable
private fun GalleryArea(
    modifier: Modifier,
    state: RepeatVerseUiState,
    sectionVerses: List<Verse>,
    sectionId: Int,
    assetNames: List<String>,
    mnemonicRepository: UserMnemonicPicturesRepository,
    onEvent: (RepeatVerseEvent) -> Unit,
    isLandscape: Boolean = false
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = if (isLandscape) Arrangement.spacedBy(6.dp, Alignment.CenterVertically) else Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(sectionVerses) { index, verse ->
            val bitmap = remember(sectionId, index, assetNames) {
                mnemonicRepository.loadActivePicture(sectionId, index, assetNames.getOrNull(index))
            }
            VerseThumbnail(
                bitmap = bitmap,
                sigla = "${verse.book} ${verse.chapter},${verse.number}",
                isSelected = state.selectedIndex == index,
                isMaxed = state.maxedIndices.contains(index), 
                onClick = { onEvent(RepeatVerseEvent.SelectVerse(index)) },
                onZoom = { onEvent(RepeatVerseEvent.ShowZoom(index)) }
            )
        }
    }
}

@Composable
private fun ColumnScope.VerseContentArea(
    verse: Verse,
    state: RepeatVerseUiState,
    onEvent: (RepeatVerseEvent) -> Unit,
    isLandscape: Boolean
) {
    val feedbackText = when {
        state.isListening -> if (state.partialText.isEmpty()) stringResource(R.string.speak_now)
        else "…" + state.partialText.split(' ').filter { it.isNotEmpty() }.takeLast(5).joinToString(" ")
        state.lastSimilarity < 0f -> ""
        state.lastSimilarity >= SIMILARITY_THRESHOLD -> stringResource(R.string.repeat_verse_accepted, state.lastSimilarity)
        else -> stringResource(R.string.repeat_verse_too_low, state.lastSimilarity)
    }
    val feedbackColor = when {
        state.isListening -> colorResource(R.color.game_button_yellow_dark)
        state.lastSimilarity in 0f..<SIMILARITY_THRESHOLD -> Color.Red
        else -> colorResource(R.color.correct_answer_green)
    }

    Text(
        text = "${verse.book} ${verse.chapter},${verse.number}",
        fontSize = if (isLandscape) 16.sp else 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = colorResource(R.color.game_button_yellow_dark),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 6.dp))
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedVerseText(verse.text),
            fontSize = if (isLandscape) 15.sp else 20.sp,
            lineHeight = if (isLandscape) 21.sp else 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        )
    }
    Text(
        text = feedbackText,
        fontSize = 13.sp,
        color = feedbackColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().height(20.dp)
    )
    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))
    
    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            MicButton(state, onEvent)
            RetentionCaptionArea(state, Modifier.widthIn(max = 200.dp))
        }
    } else {
        MicButton(state, onEvent)
    }
}

@Composable
private fun ColumnScope.EmptyHintArea(isLandscape: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.repeat_verse_choose_hint),
            fontSize = if (isLandscape) 14.sp else 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RetentionCaptionArea(state: RepeatVerseUiState, modifier: Modifier) {
    if (state.selectedIndex != null && !state.isSectionFinished) {
        Text(
            text = stringResource(
                R.string.repeat_verse_retention_growth,
                state.verseRetentionToday,
                state.sectionRetentionToday
            ),
            fontSize = 12.sp,
            lineHeight = 13.sp,
            color = colorResource(R.color.game_button_yellow_dark),
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}

@Composable
private fun MicButton(state: RepeatVerseUiState, onEvent: (RepeatVerseEvent) -> Unit) {
    Button(
        onClick = { onEvent(RepeatVerseEvent.RequestPermission) },
        enabled = state.selectedIndex != null && state.repeatCount < MAX_REPEATS,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                state.isListening -> Color.Red
                state.repeatCount >= MAX_REPEATS -> colorResource(R.color.correct_answer_green)
                else -> colorResource(R.color.game_button_yellow_dark)
            },
            disabledContainerColor = Color.LightGray
        ),
        modifier = Modifier.size(90.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(id = R.drawable.microphone), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(text = "${state.repeatCount}/$MAX_REPEATS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VerseThumbnail(
    bitmap: Bitmap?,
    sigla: String,
    isSelected: Boolean,
    isMaxed: Boolean,
    onClick: () -> Unit,
    onZoom: () -> Unit
) {
    val accent = colorResource(R.color.game_button_yellow_dark)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) accent else Color.LightGray,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .background(accent, CircleShape)
                        .clickable { onZoom() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sigla,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = accent,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        if (isMaxed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.6f))
            )
        }
    }
}
