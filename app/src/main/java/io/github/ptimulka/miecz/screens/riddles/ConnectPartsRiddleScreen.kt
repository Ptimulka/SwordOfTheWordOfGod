package io.github.ptimulka.miecz.screens.riddles

import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import kotlinx.coroutines.delay

data class PartConnectItem(val id: Int, val part1: String, val part2: String)

private fun splitVerse(verse: Verse): PartConnectItem {
    val text = verse.text.replace("_", " ")
    val words = text.split(' ')
    var splitIndex = words.size / 2

    if (words.size % 2 != 0) {
        splitIndex++
    }

    if (splitIndex > 0 && words[splitIndex - 1].length < 3) {
        splitIndex--
    }

    val part1 = words.subList(0, splitIndex).joinToString(" ")
    val part2 = words.subList(splitIndex, words.size).joinToString(" ")

    return PartConnectItem(verse.hashCode(), "${verse.book} ${verse.chapter},${verse.number} $part1", part2)
}

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
fun ConnectPartsRiddleScreen(
    sectionVerses: List<Verse>,
    sectionId: Int = 0,
    assetNames: List<String> = emptyList(),
    onSuccess: (elapsedMs: Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val buttonHeight = if (isLandscape) 80.dp else 180.dp
    val buttonPadding = PaddingValues(horizontal = 8.dp)

    val context = LocalContext.current
    val hintBitmaps: Map<Int, Bitmap> = remember(sectionId, sectionVerses) {
        if (sectionId <= 0 && assetNames.isEmpty()) emptyMap()
        else {
            val repo = MnemonicPicturesRepository(context)
            sectionVerses.mapIndexed { index, verse ->
                verse.hashCode() to repo.loadActivePicture(sectionId, index, assetNames.getOrNull(index))
            }.mapNotNull { (key, bitmap) -> bitmap?.let { key to it } }.toMap()
        }
    }

    val partListSaver = Saver<List<PartConnectItem>, List<Any>>(
        save = { list ->
            list.map { listOf(it.id, it.part1, it.part2) }.flatten()
        },
        restore = { saved ->
            val restoredList = mutableListOf<PartConnectItem>()
            var i = 0
            while (i < saved.size) {
                restoredList.add(PartConnectItem(saved[i] as Int, saved[i + 1] as String, saved[i + 2] as String))
                i += 3
            }
            restoredList
        }
    )

    val (initialParts1, initialParts2) = remember(sectionVerses) {
        val shuffledVerses = sectionVerses.shuffled()
        val parts = shuffledVerses.map { splitVerse(it) }

        val part1Items = parts.map { PartConnectItem(it.id, it.part1, "") }
        val part2Items = parts.map { PartConnectItem(it.id, "", it.part2) }

        val reorderedPart1s = part1Items.customReorder(startIndex = 3)
        val reorderedPart2s = part2Items.customReorder(startIndex = 2)

        val finalPart1s = reorderedPart1s.take(3).shuffled() + reorderedPart1s.drop(3)
        val finalPart2s = reorderedPart2s.take(3).shuffled() + reorderedPart2s.drop(3)

        finalPart1s to finalPart2s
    }

    var remainingParts1 by rememberSaveable(stateSaver = partListSaver) { mutableStateOf(initialParts1) }
    var remainingParts2 by rememberSaveable(stateSaver = partListSaver) { mutableStateOf(initialParts2) }

    val startTimeMs = rememberSaveable { System.currentTimeMillis() }
    var selectedPart1 by rememberSaveable(stateSaver = partItemSaver) { mutableStateOf<PartConnectItem?>(null) }
    var selectedPart2 by rememberSaveable(stateSaver = partItemSaver) { mutableStateOf<PartConnectItem?>(null) }
    var wrongPair by remember { mutableStateOf<Pair<PartConnectItem?, PartConnectItem?>?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var wasInLockout by rememberSaveable { mutableStateOf(false) }
    if (wasInLockout) { selectedPart1 = null; selectedPart2 = null; wasInLockout = false }
    var justMatchedId by remember { mutableStateOf<Int?>(null) }
    var completedElapsedMs by rememberSaveable { mutableStateOf(-1L) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingRemoveId by rememberSaveable { mutableStateOf<Int?>(null) }
    if (pendingRemoveId != null && previewBitmap == null) {
        val id = pendingRemoveId
        remainingParts1 = remainingParts1.filter { it.id != id }
        remainingParts2 = remainingParts2.filter { it.id != id }
        selectedPart1 = null
        selectedPart2 = null
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
            selectedPart1 = null
            selectedPart2 = null
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
            delay(300)
            val matchedId = justMatchedId!!
            val bitmap = hintBitmaps[matchedId]
            if (bitmap != null) {
                pendingRemoveId = matchedId
                justMatchedId = null
                previewBitmap = bitmap
            } else {
                remainingParts1 = remainingParts1.filter { it.id != matchedId }
                remainingParts2 = remainingParts2.filter { it.id != matchedId }
                justMatchedId = null
                selectedPart1 = null
                selectedPart2 = null
            }
        }
    }

    if (remainingParts1.isEmpty() && justMatchedId == null && completedElapsedMs < 0) {
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
                    remainingParts1 = remainingParts1.filter { it.id != id }
                    remainingParts2 = remainingParts2.filter { it.id != id }
                    pendingRemoveId = null
                    selectedPart1 = null
                    selectedPart2 = null
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
            text = stringResource(id = R.string.connect_parts_caption),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                remainingParts1.take(3).forEach { part1Item ->
                    val isSelected = selectedPart1 == part1Item
                    val isWrong = wrongPair?.first == part1Item
                    val isMatched = justMatchedId == part1Item.id

                    Button(
                        onClick = {
                            if (!isLocked) {
                            if (selectedPart1 == part1Item) {
                                selectedPart1 = null
                                wrongPair = null
                            } else {
                                selectedPart1 = part1Item
                                wrongPair = null
                                if (selectedPart2 != null) {
                                    if (part1Item.id == selectedPart2!!.id) {
                                        justMatchedId = part1Item.id
                                    } else {
                                        wrongPair = Pair(part1Item, selectedPart2)
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
                        val annotatedString = buildAnnotatedVerseText(part1Item.part1)
                        Text(annotatedString, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                remainingParts2.take(3).forEach { part2Item ->
                    val isSelected = selectedPart2 == part2Item
                    val isWrong = wrongPair?.second == part2Item
                    val isMatched = justMatchedId == part2Item.id

                    Button(
                        onClick = {
                            if (!isLocked) {
                            if (selectedPart2 == part2Item) {
                                selectedPart2 = null
                                wrongPair = null
                            } else {
                                selectedPart2 = part2Item
                                wrongPair = null
                                if (selectedPart1 != null) {
                                    if (selectedPart1!!.id == part2Item.id) {
                                        justMatchedId = part2Item.id
                                    } else {
                                        wrongPair = Pair(selectedPart1, part2Item)
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
                        val annotatedString = buildAnnotatedVerseText(part2Item.part2)
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

val partItemSaver = Saver<PartConnectItem?, Any>(
    save = { item -> if (item != null) listOf(item.id, item.part1, item.part2) else emptyList<Any>() },
    restore = { saved ->
        val list = saved as List<Any>
        if (list.isNotEmpty()) PartConnectItem(list[0] as Int, list[1] as String, list[2] as String) else null
    }
)

