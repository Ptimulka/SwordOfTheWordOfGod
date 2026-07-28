package io.github.ptimulka.miecz.components.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.helpers.AnkiExporter
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tan

// Retention rays: (angle from vertical in degrees, length factor 0..1). Angles fan the burst
// out from the heart; the varied factors give the rays different lengths. The central rays
// reach full depth (factor 1.0) so the burst extends to the deepest reached level; they are
// densest in the middle and taper toward the edges.
private val RETENTION_RAYS = listOf(
    -34f to 0.46f, -28f to 0.60f, -22f to 0.74f, -16f to 0.86f, -11f to 0.94f,
    -7f to 0.98f, -3f to 1.0f, 0f to 1.0f, 3f to 1.0f, 7f to 0.98f,
    11f to 0.94f, 16f to 0.86f, 22f to 0.74f, 28f to 0.60f, 34f to 0.46f
)

@Composable
fun SectionHeader(section: Section, isLocked: Boolean, showNumber: Boolean = true) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        color = colorResource(id = if (isLocked) R.color.game_button_grey_dark else R.color.game_button_yellow_dark),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = if (showNumber) "${section.id}. ${section.name}" else section.name,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

enum class LevelButtonState { FULL, HALF, LOCKED }

@Composable
fun LevelItem(
    levelIndex: Int,
    index: Int,
    isFinished: Boolean,
    state: LevelButtonState,
    raysReach: Int = 0,
    lockMessage: String? = null,
    onLevelClick: () -> Unit = {}
) {
    val horizontalBias = remember(index) { (sin(index * 0.8) * 0.7f).toFloat() }
    val rayColor = colorResource(id = R.color.game_button_yellow_dark)
    var showLockDialog by remember { mutableStateOf(false) }

    if (showLockDialog && lockMessage != null) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            confirmButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text(stringResource(id = R.string.ok_button))
                }
            },
            text = { Text(lockMessage) }
        )
    }

    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        if (raysReach > 0) {
            // Draw only THIS level's slice of the heart's ray burst, so nothing disappears while
            // scrolling. All rays share one apex above level 1 (the heart's position).
            Canvas(modifier = Modifier.matchParentSize()) {
                val itemHeight = size.height
                val heartGap = 90.dp.toPx()          // heart sits this far above level 1's top
                val apexToBottom = heartGap + raysReach * itemHeight
                val cx = size.width / 2f
                val topOffset = (levelIndex - 1) * itemHeight   // this item's top in "level space"
                val apexLocalY = -heartGap - topOffset          // shared apex, in this item's coords
                clipRect(0f, 0f, size.width, itemHeight) {
                    RETENTION_RAYS.forEach { (deg, factor) ->
                        val rad = deg * (PI.toFloat() / 180f)
                        val vertical = factor * apexToBottom
                        drawLine(
                            color = rayColor.copy(alpha = 0.38f),
                            start = Offset(cx, apexLocalY),
                            end = Offset(cx + tan(rad) * vertical, apexLocalY + vertical),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
        val clickable = state == LevelButtonState.FULL || lockMessage != null
        LevelButton(
            level = levelIndex,
            isFinished = isFinished,
            state = state,
            clickable = clickable,
            modifier = Modifier.align(BiasAlignment(horizontalBias, 0f)),
            onClick = {
                if (state == LevelButtonState.FULL) onLevelClick()
                else if (lockMessage != null) showLockDialog = true
            }
        )
    }
}

@Composable
fun LevelButton(
    level: Int,
    isFinished: Boolean,
    state: LevelButtonState,
    clickable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.size(100.dp).then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            LevelButtonState.FULL -> Image(
                painter = painterResource(id = R.drawable.buttonhigh),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            LevelButtonState.LOCKED -> Image(
                painter = painterResource(id = R.drawable.buttonlow),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            LevelButtonState.HALF -> {
                // Grey base with the yellow button at half opacity on top
                Image(
                    painter = painterResource(id = R.drawable.buttonlow),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.buttonhigh),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = 0.5f
                )
            }
        }
        Text(
            text = level.toString(),
            color = colorResource(
                id = if (state == LevelButtonState.LOCKED) R.color.level_button_number_grey
                else R.color.level_button_number
            ),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.offset(x = 2.dp, y = (-8).dp)
        )
        if (isFinished) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colorResource(id = R.color.game_button_yellow_dark),
                modifier = Modifier.size(56.dp).align(Alignment.CenterEnd).offset(x = 34.dp, y = (-40).dp)
            )
        }
    }
}

