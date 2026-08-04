package io.github.ptimulka.miecz.screens.mnemonic

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

private val PALETTE_COLORS = listOf(
    Color.Black,
    Color(0xFFE53935),      // Red
    Color(0xFF7E57C2),      // Violet
    Color(0xFF1E88E5),      // Blue
    Color(0xFFFDD835),      // Yellow
    Color(0xFFFFB3BA),      // Skin/Pink
    Color(0xFF43A047),      // Green
    Color(0xFFFF8F00),      // Orange
    Color(0xFF81D4FA),      // Light Blue
    Color(0xFF9E9E9E),      // Grey
    Color(0xFFA5D6A7),      // Light Green
    Color.White
)

private val STROKE_WIDTHS = listOf(8f, 16f, 28f, 40f)
private const val DRAWING_ASPECT_RATIO = 3f / 4f
private val CANVAS_OUTSIDE_COLOR = Color(0xFF9E9E9E)

private fun drawingAreaSize(availableWidth: Dp, availableHeight: Dp): Pair<Dp, Dp> {
    val maxHeightFromWidth = availableWidth * (4f / 3f)
    return if (maxHeightFromWidth <= availableHeight) {
        Pair(availableWidth, maxHeightFromWidth)
    } else {
        Pair(availableHeight * DRAWING_ASPECT_RATIO, availableHeight)
    }
}

