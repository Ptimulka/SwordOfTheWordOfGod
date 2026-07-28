package io.github.ptimulka.miecz.screens

import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clipToBounds
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
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthPx: Float
)

sealed class DrawingAction {
    data class StrokeAdded(val stroke: DrawingStroke) : DrawingAction()
}

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

/** Portrait aspect ratio (width / height) used for the drawing canvas in landscape mode. */
private const val DRAWING_ASPECT_RATIO = 3f / 4f

private val CANVAS_OUTSIDE_COLOR = Color(0xFF9E9E9E)

/**
 * Canvas always maintains 3:4 aspect ratio in both orientations.
 * Canvas is centered with grey area around it showing non-drawable space.
 */
private fun drawingAreaSize(
    availableWidth: Dp,
    availableHeight: Dp
): Pair<Dp, Dp> {
    val maxHeightFromWidth = availableWidth * (4f / 3f)
    return if (maxHeightFromWidth <= availableHeight) {
        Pair(availableWidth, maxHeightFromWidth)
    } else {
        Pair(availableHeight * DRAWING_ASPECT_RATIO, availableHeight)
    }
}

@Composable
fun DrawingScreen(
    verse: Verse,
    sectionId: Int,
    verseIndex: Int,
    repository: MnemonicPicturesRepository,
    onBack: () -> Unit
) {
    val sigla = "${verse.book} ${verse.chapter},${verse.number}"
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var backgroundBitmap by remember { mutableStateOf(repository.loadPicture(sectionId, verseIndex)) }
    var strokes by remember { mutableStateOf(listOf<DrawingStroke>()) }
    var undoStack by remember { mutableStateOf(listOf<DrawingAction>()) }
    var redoStack by remember { mutableStateOf(listOf<DrawingAction>()) }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidthPx by remember { mutableStateOf(STROKE_WIDTHS[0]) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isDirty by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var pendingBackAction: (() -> Unit)? by remember { mutableStateOf(null) }

    fun addStroke(rawPoints: List<Offset>, color: Color, width: Float) {
        if (canvasSize == IntSize.Zero) return
        val normalized = rawPoints.map {
            Offset(it.x / canvasSize.width, it.y / canvasSize.height)
        }
        val stroke = DrawingStroke(normalized, color, width)
        strokes = strokes + stroke
        undoStack = undoStack + DrawingAction.StrokeAdded(stroke)
        redoStack = emptyList()
        isDirty = true
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = redoStack + action

            when (action) {
                is DrawingAction.StrokeAdded -> strokes = strokes.dropLast(1)
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = undoStack + action

            when (action) {
                is DrawingAction.StrokeAdded -> strokes = strokes + action.stroke
            }
        }
    }

    fun saveDrawing() {
        if (canvasSize == IntSize.Zero) return
        val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        backgroundBitmap?.let { bg ->
            val scale = minOf(canvasSize.width.toFloat() / bg.width, canvasSize.height.toFloat() / bg.height)
            val dstW = (bg.width * scale).toInt()
            val dstH = (bg.height * scale).toInt()
            val dstX = (canvasSize.width - dstW) / 2
            val dstY = (canvasSize.height - dstH) / 2
            canvas.drawBitmap(
                android.graphics.Bitmap.createScaledBitmap(bg, dstW, dstH, true),
                dstX.toFloat(), dstY.toFloat(), null
            )
        }
        val paint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            paint.color = stroke.color.toArgb()
            paint.strokeWidth = stroke.strokeWidthPx
            val path = android.graphics.Path()
            path.moveTo(stroke.points.first().x * canvasSize.width, stroke.points.first().y * canvasSize.height)
            stroke.points.drop(1).forEach { path.lineTo(it.x * canvasSize.width, it.y * canvasSize.height) }
            canvas.drawPath(path, paint)
        }
        repository.savePicture(sectionId, verseIndex, bitmap)
        backgroundBitmap = bitmap
        strokes = emptyList()
        undoStack = emptyList()
        redoStack = emptyList()
        isDirty = false
    }

    fun handleBack() {
        if (isDirty) {
            showExitDialog = true
            pendingBackAction = onBack
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    // Composable lambda to avoid duplicating the BoxWithConstraints + Canvas block.
    val drawingCanvas: @Composable (Modifier) -> Unit = { outerModifier ->
        BoxWithConstraints(
            modifier = outerModifier.background(CANVAS_OUTSIDE_COLOR),
            contentAlignment = Alignment.Center
        ) {
            val (drawWidth, drawHeight) = drawingAreaSize(maxWidth, maxHeight)

            Canvas(
                modifier = Modifier
                    .size(drawWidth, drawHeight)
                    .background(Color.White)
                    .clipToBounds()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(selectedColor, strokeWidthPx) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPoints = listOf(offset) },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    addStroke(currentPoints, selectedColor, strokeWidthPx)
                                }
                                currentPoints = emptyList()
                            },
                            onDragCancel = { currentPoints = emptyList() }
                        )
                    }
            ) {
                drawRect(Color.White)

                // Display existing bitmap with fit-within (letterbox) — never stretch.
                backgroundBitmap?.let { bg ->
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

                strokes.forEach { stroke ->
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

                if (currentPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        currentPoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path, selectedColor,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    saveDrawing()
                    showExitDialog = false
                    pendingBackAction?.invoke()
                }) {
                    Text(stringResource(R.string.save_and_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    pendingBackAction?.invoke()
                }) {
                    Text(stringResource(R.string.discard_and_exit))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DrawingTopBar(
            sigla = sigla,
            verseText = verse.text,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onBack = { handleBack() },
            onUndo = { undo() },
            onRedo = { redo() },
            onSave = { saveDrawing() }
        )

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                drawingCanvas(Modifier.weight(1f).fillMaxHeight())
                DrawingToolbar(
                    selectedColor = selectedColor,
                    selectedStrokeWidth = strokeWidthPx,
                    onColorSelected = { selectedColor = it },
                    onStrokeWidthSelected = { strokeWidthPx = it },
                    isLandscape = true
                )
            }
        } else {
            drawingCanvas(Modifier.fillMaxWidth().weight(1f))
            DrawingToolbar(
                selectedColor = selectedColor,
                selectedStrokeWidth = strokeWidthPx,
                onColorSelected = { selectedColor = it },
                onStrokeWidthSelected = { strokeWidthPx = it },
                isLandscape = false
            )
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
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
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
                TextButton(onClick = onSave) {
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
private fun DrawingToolbar(
    selectedColor: Color,
    selectedStrokeWidth: Float,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthSelected: (Float) -> Unit,
    isLandscape: Boolean
) {
    Surface(
        modifier = if (isLandscape) Modifier.fillMaxHeight() else Modifier.fillMaxWidth(),
        color = colorResource(id = R.color.game_button_yellow_light),
        shadowElevation = 8.dp
    ) {
        if (isLandscape) {
            LandscapeToolbarContent(selectedColor, selectedStrokeWidth, onColorSelected, onStrokeWidthSelected)
        } else {
            PortraitToolbarContent(selectedColor, selectedStrokeWidth, onColorSelected, onStrokeWidthSelected)
        }
    }
}

@Composable
private fun PortraitToolbarContent(
    selectedColor: Color,
    selectedStrokeWidth: Float,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PALETTE_COLORS.forEach { color ->
                ColorDot(color = color, isSelected = color == selectedColor, onClick = { onColorSelected(color) }, size = 20.dp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            STROKE_WIDTHS.forEach { width ->
                StrokeDot(width = width, isSelected = width == selectedStrokeWidth, onClick = { onStrokeWidthSelected(width) })
            }
        }
    }
}

@Composable
private fun LandscapeToolbarContent(
    selectedColor: Color,
    selectedStrokeWidth: Float,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .fillMaxHeight()
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Colors: 3 columns
        PALETTE_COLORS.chunked(3).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowColors.forEach { color ->
                    ColorDot(color = color, isSelected = color == selectedColor, onClick = { onColorSelected(color) }, size = 24.dp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Stroke widths: single column
        STROKE_WIDTHS.forEach { width ->
            StrokeDot(width = width, isSelected = width == selectedStrokeWidth, onClick = { onStrokeWidthSelected(width) }, baseSize = 28.dp)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ColorDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray,
                shape = CircleShape
            )
            .background(color, CircleShape)
            .clickable { onClick() }
    )
}

@Composable
private fun StrokeDot(
    width: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    baseSize: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(baseSize)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) colorResource(id = R.color.game_button_yellow_dark) else Color.Gray,
                shape = CircleShape
            )
            .background(Color(0xFFF5F5F5), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((width / 3.5f).dp)
                .background(Color.DarkGray, CircleShape)
        )
    }
}
