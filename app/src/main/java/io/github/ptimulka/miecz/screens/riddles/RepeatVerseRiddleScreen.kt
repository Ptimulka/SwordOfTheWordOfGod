package io.github.ptimulka.miecz.screens.riddles

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.helpers.calculateWordSimilarity
import io.github.ptimulka.miecz.helpers.createPolishSpeechIntent
import io.github.ptimulka.miecz.helpers.normalizeVerseText
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository

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
    val repository = remember { MnemonicPicturesRepository(context) }
    val progressRepository = remember { UserProgressRepository(context) }

    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var repeatCount by remember { mutableIntStateOf(0) }
    var isListening by remember { mutableStateOf(false) }
    var lastSimilarity by remember { mutableFloatStateOf(-1f) }
    var partialText by remember { mutableStateOf("") }
    var zoomIndex by remember { mutableStateOf<Int?>(null) }

    val selectedVerse = selectedIndex?.let { sectionVerses.getOrNull(it) }

    // Reload the persisted daily count whenever the selection changes
    LaunchedSelection(selectedIndex) { idx ->
        repeatCount = if (idx != null) progressRepository.getVerseRepeatCountToday(sectionId, idx) else 0
        lastSimilarity = -1f
        partialText = ""
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // Prefer on-device recognition so the practice also works without internet. If the device has
    // no offline Polish pack this fails, so we remember the mode and retry online once.
    var lastAttemptPreferredOffline by remember { mutableStateOf(false) }

    fun startListening(preferOffline: Boolean = true) {
        lastSimilarity = -1f
        partialText = ""
        isListening = true
        lastAttemptPreferredOffline = preferOffline
        speechRecognizer.startListening(
            createPolishSpeechIntent(
                preferOffline = preferOffline,
                maxResults = 1,
                partialResults = true
            )
        )
    }

    // Kept up to date so the (once-registered) recognition listener always sees the current selection
    val handleResult by rememberUpdatedState<(String) -> Unit> { recognized ->
        partialText = ""
        val idx = selectedIndex
        val verse = idx?.let { sectionVerses.getOrNull(it) }
        if (verse != null) {
            val userWords = normalizeVerseText(recognized).split(' ').filter { it.isNotEmpty() }
            val cleanVerse = verse.text.replace("_", " ").replace("*", "")
            val verseWords = normalizeVerseText(cleanVerse).split(' ').filter { it.isNotEmpty() }
            val similarity = calculateWordSimilarity(userWords, verseWords)
            lastSimilarity = similarity
            if (similarity >= SIMILARITY_THRESHOLD) {
                repeatCount = progressRepository.incrementVerseRepeatToday(sectionId, idx)
                // Lifetime tally of aloud repetitions (shown in achievements)
                progressRepository.incrementTotalAloudRepeats()
                // Practicing counts toward the daily streak (no-op if already counted today)
                progressRepository.updateDayStreak()
            }
        }
        isListening = false
    }
    val handleResultState = rememberUpdatedState(handleResult)

    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                handleResultState.value(matches?.getOrNull(0) ?: "")
            }
            override fun onError(error: Int) {
                partialText = ""
                // No offline pack on this device — retry once using online recognition
                val offlineUnavailable = error == SpeechRecognizer.ERROR_NETWORK ||
                    error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_SERVER
                if (lastAttemptPreferredOffline && offlineUnavailable) {
                    startListening(preferOffline = false)
                } else {
                    isListening = false
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.getOrNull(0) ?: ""
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Shared feedback state
    val feedbackText = when {
        isListening -> if (partialText.isEmpty()) stringResource(R.string.speak_now)
                       else "…" + partialText.split(' ').filter { it.isNotEmpty() }.takeLast(5).joinToString(" ")
        lastSimilarity < 0f -> ""
        lastSimilarity >= SIMILARITY_THRESHOLD -> stringResource(R.string.repeat_verse_accepted, lastSimilarity)
        else -> stringResource(R.string.repeat_verse_too_low, lastSimilarity)
    }
    val feedbackColor = when {
        isListening -> colorResource(R.color.game_button_yellow_dark)
        lastSimilarity in 0f..<SIMILARITY_THRESHOLD -> Color.Red
        else -> colorResource(R.color.correct_answer_green)
    }

    // Retention gained today for the selected verse and for the whole section (recomputed after each repeat)
    val verseRetentionToday = progressRepository.retentionContributionForRepeats(repeatCount)
    val sectionRetentionToday = remember(repeatCount, selectedIndex) {
        sectionVerses.indices.sumOf {
            progressRepository.retentionContributionForRepeats(
                progressRepository.getVerseRepeatCountToday(sectionId, it)
            )
        }
    }

    val isSectionFinished = remember { progressRepository.areSpecialChallengesFinished(sectionId) }
    val retentionCaption: @Composable () -> Unit = {
        if (selectedVerse != null && !isSectionFinished) {
            Text(
                text = stringResource(
                    R.string.repeat_verse_retention_growth,
                    verseRetentionToday,
                    sectionRetentionToday
                ),
                fontSize = 12.sp,
                color = colorResource(R.color.game_button_yellow_dark),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val gallery: @Composable (Modifier) -> Unit = { galleryModifier ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = galleryModifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(sectionVerses) { index, verse ->
                val bitmap = remember(sectionId, index, assetNames) {
                    repository.loadActivePicture(sectionId, index, assetNames.getOrNull(index))
                }
                val isMaxed = remember(repeatCount, index) {
                    progressRepository.getVerseRepeatCountToday(sectionId, index) >= MAX_REPEATS
                }
                VerseThumbnail(
                    bitmap = bitmap,
                    sigla = "${verse.book} ${verse.chapter},${verse.number}",
                    isSelected = selectedIndex == index,
                    isMaxed = isMaxed,
                    onClick = { selectedIndex = index },
                    onZoom = { zoomIndex = index }
                )
            }
        }
    }

    val micButton: @Composable () -> Unit = {
        Button(
            onClick = {
                if (selectedIndex != null && !isListening && repeatCount < MAX_REPEATS) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = selectedIndex != null && repeatCount < MAX_REPEATS,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isListening -> Color.Red
                    repeatCount >= MAX_REPEATS -> colorResource(R.color.correct_answer_green)
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
                Text(text = "$repeatCount/$MAX_REPEATS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            gallery(Modifier.fillMaxHeight().weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedVerse != null) {
                    Text(
                        text = "${selectedVerse.book} ${selectedVerse.chapter},${selectedVerse.number}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = colorResource(R.color.game_button_yellow_dark),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildAnnotatedVerseText(selectedVerse.text),
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
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
                    Spacer(modifier = Modifier.height(8.dp))
                    micButton()
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.repeat_verse_choose_hint),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                retentionCaption()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            gallery(Modifier.fillMaxWidth().weight(1f))
            Spacer(modifier = Modifier.height(12.dp))
            if (selectedVerse != null) {
                Text(
                    text = "${selectedVerse.book} ${selectedVerse.chapter},${selectedVerse.number}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colorResource(R.color.game_button_yellow_dark),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buildAnnotatedVerseText(selectedVerse.text),
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feedbackText,
                    fontSize = 13.sp,
                    color = feedbackColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                micButton()
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.repeat_verse_choose_hint),
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            retentionCaption()
        }
    }

    zoomIndex?.let { zi ->
        val zoomBitmap = remember(zi) {
            repository.loadActivePicture(sectionId, zi, assetNames.getOrNull(zi))
        }
        if (zoomBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { zoomIndex = null },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = zoomBitmap.asImageBitmap(),
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
        // Maxed out today: grey the picture so the user knows it can't gain more retention
        if (isMaxed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.6f))
            )
        }
    }
}

/** Small helper to run [block] whenever [key] changes (including first composition). */
@Composable
private fun LaunchedSelection(key: Int?, block: (Int?) -> Unit) {
    LaunchedEffect(key) { block(key) }
}