@Composable
fun DrawingScreen(
    state: MnemonicUiState,
    onEvent: (MnemonicEvent) -> Unit
) {
    val drawing = state.drawingState
    val verseData = state.verseData[drawing.verseIndex]
    val sigla = "${verseData.verse.book} ${verseData.verse.chapter},${verseData.verse.number}"
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    BackHandler(enabled = true) {
        onEvent(MnemonicEvent.RequestBackFromDrawing)
    }

    if (drawing.showExitDialog) {
        ExitDialog(
            onSave = { onEvent(MnemonicEvent.SaveDrawing(canvasSize)) },
            onDiscard = { onEvent(MnemonicEvent.ConfirmDiscardDrawing) },
            onDismiss = { onEvent(MnemonicEvent.DismissExitDialog) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DrawingTopBar(
            sigla = sigla,
            verseText = verseData.verse.text,
            canUndo = drawing.undoStack.isNotEmpty(),
            canRedo = drawing.redoStack.isNotEmpty(),
            onBack = { onEvent(MnemonicEvent.RequestBackFromDrawing) },
            onUndo = { onEvent(MnemonicEvent.Undo) },
            onRedo = { onEvent(MnemonicEvent.Redo) },
            onSave = { onEvent(MnemonicEvent.SaveDrawing(canvasSize)) }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    DrawingCanvas(state, onEvent, Modifier.weight(1f).fillMaxHeight()) { canvasSize = it }
                    DrawingToolbar(
                        selectedColor = drawing.selectedColor,
                        selectedStrokeWidth = drawing.strokeWidthPx,
                        onEvent = onEvent,
                        isLandscape = true
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    DrawingCanvas(state, onEvent, Modifier.fillMaxWidth().weight(1f)) { canvasSize = it }
                    DrawingToolbar(
                        selectedColor = drawing.selectedColor,
                        selectedStrokeWidth = drawing.strokeWidthPx,
                        onEvent = onEvent,
                        isLandscape = false
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingCanvas(
    state: MnemonicUiState,
    onEvent: (MnemonicEvent) -> Unit,
    modifier: Modifier,
    onSizeChanged: (IntSize) -> Unit
) {
    val drawing = state.drawingState
    val verseData = state.verseData[drawing.verseIndex]
    
    // Live points kept local for responsiveness and to avoid stale ViewModel capture
    var localLivePoints by remember { mutableStateOf<List<androidx.compose.ui.geometry.Offset>>(emptyList()) }

    BoxWithConstraints(
        modifier = modifier.background(CANVAS_OUTSIDE_COLOR),
        contentAlignment = Alignment.Center
    ) {
        val (drawWidth, drawHeight) = drawingAreaSize(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier
                .size(drawWidth, drawHeight)
                .background(Color.White)
                .clipToBounds()
                .onSizeChanged { onSizeChanged(it) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            localLivePoints = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            localLivePoints = localLivePoints + change.position
                        },
                        onDragEnd = {
                            if (localLivePoints.size >= 2) {
                                val normalized = localLivePoints.map {
                                    androidx.compose.ui.geometry.Offset(it.x / size.width.toFloat(), it.y / size.height.toFloat())
                                }
                                onEvent(MnemonicEvent.AddStroke(normalized))
                            }
                            localLivePoints = emptyList()
                        },
                        onDragCancel = {
                            localLivePoints = emptyList()
                        }
                    )
                }
        ) {
            drawRect(Color.White)

            verseData.userBitmap?.let { bg ->
                val scale = minOf(size.width / bg.width, size.height / bg.height)
                val dstW = (bg.width * scale).toInt()
                val dstH = (bg.height * scale).toInt()
                val dstX = ((size.width - dstW) / 2).toInt()
                val dstY = ((size.height - dstH) / 2).toInt()
                drawImage(
                    image = bg.asImageBitmap(),
                    dstOffset = IntOffset(dstX, dstY),
                    dstSize = IntSize(dstW, dstH)
                )
            }

            drawing.strokes.forEach { stroke ->
                if (stroke.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(stroke.points.first().x * size.width, stroke.points.first().y * size.height)
                        stroke.points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                    }
                    drawPath(
                        path, stroke.color,
                        style = Stroke(width = stroke.strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            if (localLivePoints.size >= 2) {
                val path = Path().apply {
                    moveTo(localLivePoints.first().x, localLivePoints.first().y)
                    localLivePoints.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path, drawing.selectedColor,
                    style = Stroke(width = drawing.strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
private fun DrawingTopBar(
    sigla: String,
    verseText: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = colorResource(id = R.color.game_button_yellow_dark),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = sigla,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.undo),
                        tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.graphicsLayer { scaleX = -1f }
                    )
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.redo),
                        tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                }
                TextButton(onClick = { onSave() }) { 
                    Text(stringResource(R.string.save_drawing), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = buildAnnotatedVerseText(verseText),
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ExitDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.unsaved_changes_title)) },
        text = { Text(stringResource(R.string.unsaved_changes_message)) },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save_and_exit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard_and_exit))
            }
        }
    )
}

@Composable
private fun DrawingToolbar(
    selectedColor: Color,
    selectedStrokeWidth: Float,
    onEvent: (MnemonicEvent) -> Unit,
    isLandscape: Boolean
) {
    Surface(
        modifier = if (isLandscape) Modifier.fillMaxHeight() else Modifier.fillMaxWidth(),
        color = colorResource(id = R.color.game_button_yellow_light),
        shadowElevation = 8.dp
    ) {
        if (isLandscape) {
            Column(
                modifier = Modifier.width(110.dp).fillMaxHeight().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PALETTE_COLORS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { color ->
                            ColorDot(color, color == selectedColor) { onEvent(MnemonicEvent.SetColor(it)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(12.dp))
                // Stroke widths: 2 columns
                STROKE_WIDTHS.chunked(2).forEach { rowWidths ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowWidths.forEach { width ->
                            StrokeDot(width, width == selectedStrokeWidth) { onEvent(MnemonicEvent.SetStrokeWidth(it)) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    PALETTE_COLORS.forEach { color ->
                        ColorDot(color, color == selectedColor, size = 20.dp) { onEvent(MnemonicEvent.SetColor(it)) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    STROKE_WIDTHS.forEach { width ->
                        StrokeDot(width, width == selectedStrokeWidth) { onEvent(MnemonicEvent.SetStrokeWidth(it)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, isSelected: Boolean, size: Dp = 24.dp, onClick: (Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) colorResource(R.color.game_button_yellow_dark) else Color.Gray, CircleShape)
            .background(color, CircleShape)
            .clickable { onClick(color) }
    )
}

@Composable
private fun StrokeDot(width: Float, isSelected: Boolean, onClick: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) colorResource(R.color.game_button_yellow_dark) else Color.Gray, CircleShape)
            .background(Color(0xFFF5F5F5), CircleShape)
            .clickable { onClick(width) },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size((width / 3.5f).dp).background(Color.DarkGray, CircleShape))
    }
}
