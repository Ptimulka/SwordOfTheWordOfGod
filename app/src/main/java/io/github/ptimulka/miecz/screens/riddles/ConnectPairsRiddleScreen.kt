package io.github.ptimulka.miecz.screens.riddles

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import android.graphics.Bitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import kotlinx.coroutines.delay

data class PairConnectItem(val id: Int, val text: String)

private fun <T> List<T>.customReorder(startIndex: Int): List<T> {
    if (this.size < startIndex + 1) return this

    val result = this.toMutableList()
    for (i in startIndex until result.size - 1 step 2) {
        val temp = result[i]
        result[i] = result[i + 1]
        result[i + 1] = temp
    }
    return result
}

@Composable
fun ConnectPairsRiddleScreen(
    sectionVerses: List<Verse>,
    sectionId: Int,
    assetNames: List<String> = emptyList(),
    onSuccess: (elapsedMs: Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    // Map sigla text → bitmap, loaded once for all visible sigla buttons
    val hintBitmaps: Map<String, Bitmap> = remember(sectionId, sectionVerses) {
        if (sectionId <= 0 && assetNames.isEmpty()) emptyMap()
        else {
            val repo = MnemonicPicturesRepository(context)
            sectionVerses.mapIndexed { index, verse ->
                val assetName = assetNames.getOrNull(index)
                "${verse.book} ${verse.chapter},${verse.number}" to
                    repo.loadActivePicture(sectionId, index, assetName)
            }.mapNotNull { (key, bitmap) -> bitmap?.let { key to it } }.toMap()
        }
    }
    val buttonHeight = if (isLandscape) 80.dp else 180.dp
    val rightColumnWeight = if (isLandscape) 4f else 3f
    val buttonPadding = PaddingValues(horizontal = 8.dp)

    val pairListSaver = Saver<List<PairConnectItem>, List<Any>>(
        save = { list ->
            list.map { listOf(it.id, it.text) }.flatten()
        },
        restore = { saved ->
            val restoredList = mutableListOf<PairConnectItem>()
            var i = 0
            while (i < saved.size) {
                restoredList.add(PairConnectItem(saved[i] as Int, saved[i + 1] as String))
                i += 2
            }
            restoredList
        }
    )

    val (initialSigla, initialVerses) = remember(sectionVerses) {
        val verses = sectionVerses.shuffled().map { it.copy(text = it.text.replace("_", " ")) }

        val siglaItems = verses.map { PairConnectItem(it.hashCode(), "${it.book} ${it.chapter},${it.number}") }
        val verseItems = verses.map { PairConnectItem(it.hashCode(), it.text) }

        val reorderedSigla = siglaItems.customReorder(startIndex = 3)
        val reorderedVerses = verseItems.customReorder(startIndex = 2)

        val finalSigla = reorderedSigla.take(3).shuffled() + reorderedSigla.drop(3)
        val finalVerses = reorderedVerses.take(3).shuffled() + reorderedVerses.drop(3)

        finalSigla to finalVerses
    }

    var remainingSigla by rememberSaveable(stateSaver = pairListSaver) { mutableStateOf(initialSigla) }
    var remainingVerses by rememberSaveable(stateSaver = pairListSaver) { mutableStateOf(initialVerses) }

    val startTimeMs = rememberSaveable { System.currentTimeMillis() }
    var selectedSigla by rememberSaveable(stateSaver = pairItemSaver) { mutableStateOf<PairConnectItem?>(null) }
    var selectedVerse by rememberSaveable(stateSaver = pairItemSaver) { mutableStateOf<PairConnectItem?>(null) }
    var wrongPair by remember { mutableStateOf<Pair<PairConnectItem?, PairConnectItem?>?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var wasInLockout by rememberSaveable { mutableStateOf(false) }
    if (wasInLockout) { selectedSigla = null; selectedVerse = null; wasInLockout = false }
    var justMatchedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var completedElapsedMs by rememberSaveable { mutableStateOf(-1L) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingRemoveId by rememberSaveable { mutableStateOf<Int?>(null) }
    if (pendingRemoveId != null && previewBitmap == null) {
        val id = pendingRemoveId
        remainingSigla = remainingSigla.filter { it.id != id }
        remainingVerses = remainingVerses.filter { it.id != id }
        selectedSigla = null
        selectedVerse = null
        pendingRemoveId = null
    }
    val redOverlayAlpha = remember { Animatable(0f) }

    LaunchedEffect(wrongPair) {
        if (wrongPair != null) {
            isLocked = true
            wasInLockout = true
            redOverlayAlpha.snapTo(0.35f)
            redOverlayAlpha.animateTo(0f, animationSpec = tween(2000))
            wrongPair = null
            selectedSigla = null
            selectedVerse = null
            isLocked = false
            wasInLockout = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (justMatchedId != null) 0f else 1f,
        animationSpec = tween(300)
    )

    LaunchedEffect(justMatchedId) {
        if (justMatchedId != null) {
            delay(300) // Wait for animation
            val matchedId = justMatchedId!!
            val siglaText = remainingSigla.find { it.id == matchedId }?.text
            val bitmap = siglaText?.let { hintBitmaps[it] }
            if (bitmap != null) {
                pendingRemoveId = matchedId
                justMatchedId = null
                previewBitmap = bitmap
            } else {
                remainingSigla = remainingSigla.filter { it.id != matchedId }
                remainingVerses = remainingVerses.filter { it.id != matchedId }
                justMatchedId = null
                selectedSigla = null
                selectedVerse = null
            }
        }
    }

    if (remainingSigla.isEmpty() && justMatchedId == null && completedElapsedMs < 0) {
        completedElapsedMs = System.currentTimeMillis() - startTimeMs
    }

    LaunchedEffect(completedElapsedMs) {
        if (completedElapsedMs >= 0) {
            onSuccess(completedElapsedMs)
        }
    }

    if (previewBitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable {
                    val id = pendingRemoveId
                    remainingSigla = remainingSigla.filter { it.id != id }
                    remainingVerses = remainingVerses.filter { it.id != id }
                    pendingRemoveId = null
                    selectedSigla = null
                    selectedVerse = null
                    previewBitmap = null
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f)
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.connect_pairs_caption),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Siglas
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                remainingSigla.take(3).forEach { siglaItem ->
                    val isSelected = selectedSigla == siglaItem
                    val isWrong = wrongPair?.first == siglaItem
                    val isMatched = justMatchedId == siglaItem.id

                    Button(
                        onClick = {
                            if (!isLocked) {
                            if (selectedSigla == siglaItem) {
                                selectedSigla = null
                                wrongPair = null
                            } else {
                                selectedSigla = siglaItem
                                wrongPair = null
                                if (selectedVerse != null) {
                                    if (siglaItem.id == selectedVerse!!.id) {
                                        justMatchedId = siglaItem.id
                                    } else {
                                        wrongPair = Pair(siglaItem, selectedVerse)
                                    }
                                }
                            }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isWrong -> colorResource(R.color.wrong_answer_highlight)
                                isSelected -> colorResource(id = R.color.game_button_yellow_dark)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = buttonPadding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .graphicsLayer(scaleX = if (isMatched) scale else 1f, scaleY = if (isMatched) scale else 1f)
                    ) {
                        val bitmap = hintBitmaps[siglaItem.text]
                        if (isLandscape) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(siglaItem.text, textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (bitmap != null) {
                                    Spacer(Modifier.width(6.dp))
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.height(56.dp).width(42.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(siglaItem.text, textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (bitmap != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().height(100.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: Verses
            Column(modifier = Modifier.weight(rightColumnWeight), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                remainingVerses.take(3).forEach { verseItem ->
                    val isSelected = selectedVerse == verseItem
                    val isWrong = wrongPair?.second == verseItem
                    val isMatched = justMatchedId == verseItem.id

                    Button(
                        onClick = {
                            if (!isLocked) {
                            if (selectedVerse == verseItem) {
                                selectedVerse = null
                                wrongPair = null
                            } else {
                                selectedVerse = verseItem
                                wrongPair = null
                                if (selectedSigla != null) {
                                    if (selectedSigla!!.id == verseItem.id) {
                                        justMatchedId = verseItem.id
                                    } else {
                                        wrongPair = Pair(selectedSigla, verseItem)
                                    }
                                }
                            }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isWrong -> colorResource(R.color.wrong_answer_highlight)
                                isSelected -> colorResource(id = R.color.game_button_yellow_dark)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = buttonPadding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .graphicsLayer(scaleX = if (isMatched) scale else 1f, scaleY = if (isMatched) scale else 1f)
                    ) {
                        val annotatedString = buildAnnotatedVerseText(verseItem.text)
                        Text(annotatedString, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
    if (redOverlayAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red.copy(alpha = redOverlayAlpha.value))
        )
    }
    } // end Box
}

val pairItemSaver = Saver<PairConnectItem?, Any>(
    save = { item -> if (item != null) listOf(item.id, item.text) else emptyList<Any>() },
    restore = { saved ->
        val list = saved as List<Any>
        if (list.isNotEmpty()) PairConnectItem(list[0] as Int, list[1] as String) else null
    }
)