@Composable
fun SquareGameButton(iconRes: Int, labelRes: Int, isLocked: Boolean, recordText: String? = null, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp).then(if (!isLocked) Modifier.clickable { onClick() } else Modifier)
    ) {
        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(100.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = labelRes),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            color = Color.Gray,
            lineHeight = 11.sp,
            modifier = Modifier.width(100.dp)
        )
        if (recordText != null) {
            Text(
                text = recordText,
                textAlign = TextAlign.Center,
                fontSize = 9.sp,
                color = colorResource(R.color.record_gold),
                lineHeight = 10.sp,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

/**
 * Heart button representing section retention.
 * - Not reached yet (isLocked): fully grey heart.
 * - Finished section (isFinished): fully yellow heart.
 * - Current section (reached but not finished): grey heart filled from the bottom
 *   with the yellow heart up to [retentionPercent].
 */
@Composable
fun HeartRetentionButton(
    isLocked: Boolean,
    isFinished: Boolean,
    retentionPercent: Int,
    dailyMaxReached: Boolean,
    labelRes: Int,
    modifier: Modifier = Modifier
) {
    val isCurrent = !isLocked && !isFinished
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(id = R.string.ok_button))
                }
            },
            text = {
                Text(
                    stringResource(
                        id = if (dailyMaxReached) R.string.retention_heart_toast_maxed
                        else R.string.retention_heart_toast
                    )
                )
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .then(
                    if (isCurrent) Modifier.clickable { showInfoDialog = true } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLocked -> {
                    Image(
                        painter = painterResource(id = R.drawable.buttonheartlow),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                isFinished -> {
                    Image(
                        painter = painterResource(id = R.drawable.buttonheart),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                else -> {
                    // Current section: full grey heart stays visible …
                    Image(
                        painter = painterResource(id = R.drawable.buttonheartlow),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    // … and the yellow heart fills up inside it like liquid, with a flat
                    // waterline at the retention level (the shape never grows or scales).
                    val fraction = (retentionPercent.coerceIn(0, 100)) / 100f
                    Image(
                        painter = painterResource(id = R.drawable.buttonheart),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(top = size.height * (1f - fraction)) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Finished sections are fully retained → show 100%
        val displayPercent = if (isFinished) 100 else retentionPercent.coerceIn(0, 100)
        Text(
            text = stringResource(id = labelRes, displayPercent),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            color = Color.Gray,
            lineHeight = 11.sp,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
fun ChallengeButton(isLocked: Boolean, isFinished: Boolean, iconRes: Int, labelRes: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp).then(if (!isLocked) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(100.dp), contentScale = ContentScale.Fit)
            if (isFinished) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colorResource(id = R.color.game_button_yellow_dark),
                    modifier = Modifier.size(56.dp).align(Alignment.CenterEnd).offset(x = 34.dp, y = (-40).dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(id = labelRes), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color.Gray, lineHeight = 11.sp)
    }
}

@Composable
fun SectionVersesDialog(section: Section, onDismissRequest: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.verses_title, section.name)) },
        text = {
            SelectionContainer {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(section.verses) { index, verse ->
                        val annotatedText = buildAnnotatedString {
                            append("${index + 1}. ${verse.book} ${verse.chapter},${verse.number} - ")
                            val cleanText = verse.text.replace('_', ' ')
                            val parts = cleanText.split("*")
                            parts.forEachIndexed { i, part ->
                                if (i % 2 == 0) withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
                                else withStyle(style = SpanStyle(color = Color.Gray)) { append(part) }
                            }
                        }
                        Text(text = annotatedText, modifier = Modifier.padding(vertical = 4.dp), fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.ok_button))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                AnkiExporter.exportToCsv(context, section.name, section.verses)
            }) {
                Text(stringResource(id = R.string.export_to_anki))
            }
        }
    )
}
